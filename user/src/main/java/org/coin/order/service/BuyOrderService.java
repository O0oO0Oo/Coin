package org.coin.order.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.coin.common.exception.CustomException;
import org.coin.common.exception.ErrorCode;
import org.coin.crypto.entity.Crypto;
import org.coin.crypto.repository.CryptoRepository;
import org.coin.order.dto.request.AddMarketPriceOrderRequest;
import org.coin.order.dto.request.AddOrderRequest;
import org.coin.order.dto.response.AddBuyOrderResponse;
import org.coin.order.dto.response.FindOrderResponse;
import org.coin.order.entity.BuyOrder;
import org.coin.order.repository.BuyOrderRepository;
import org.coin.price.service.PriceService;
import org.coin.trade.dto.service.OrderDto;
import org.coin.trade.service.TradeService;
import org.coin.user.entity.User;
import org.coin.user.repository.UserRepository;
import org.coin.wallet.entity.Wallet;
import org.coin.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BuyOrderService {
    private final BuyOrderRepository buyOrderRepository;
    private final UserRepository userRepository;
    private final CryptoRepository cryptoRepository;
    private final WalletRepository walletRepository;
    private final PriceService priceService;
    private final TradeService tradeService;

    @Value("${module.user.minimum-order-price}")
    private Double minimumOrderPrice;
    /**
     * 구매 주문 추가
     *  1. 유저 확인
     *  2. 거래 가능 코인인지 확인
     *  3. 유저 돈 인출, 저장
     *  4. BuyOrder 엔티티 생성, 저장
     *  5. 지갑 찾기, 없다면 생성
     *  6. TODO : Trade 모듈 Redis 에 주문 올리기
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public AddBuyOrderResponse executeBuyOrderTransaction(Long userId, AddOrderRequest request) {
        User user = findUserByIdOrElseThrow(userId); // 1

        Crypto crypto = findCryptoByIdOrElseThrow(request.cryptoId()); // 2

        withdrawMoneyOrElseThrow(user, request.price(), request.quantity()); // 3
        User updatedUser = userRepository.save(user);

        BuyOrder order = BuyOrder.builder() // 4
                .user(user)
                .crypto(crypto)
                .quantity(request.quantity())
                .price(request.price())
                .build();
        BuyOrder savedOrder = buyOrderRepository.save(order);

        Optional<Wallet> walletOpt = findWalletByUserIdAndCryptoId(user, crypto); // 5
        Wallet wallet;
        if (walletOpt.isEmpty()) {
            Wallet build = Wallet.builder()
                    .user(user)
                    .crypto(crypto)
                    .quantity(0.)
                    .build();
            wallet = walletRepository.save(build);
        }
        else {
            wallet = walletOpt.get();
        }

        tradeService.registerOrder( // 6 redis 에 등록
                registerOrderString(savedOrder, wallet, user, crypto)
        );
        return AddBuyOrderResponse.of(savedOrder, crypto.getName(), updatedUser.getMoney());
    }

    private Optional<Wallet> findWalletByUserIdAndCryptoId(User user, Crypto crypto) {
       return walletRepository.findByUserIdAndCryptoId(user.getId(), crypto.getId());
    }

    private OrderDto registerOrderString(BuyOrder buyOrder, Wallet wallet, User user, Crypto crypto) {
        return OrderDto.of(
                "buy",
                crypto.getName(),
                buyOrder.getPrice(),
                buyOrder.getId(),
                wallet.getId(),
                user.getId(),
                buyOrder.getQuantity(),
                convertToMilliseconds(buyOrder.getCreatedTime())
        );
    }

    private long convertToMilliseconds(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 현재가 구매 주문 추가
     *  1. 거래 가능 코인인지 확인
     *  2. 가격 체크, 일정 이상 금액의 주문인지 확인
     *  3. 유저 확인, 돈 인출, 업데이트
     *  4. 암호화폐 지갑에 저장, 없다면 생성
     *  5. BuyOrder 엔티티 생성, 저장
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public AddBuyOrderResponse executeMarketPriceBuyOrderTransaction(Long userId, @Valid AddMarketPriceOrderRequest request) {
        Crypto crypto = findCryptoByIdOrElseThrow(request.cryptoId());

        Double marketPrice = Double.valueOf(priceService.findPrice(crypto.getName()));
        checkTotalOrderPriceOrElseThrow(marketPrice, request.quantity());

        User user = findUserByIdOrElseThrow(userId);
        withdrawMoneyOrElseThrow(user, marketPrice, request.quantity());
        User updatedUser = userRepository.save(user);

        Wallet wallet = findWalletOrElseBuildAndSetQuantity(user, crypto, request.quantity());
        walletRepository.save(wallet);

        BuyOrder order = BuyOrder.builder()
                .user(user)
                .crypto(crypto)
                .quantity(request.quantity())
                .price(marketPrice)
                .processed(true)
                .build();
        BuyOrder savedOrder = buyOrderRepository.save(order);
        return AddBuyOrderResponse.of(savedOrder, crypto.getName(), updatedUser.getMoney());
    }

    /**
     * 존재하지 않거나, 거래 불가능한 화폐인지 검사
     * @param cryptoId
     * @return
     */
    private Crypto findCryptoByIdOrElseThrow(Long cryptoId) {
        Optional<Crypto> cryptoOpt = cryptoRepository.findById(cryptoId);
        if (cryptoOpt.isEmpty()){
            throw new CustomException(ErrorCode.CRYPTO_NOT_FOUND);
        }

        Crypto crypto = cryptoOpt.get();
        if(!crypto.isActive()){
            throw new CustomException(ErrorCode.CRYPTO_NOT_ACTIVE);
        }
        return crypto;
    }

    private void checkTotalOrderPriceOrElseThrow(Double marketPrice, Double quantity) {
        if(marketPrice * quantity < minimumOrderPrice){
            throw new CustomException(ErrorCode.TOTAL_ORDER_PRICE_INVALID, minimumOrderPrice);
        }
    }

    /**
     * 유저 조회 없다면 exception
     * @param userId
     * @return
     */
    private User findUserByIdOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Wallet findWalletOrElseBuildAndSetQuantity(User user, Crypto crypto, Double quantity) {
        Optional<Wallet> walletOpt = walletRepository.findByUserIdAndCryptoId(user.getId(), crypto.getId());

        if (walletOpt.isPresent()) {
            Wallet wallet = walletOpt.get();
            wallet.increaseQuantity(quantity);
            return wallet;
        }

        return Wallet.builder()
                .user(user)
                .crypto(crypto)
                .quantity(quantity)
                .build();
    }

    /**
     * 돈 인출, 부족하다면 exception
     * @param user
     * @param price
     * @param quantity
     */
    private void withdrawMoneyOrElseThrow(User user, Double price, Double quantity) {
        Double totalOrderPrice = price * quantity;

        if (user.getMoney() < totalOrderPrice) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_MONEY);
        }

        user.decreaseMoney(totalOrderPrice);
    }

    @Transactional(readOnly = true)
    public FindOrderResponse findAllBuyOrder(Long userId) {
        findUserByIdOrElseThrow(userId);

        return FindOrderResponse.of(buyOrderRepository.findBuyOrdersByUserId(userId));
    }

    /**
     * 구매 주문 취소
     * 1. 유저 조회
     * 2. 구매 주문 조회
     * 3. 구매 주문이 삭제 가능한지 조회
     * 4. 환불
     * 5. TODO : 거래 모듈의 Redis 에서도 삭제해야함
     * 6. 구매 주문 엔티티 취소됨 업데이트, 저장
     */
    @Transactional
    public void cancelAndRefundBuyOrderTransaction(Long userId, Long buyOrderId) {
        User user = findUserByIdOrElseThrow(userId);
        BuyOrder buyOrder = findBuyOrderByIdOrElseThrow(buyOrderId);

        checkBuyOrderCanCanceledOrElseThrow(buyOrder);
        refundMoney(user, buyOrder);

        // 거래 모듈 삭제기능 추가
        buyOrder.setCanceled(true);
        
        // redis 에서 삭제
        //

        userRepository.save(user);
        buyOrderRepository.save(buyOrder);
    }

    /**
     * 구매 주문 조회
     * @param buyOrderId
     * @return
     */
    private BuyOrder findBuyOrderByIdOrElseThrow(Long buyOrderId) {
        return buyOrderRepository.findById(buyOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 취소 가능여부 검사, exception
     * @param buyOrder
     */
    private void checkBuyOrderCanCanceledOrElseThrow(BuyOrder buyOrder) {
        if (buyOrder.isProcessed()) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        if (buyOrder.isCanceled()) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED);
        }
    }

    /**
     * 구매 취소에 따른 환불
     * @param user
     * @param buyOrder
     */
    private void refundMoney(User user, BuyOrder buyOrder) {
        Double price = buyOrder.getPrice();
        Double quantity = buyOrder.getQuantity();
        user.increaseMoney(price * quantity);
    }
}
