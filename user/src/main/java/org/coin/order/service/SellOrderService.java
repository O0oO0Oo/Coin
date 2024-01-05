package org.coin.order.service;

import lombok.RequiredArgsConstructor;
import org.coin.common.exception.CustomException;
import org.coin.common.exception.ErrorCode;
import org.coin.crypto.entity.Crypto;
import org.coin.crypto.repository.CryptoRepository;
import org.coin.order.dto.request.AddMarketPriceOrderRequest;
import org.coin.order.dto.request.AddOrderRequest;
import org.coin.order.dto.response.AddSellOrderResponse;
import org.coin.order.dto.response.FindOrderResponse;
import org.coin.order.entity.SellOrder;
import org.coin.order.repository.SellOrderRepository;
import org.coin.price.service.PriceService;
import org.coin.trade.dto.service.OrderDto;
import org.coin.trade.service.TradeService;
import org.coin.user.entity.User;
import org.coin.user.repository.UserRepository;
import org.coin.wallet.entity.Wallet;
import org.coin.wallet.repository.WalletRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SellOrderService {
    private final SellOrderRepository sellOrderRepository;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final CryptoRepository cryptoRepository;
    private final PriceService priceService;
    private final TradeService tradeService;

    @Value("${module.user.minimum-order-price}")
    private Double minimumOrderPrice;
    
    /**
     * 판매 주문 추가
     * 1. 유저 조회
     * 2. 거래 가능 코인인지 확인
     * 3. 지갑에서 화폐 인출
     * 4. TODO : Trade 모듈 Redis 에 주문 올리기
     * 5. SellOrder 엔티티 생성, 저장
     * 6. Trade 모듈 Redis 에 주문 올리기
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public AddSellOrderResponse executeSellOrderTransaction(Long userId, AddOrderRequest request) {
        User user = findUserByIdOrElseThrow(userId);
        Crypto crypto = findCryptoByIdOrElseThrow(request.cryptoId());
        Wallet wallet = findWalletByUserIdAndCryptoIdOrElseThrow(user, crypto);

        withdrawWalletOrElseThrow(wallet, request.quantity());
        Wallet savedWallet = walletRepository.save(wallet);

        // TODO : Trade module
        SellOrder sellOrder = SellOrder.builder()
                .user(user)
                .crypto(crypto)
                .quantity(request.quantity())
                .price(request.price())
                .build();
        SellOrder saveOrder = sellOrderRepository.save(sellOrder);

        // redis 에 등록
        tradeService.registerOrder(
                createOrderDto(sellOrder, wallet, user, crypto)
        );
        return AddSellOrderResponse.of(saveOrder, crypto.getName(), savedWallet.getQuantity());
    }

    private OrderDto createOrderDto(SellOrder sellOrder, Wallet wallet, User user, Crypto crypto) {
        return OrderDto.of(
                "sell",
                crypto.getName(),
                sellOrder.getPrice(),
                sellOrder.getId(),
                wallet.getId(),
                user.getId(),
                sellOrder.getQuantity(),
                convertToMilliseconds(sellOrder.getCreatedTime())
        );
    }

    private long convertToMilliseconds(LocalDateTime localDateTime) {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     * 현재가 판매 주문 추가
     * 1. 거래 가능 코인인지 확인
     * 2. 설정한 총 주문가격 이하인지 확인 
     * 3. 유저 조회
     * 4. 지갑 조회, 인출
     * 5. SellOrder 엔티티 생성
     * 6. 유저 돈 입금, 저장
     * 7. SellOrder 거래 저장
     *
     * @param userId
     * @param request
     * @return
     */
    @Transactional
    public AddSellOrderResponse executeMarketPriceSellOrderTransaction(Long userId, AddMarketPriceOrderRequest request) {
        Crypto crypto = findCryptoByIdOrElseThrow(request.cryptoId());

        Double marketPrice = Double.valueOf(priceService.findPrice(crypto.getName()));
        checkTotalOrderPriceOrElseThrow(marketPrice, request.quantity());

        User user = findUserByIdOrElseThrow(userId);

        Wallet wallet = findWalletByUserIdAndCryptoIdOrElseThrow(user, crypto);
        withdrawWalletOrElseThrow(wallet, request.quantity());
        Wallet updatedWallet = walletRepository.save(wallet);

        SellOrder order = SellOrder.builder()
                .user(user)
                .crypto(crypto)
                .quantity(request.quantity())
                .price(marketPrice)
                .processed(true)
                .build();

        user.increaseMoney(marketPrice * request.quantity());
        userRepository.save(user);

        SellOrder savedOrder = sellOrderRepository.save(order);
        return AddSellOrderResponse.of(savedOrder, crypto.getName(), updatedWallet.getQuantity());
    }

    /**
     * 존재하지 않거나, 거래 불가능한 화폐인지 검사
     *
     * @param cryptoId
     * @return
     */
    private Crypto findCryptoByIdOrElseThrow(Long cryptoId) {
        Optional<Crypto> cryptoOpt = cryptoRepository.findById(cryptoId);
        if (cryptoOpt.isEmpty()) {
            throw new CustomException(ErrorCode.CRYPTO_NOT_FOUND);
        }

        Crypto crypto = cryptoOpt.get();
        if (!crypto.isActive()) {
            throw new CustomException(ErrorCode.CRYPTO_NOT_ACTIVE);
        }
        return crypto;
    }

    private void checkTotalOrderPriceOrElseThrow(Double marketPrice, Double quantity) {
        if(marketPrice * quantity < minimumOrderPrice){
            throw new CustomException(ErrorCode.TOTAL_ORDER_PRICE_INVALID, minimumOrderPrice);
        }
    }

    private User findUserByIdOrElseThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Wallet findWalletByUserIdAndCryptoIdOrElseThrow(User user, Crypto crypto) {
        return walletRepository.findByUserIdAndCryptoId(user.getId(), crypto.getId())
                .orElseThrow(
                        () -> new CustomException(ErrorCode.WALLET_NOT_FOUND)
                );
    }

    private void withdrawWalletOrElseThrow(Wallet wallet, Double quantity) {
        if (wallet.getQuantity() < quantity) {
            throw new CustomException(ErrorCode.NOT_ENOUGH_CRYPTO);
        }

        wallet.decreaseQuantity(quantity);
    }

    @Transactional(readOnly = true)
    public FindOrderResponse findAllSellOrder(Long userId) {
        findUserByIdOrElseThrow(userId);

        return FindOrderResponse.of(sellOrderRepository.findSellOrdersByUserId(userId));
    }

    /**
     * 판매 주문 취소
     * 1. 유저
     * 2. 판매 주문 조회
     * 3. 지갑 조회
     * 4. 판매 주문이 삭제 가능한지 조회
     * 5. 환불
     * 6. redis 삭제
     * 8. 판매 주문 엔티티 취소됨 업데이트, 저장
     * @param userId
     * @param sellOrderId
     */
    @Transactional
    public void cancelAndRefundSellOrderTransaction(Long userId, Long sellOrderId) {
        User user = findUserByIdOrElseThrow(userId); // 1
        SellOrder sellOrder = findSellOrderByIdOrElseThrow(sellOrderId); // 2
        Wallet wallet = findWalletByUserIdAndCryptoIdOrElseThrow(user, sellOrder.getCrypto()); // 3

        checkSellOrderCanCanceledOrElseThrow(sellOrder); // 4
        refundCrypto(wallet, sellOrder);

        sellOrder.setCanceled(true); // 5

        OrderDto orderDto = createOrderDto(sellOrder, wallet, user, sellOrder.getCrypto());
        deregisterOrderOrElseThrow(orderDto);

        walletRepository.save(wallet); // 6
        sellOrderRepository.save(sellOrder);
    }

    private SellOrder findSellOrderByIdOrElseThrow(Long sellOrderId) {
        return sellOrderRepository.findById(sellOrderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    /**
     * 취소 가능여부 검사, exception
     * @param sellOrder
     */
    private void checkSellOrderCanCanceledOrElseThrow(SellOrder sellOrder) {
        if (sellOrder.isProcessed()) {
            throw new CustomException(ErrorCode.ALREADY_PROCESSED);
        }

        if (sellOrder.isCanceled()) {
            throw new CustomException(ErrorCode.ALREADY_CANCELED);
        }
    }

    private void refundCrypto(Wallet wallet, SellOrder sellOrder) {
        Double quantity = sellOrder.getQuantity();
        wallet.increaseQuantity(quantity);
    }

    private void deregisterOrderOrElseThrow(OrderDto orderDto) {
        boolean result = tradeService.deregisterOrder(orderDto);
        if (!result) {
            throw new CustomException(ErrorCode.ORDER_CANT_BE_CANCELED);
        }
    }
}
