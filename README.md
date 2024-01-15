# Coin
비트코인 프로젝트 리팩토링

[자세한 내용 노션 링크](https://sungwon9.notion.site/Coin-8724855a1d8a45a38b4d693734be0144?pvs=4)

## 아키텍처
초기 구성은 다음과 같이 계획하였지만, 테스트 후에 변경할 예정입니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/659c74bc-0e7e-4e40-8226-2c54e71c30ca" width="700">

### 중요 요구사항
- (Price Module) 최소 1초에 4번 모든 코인의 가격을 받아와야 한다.
- (Trade Module) 최소 1초에 2번 모든 종류의 코인 가격에대해 매치되는 주문을 검색하고 처리해야한다.

## 모듈의 동작 과정

### Price Module
가격 정보를 받아오는 모듈입니다.

```@Scheduled```, ```@Async``` 를 사용하여 요구사항에 맞게 구현하였습니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/89d51550-b664-402f-98e6-8187369c0463" width="550">

[비동기, 스레드풀 풀 설정](https://github.com/O0oO0Oo/Coin/blob/develop/price/src/main/java/org/coin/price/config/AsyncSchedulingConfiguration.java)

[가격 데이터 요청 스케줄러](https://github.com/O0oO0Oo/Coin/blob/develop/price/src/main/java/org/coin/price/task/PriceRequestTask.java)

[가격 데이터 저장 메세지 큐 구현](https://github.com/O0oO0Oo/Coin/blob/develop/price/src/main/java/org/coin/price/queue/PriceMessageWindowBlockingQueue.java)
<details>
<summary>Price Module 핵심 코드</summary>
<div markdown="1">
   
```java
// 비동기, 스레드 풀 생성
@Slf4j
@EnableAsync
@EnableScheduling
@Configuration
@RequiredArgsConstructor
public class AsyncSchedulingConfiguration implements AsyncConfigurer {
    @Value("${module.price.rps}")
    private int rps;
    private final AsyncSchedulingExceptionHandler asyncSchedulingExceptionHandler;

    /**
     * Thread = rps x response time
     * rps : 초당 4번 요청
     * response time : PriceRequestTask 는 1초 미만의 응답시간(88 ~ 771ms), 1초로 설정
     */
    @Override
    @Bean(name = "priceRequestTaskExecutor")
    public Executor getAsyncExecutor() {
        log.debug("Creating Async Task Executor");
        ThreadPoolTaskScheduler executor = new ThreadPoolTaskScheduler();
        executor.setPoolSize(rps);
        executor.setThreadNamePrefix("pool-price-thread-");
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return asyncSchedulingExceptionHandler;
    }
}

// @Async, @Scheduled 를 사용하여 가격 데이터 API 요청
@Service
@RequiredArgsConstructor
public class PriceRequestTask {
    private final ApplicationEventPublisher eventPublisher;
    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${module.price.url}")
    private String API_URL;

    @Async("priceRequestTaskExecutor")
    @Scheduled(fixedRate = 250L)
    public void requestScheduler() {
        PriceApiRequest priceApiRequest = apiRequest();

        eventPublisher.publishEvent(PriceMessageProduceEvent.of(priceApiRequest));
        eventPublisher.publishEvent(AsyncSchedulingFailureCountEvent.success());
    }

    private PriceApiRequest apiRequest() {
        return restTemplate.getForObject(API_URL, PriceApiRequest.class);
    }
}

// 최종적으로 스레드 세이프한 자료구조를 사용하여 produce(PriceMessageProduceEvent event) 통해 저장됨
@Slf4j
@Component
public class PriceMessageWindowBlockingQueue implements MessageQueue<PriceMessageProduceEvent, List<CryptoCoin>> {
    private ConcurrentHashMap<String, PriorityBlockingQueue<CryptoCoin>> priceHashMapPriorityQueue = new ConcurrentHashMap<>();
    private ArrayList<String> coins = new ArrayList<>();
    private final AtomicInteger coinsIndex = new AtomicInteger(0);
    @Value("${module.price.initial-queue-size}")
    private int queueSize;
    @Value("${module.price.price-window-size}")
    private int windowSize;
    private final Map<String, ReentrantLock> reentrantLockMap = new HashMap<>();

    @PostConstruct
    void init() {
        log.debug("PriceMessageBlockingQueue init.");

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("BaseCryptoList.txt")) {
            coins = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.toCollection(ArrayList::new));
        } catch (Exception e) {
            log.error("PriceMessageBlockingQueue PostConstruct Failed. : {}", e.getMessage());
        }

        coins.forEach(coinName -> {
            priceHashMapPriorityQueue.put(coinName, new PriorityBlockingQueue<>(queueSize, new CryptoCoinComparator()));
            reentrantLockMap.put(coinName, new ReentrantLock());
        });
    }

    // ##-----------------------  저장 -----------------------##
    @Override
    public void produce(PriceMessageProduceEvent event) {
        Long timestamp = event.timestamp();
        Map<String, PriceApiRequest.PriceData> priceDataMap = event.priceDataMap();

        priceDataMap.forEach((key, value) -> {
            CryptoCoin coin = buildCryptoCoin(key, value, timestamp);
            addPricePriorityBlockingQueue(key, coin);
        });
    }

    private CryptoCoin buildCryptoCoin(String key, PriceApiRequest.PriceData value, Long timestamp) {
        return CryptoCoin.builder()
                .price(value.getClosing_price())
                .coinName(key)
                .timestamp(timestamp)
                .build();
    }

    private void addPricePriorityBlockingQueue(String key, CryptoCoin coin) {
        this.priceHashMapPriorityQueue.computeIfPresent(key, (k, blockingQueue) -> {
            blockingQueue.put(coin);
            return blockingQueue;
        });
    }

    @Override
    public List<CryptoCoin> consume() {
        return tumblingWindow(
                getCoinName()
        );
    }

    private String getCoinName() {
        return coins.get(getCoinsIndex());
    }

    private int getCoinsIndex() {
        return coinsIndex.getAndAccumulate(
                1,
                (current, update) -> {
                    if (current < coins.size() - 1) {
                        return current + update;
                    }
                    return 0;
                });
    }

    // 중복 데이터 제거를 통한 최적화
    private List<CryptoCoin> tumblingWindow(String name) {
        if (reentrantLockMap.get(name).tryLock()) {
            try {
                PriorityBlockingQueue<CryptoCoin> coinBlockingQueue = priceHashMapPriorityQueue.get(name);
                Map<Double, CryptoCoin> windowMap = new HashMap<>(windowSize + 1, 1.0f);

                while (windowMap.keySet().size() < windowSize && coinBlockingQueue.peek() != null) {
                    CryptoCoin coin = coinBlockingQueue.poll();
                    windowMap.put(coin.getPrice(), coin);
                }
                return windowMap.values().stream().toList();
            } finally {
                reentrantLockMap.get(name).unlock();
            }
        } else {
            return Collections.emptyList();
        }
    }
}
```
</div>
</details>

### Trade Module - EventLoop - 문제 해결 - [Issue-29 - 이벤트 루프 구현과정](https://github.com/O0oO0Oo/Coin/issues/29)

Price 모듈로부터 받아온 가격정보와 매치되는 주문들을 검색하여 거래하는 모듈입니다.

```이벤트루프``` 구조를 모방하여 만들었으며, Redis 에서 ```비동기```로 데이터를 받아와 성공/실패에 따라 이벤트를 발행하고 큐에 저장하게 됩니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/ab37f409-c25c-48cc-bb42-6072d6b3b151" width="900">

Redisson 의 Netty 스레드를 사용하여 Redis 와 비동기적으로 통신합니다.

[Redis lua script](https://github.com/O0oO0Oo/Coin/tree/develop/trade/src/main/resources/lua)

[Redis 비동기 락 구현](https://github.com/O0oO0Oo/Coin/blob/develop/trade/src/main/java/org/coin/trade/pipeline/eventloop/redis/OrderLock.java)

[Redis 비동기 처리](https://github.com/O0oO0Oo/Coin/blob/develop/trade/src/main/java/org/coin/trade/pipeline/eventloop/script/ReadOrderScript.java)

<details>
<summary>Trade Module - EventLoop 구현 코드 </summary>
<div markdown="1">

```java
// 이벤트 큐
public abstract class AbstractEventQueue implements EventQueue {
    protected final BlockingQueue<Event> events;

    protected AbstractEventQueue(BlockingQueue<Event> events) {
        this.events = events;
    }
}

/**
 * 주문을 읽기 위한 이벤트 큐
 */
@Component
public class ReadEventQueue extends AbstractEventQueue {
    @Autowired
    protected ReadEventQueue(@Qualifier("readEventBlockingQueue") BlockingQueue<Event> events) {
        super(events);
    }

    @Override
    public Optional<Event> next() throws InterruptedException {
        return Optional.of(events.take());
    }

    @Override
    public void add(Event event) {
        events.add(event);
    }
}

/**
 * 이벤트 루프
 * 루프를 돌면서 이벤트 큐에서 작업을 가져와 이벤트 핸들러로 처리한다.
 */
public interface EventLoop {
    /**
     * 이벤트 루프 스타트
     */
    void start();

    /**
     * 이벤트 루프 종료
     */
    void stop();
}

public abstract class AbstractEventLoop implements EventLoop {
    protected final AtomicBoolean alive = new AtomicBoolean(true);
    protected final EventQueue eventQueue;
    protected final EventHandler eventHandler;

    protected AbstractEventLoop(EventQueue eventQueue, EventHandler eventHandler) {
        this.eventQueue = eventQueue;
        this.eventHandler = eventHandler;
    }
}

/**
 * 주문 읽기 이벤트 루프, 이벤트가 들어오면 등록된 이벤트 핸들러를 통해 이벤트를 처리한다.
 */
@Component
public class ReadEventLoop extends AbstractEventLoop {
    @Autowired
    public ReadEventLoop(@Qualifier("readEventQueue") EventQueue eventQueue,
                         @Qualifier("tradePipelineEventHandler") EventHandler eventHandler) {
        super(eventQueue, eventHandler);
    }

    public void start() {
        while (alive.get()) {
            try {
                eventQueue.next().ifPresent(eventHandler::handle);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void stop() {
        alive.set(false);
    }
}
```

</div>
</details>

<details>
<summary>Trade Module - EventLoop - Redis 비동기 처리 </summary>
<div markdown="1">

```java

// ## -------------------- 락 구현 ------------------------ ##
@Slf4j
public class OrderLock implements Lock{
    private RedissonClient redissonClient;
    private List<CryptoCoin> prices;
    private CompletableFuture<List<?>> lockFuture;
    private List<LockResultDto> lockResultDtoList = new ArrayList<>();
    private int leaseTime;

    public List<LockResultDto> getLockResultDtoList() {
        return lockResultDtoList;
    }

    public OrderLock(RedissonClient redissonClient, List<CryptoCoin> prices, int leaseTime) {
        this.redissonClient = redissonClient;
        this.prices = prices;
        this.leaseTime = leaseTime;
    }

    @Override
    public boolean tryLock() {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        // ThreadId
        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);

        prices.forEach(price -> {
            String key = "lock:order:" + price.getCoinName() + ":" + price.getPrice();
            keys.add(key);
            values.add(String.valueOf(price.getTimestamp()));
        });

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        List<String> result = script.eval(RScript.Mode.READ_WRITE, TradeLua.LOCK_WRITE_HISTORY, RScript.ReturnType.MULTI, keys, values.toArray(new Object[0]));

        if(result.isEmpty()){
            return false;
        }
        result.forEach(res -> lockResultDtoList.add(LockResultDto.of(res)));
        return true;
    }

    /**
     * 비동기 락 리스트 획득
     * lock:order:BTC:6.3766E7:1704929698503:1704929698503 다음과 같이 획득한 락에 대해 주문을 검색 가능하다.
     * @return
     */
    public CompletableFuture<List<?>> tryLockAsync() {
        List<Object> keys = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        values.add(String.valueOf(Thread.currentThread().getId()));
        values.add(leaseTime);

        prices.forEach(price -> {
            String key = "lock:order:" + price.getCoinName() + ":" + price.getPrice();
            keys.add(key);
            values.add(String.valueOf(price.getTimestamp()));
        });

        // 다음과 같이 [lock:order:BTC:6.3766E7:1704929698503:1704929698503, lock:order:BTC:6.3755E7:1704929698259:1704929698259, lock:order:BTC:6.3759E7:1704929698018:1704929698018]
        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        lockFuture = script.evalAsync(RScript.Mode.READ_WRITE, TradeLua.LOCK_WRITE_HISTORY, RScript.ReturnType.MULTI, keys, values.toArray(new Object[0]))
                .thenApply(res -> {
                    // Lock 데이터 받아서 초기화
                    if (res instanceof List<?> resString && (!resString.isEmpty())) {
                        resString.forEach(lockString -> lockResultDtoList.add(LockResultDto.of((String) lockString)));
                        return resString;
                    }
                    return Collections.emptyList();
                }).toCompletableFuture();
        return lockFuture;
    }

    public RFuture<Boolean> unlockAsync() {
        List<Object> keys = new ArrayList<>();

        lockResultDtoList.forEach(dto -> keys.add(dto.lockKey()));

        RScript script = redissonClient.getScript(StringCodec.INSTANCE);
        return script.evalAsync(RScript.Mode.READ_WRITE, TradeLua.UNLOCK_DELETE_HISTORY, RScript.ReturnType.BOOLEAN, keys);
    }

    public CompletableFuture<List<?>> getLockFuture() {
        return lockFuture;
    }

    /**
     * Not Used
     */
    @Override
    public void unlock() {
    }

    /**
     * Not Used
     */
    @Override
    public void lock() {

    }

    /**
     * Not Used
     * @throws InterruptedException
     */
    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    /**
     * Not Used
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return
     * @throws InterruptedException
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    /**
     * Not Used
     * @return
     */
    @Override
    public Condition newCondition() {
        return null;
    }
}


// # ------------------------ Redis 주문 읽기 ------------------------- #
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadOrderScript implements Script<List<CryptoCoin>> {
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher eventPublisher;
    private final BatchOptions batchOptions = BatchOptions.defaults();

    @Override
    public void run(List<CryptoCoin> prices, Consumer<List<CryptoCoin>> onSuccess, BiConsumer<Throwable, List<CryptoCoin>> onFailure) {
        if (prices.isEmpty()) {
            return;
        }

        // 락 시도
        OrderLock lock = new OrderLock(redissonClient, prices, 6000);
        lock.tryLockAsync()
                .thenAccept(lockResult -> {
                    // 획득한 락이 없다면 실행 x
                    if (lockResult.isEmpty()) {
                        return;
                    }
                    // 획득한 락 종류에 대해 읽기 수행
                    executeBatchOperation(lock);
                    // 성공
                    onSuccess.accept(prices);
                })
                .exceptionally(throwable -> {
                    // 실패, 읽기 다시시도
                    eventPublisher.publishEvent(new ReadOrderEvent(prices));
                    onFailure.accept(throwable, prices);
                    return null;
                });
    }

    private void executeBatchOperation(OrderLock lock) {
        RBatch batch = redissonClient.createBatch(batchOptions);
        createWriteEvent(lock, batch);
        batch.executeAsync();
    }

    private void createWriteEvent(OrderLock lock, RBatch batch) {
        List<CompletableFuture<Optional<?>>> completableFutures = lock.getLockResultDtoList()
                .stream().flatMap(lockResultDto ->
                        Stream.of(
                                /**
                                 * 다음과 같이 주문들을 검색하고 없다면 Optional 을 반환하여 후에 CompletableFuture 에서 처리한다.
                                 * ZRANGEBYSCORE 로 10시0분0초에 BTC는 5000원 이였다 이를 기반으로 유저가 등록한 주문 중 10시0분0초 까지 등록한 주문을 검색한다.
                                 * ZRANGEBYSCORE key = ..BTC:4000 score 0 ~ 10시0분0초
                                 * 또는
                                 * 다른 스레드에서 락을 획득했다면
                                 * 스레드 1번 ZRANGEBYSCORE key = ..BTC:4000 score 0 ~ 10시0분0초
                                 * 스레드 2번 ZRANGEBYSCORE key = ..BTC:4000 score 10시0분0초 ~ 10시0분10초
                                 * 다음과 같이 검색하여 주문이 중복처리 되지 않도록 한다.
                                 */
                                // buy orders
                                batch.getScoredSortedSet(lockResultDto.buyOrderKey())
                                        .valueRangeAsync(lockResultDto.beginTimestamp(), false, lockResultDto.endTimestamp(), true)
                                        .thenApply(res -> res.isEmpty() ? Optional.empty() : Optional.of(OrderSortedSetDto.of("buy", lockResultDto, res)))
                                        .toCompletableFuture(),
                                // sell orders
                                batch.getScoredSortedSet(lockResultDto.sellOrderKey())
                                        .valueRangeAsync(lockResultDto.beginTimestamp(), false, lockResultDto.endTimestamp(), true)
                                        .thenApply(res -> res.isEmpty() ? Optional.empty() : Optional.of(OrderSortedSetDto.of("sell", lockResultDto, res)))
                                        .toCompletableFuture()
                        )
                ).toList();

        CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> completableFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList())
                .thenAccept(results -> {
                    // 데이터가 있다면 쓰기위해 이벤트 발행.
                    if (!results.isEmpty()) {
                        eventPublisher.publishEvent(new WriteOrderEvent(ReadOrderDto.of(lock, (List<OrderSortedSetDto>) results)));
                    }
                    else {
                        // 읽기 후, 읽은 데이터가 없다면 바로 unlock
                        lock.unlockAsync();
                    }
                });
    }
}
```

</div>
</details>

### Trade Module - Async Recurrsion Loop - 문제 발생 - [Issue-26 - 문제 발생 및 해결과정](https://github.com/O0oO0Oo/Coin/issues/26)

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/d08c1a22-650e-4d5d-aa03-728c20b96ada" width="900">

Reader, Writer 는 아래의 추상클라스를 상속받아 구현하였습니다.

[비동기 재귀 루프 추상 클래스](https://github.com/O0oO0Oo/Coin/blob/develop/trade/src/main/java/org/coin/trade/pipeline/asyncloop/loop/AbstractAsyncRecursionLoop.java)

<details>
<summary>Trade Module - Async Recurrsion Loop 구현 코드 </summary>
<div markdown="1">

```java
// 비동기 루프 인터페이스
public interface AsyncLoop {
    void runAsyncLoop(int count);
    void stopAsyncLoop();
}

/**
 * 비동기 루프 추상 클래스 입니다.
 * 작업을 완료 후 재귀로 루프를 돌며 스택트레이스를 추적하여 특정 사이즈가 넘어가면 다른 스레드 풀로 작업을 넘겨 오버플로를 방지합니다.
 * <p>
 * 1. O processResult(I result) 결과 처리 메서드
 * <p>
 * 2. doConcurrencyLevelControl(O result) 다른 스레드와 동기화 조정이 필요할 때 동작을 정의하는 메서드
 * <p>
 * 3. doHandlerError(Throwable throwable) 예외 발생 처리 메서드
 * 4. shouldStopAsyncLoop() 예외 발생했을때 멈춰야하는 조건을 정의
 * <p>
 * 네 가지를 구현해야 합니다.
 * @param <I> Supplier 에서 반환하는 데이터의 타입
 * @param <O> 이전의 결과를 바탕으로 동시성을 조정할 데이터의 타입
 */
@Slf4j
public abstract class AbstractAsyncRecursionLoop<I, O> implements AsyncLoop {
    private ExecutorService mainThreadPool;
    private ExecutorService swapThreadPool;
    private Supplier<I> loopSupplier;

    private final AtomicBoolean atomicChanger = new AtomicBoolean();
    private final Map<Boolean, ExecutorService> threadPoolMap = new HashMap<>();

    @Value("${module.thread-pool.stack-trace-size}")
    private int stackTraceSize;

    /**
     * loopSupplier 로부터 읽어온 결과 처리
     * @param result
     * @return
     */
    protected abstract O processResult(I result);

    /**
     * 동시성 수준 조정, 작업이 밀리면 루프를 멈추고 해결되면 재시작하기 위함.
     */
    protected abstract CompletableFuture<Void> doConcurrencyLevelControl(O result);
    private CompletableFuture<Void> concurrencyLevelControl(O result){
        CompletableFuture<Void> voidCompletableFuture = doConcurrencyLevelControl(result);
        checkStackTraceThenSwapThreadPool();
        return voidCompletableFuture;
    }

    /**
     * 예외 처리
     * @param throwable
     * @return
     */
    protected abstract Void doHandlerError(Throwable throwable);
    /**
     * 예외 발생시 루프를 정지할 수 있는 조건을 정의
     * @return true 는 루프 정지, false 는 루프 계속 동작
     */
    protected abstract boolean shouldStopAsyncLoop();
    private Void handlerError(Throwable throwable){
        try {
            return doHandlerError(throwable);
        }
        finally {
            if(shouldStopAsyncLoop()) {
                stopAsyncLoop();
            }
            else {
                checkStackTraceThenSwapThreadPool();
            }
        }
    }

    /**
     * Stack Trace 사이즈 체크, 사이즈가 stackTraceSize 넘어가면 다른 스레드풀로 넘기기.
     */
    private void checkStackTraceThenSwapThreadPool() {
        if (Thread.currentThread().getStackTrace().length < stackTraceSize) {
            asyncLoop();
        } else {
            CompletableFuture.runAsync(this::asyncLoop, threadPoolMap.get(atomicChanger.getAndSet(!atomicChanger.get())));
        }
    }


    /**
     * @param count 시작 루프의 수를 결정
     */
    @Override
    public void runAsyncLoop(int count) {
        threadPoolMap.put(Boolean.TRUE, mainThreadPool);
        threadPoolMap.put(Boolean.FALSE, swapThreadPool);
        for (int i = 0; i < count; i++) {
            asyncLoop();
        }
    }

    private void asyncLoop() {
        CompletableFuture<I> cf = CompletableFuture.supplyAsync(
                loopSupplier, mainThreadPool
        );

        cf
                .thenApply(this::processResult)
                .thenAccept(this::concurrencyLevelControl)
                .exceptionally(this::handlerError);
    }

    /**
     * 루프 정지
     * TODO : 언제 정지해야할까?
     */
    @Override
    public void stopAsyncLoop() {
        log.info("ThreadPool shutdown.");
        mainThreadPool.shutdown();
        try {
            if (!mainThreadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                mainThreadPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            mainThreadPool.shutdownNow();
        }
    }

    public void setSwapThreadPool(ExecutorService swapThreadPool) {
        this.swapThreadPool = swapThreadPool;
    }

    protected void setMainThreadPool(ExecutorService mainThreadPool) {
        this.mainThreadPool = mainThreadPool;
    }

    protected void setLoopSupplier(Supplier<I> loopSupplier) {
        this.loopSupplier = loopSupplier;
    }
}
```
   
</div>
</details>

### User Module
Price, Trade 모듈을 통해 주문등록, 아래와 같이 처리된 주문의 저장 그리고 전반적인 유저의 요청들을 처리합니다.

<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/9f3f6b12-6aa5-4867-aa2b-3480bbdd8ef0" width="550">

### Issue
1. 처음 계획했던 루프 구조 문제점 문제해결 과정 : [Issue-26](https://github.com/O0oO0Oo/Coin/issues/26)
   
### ERD
- crypto : 코인 종류
- wallet : 유저 보유 코인지갑
- sell/buy_order : 유저의 판매/구매
- user : 사용자
<img src="https://github.com/O0oO0Oo/Coin/assets/110446760/74aa375c-abba-4342-adcc-e407e391e169" width="550">
