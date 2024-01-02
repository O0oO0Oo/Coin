package org.coin.trade.pipeline;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * 주문 처리를 위한 로직 테스트
 */
class PipelineSynchronizerTest {
    private static final Logger logger = LoggerFactory.getLogger(PipelineSynchronizerTest.class);
    private Random r = new Random();
    private BlockingQueue<Integer> readBlockingQueue = new LinkedBlockingQueue<>();
    private BlockingQueue<String> processedBlockingQueue = new LinkedBlockingQueue<>();


    @Test
    @DisplayName("Phaser Test")
    void phaser_test() throws InterruptedException {
        final int NUMBER_OF_THREADS_PHASER = 4;
        Phaser phaser = new Phaser(5);

        ReadTask readTask = new ReadTask(phaser, 4);
        ProcessTask processTask = new ProcessTask(phaser, 1);
        WriteTask writeTask = new WriteTask(phaser, 2);

        readTask.init();
        processTask.init();
        writeTask.init();

        Thread.sleep(200000);
    }

    private class ReadTask implements Runnable{
        private Phaser phaser;
        private final int THREAD_POOL_SIZE;
        ExecutorService ex;
        public ReadTask(Phaser phaser, int threadPoolSize) {
            this.phaser = phaser;
            this.THREAD_POOL_SIZE = threadPoolSize;
        }

        public void init() {
            ExecutorService ex = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            IntStream.range(0,THREAD_POOL_SIZE).forEach(i -> ex.submit(this));
        }

        @Override
        public void run() {
            try {
                read();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void read() throws InterruptedException {
            do {
                // write 가 밀리면 read 작업 정지
                if (phaser.getRegisteredParties() > 15) {
                    logger.warn("----------stop read task----------");
                    phaser.arriveAndAwaitAdvance();
                    logger.info("----------start read task----------");
                }

                // parties 증가
                phaser.register();

                // Read Redis
                Thread.sleep(r.nextInt(100,800));
                readBlockingQueue.put(r.nextInt(1,10000));

            } while (true);
        }
    }

    private class ProcessTask implements Runnable{
        private Phaser phaser;
        private final int THREAD_POOL_SIZE;
        ExecutorService ex;
        public ProcessTask(Phaser phaser, int THREAD_POOL_SIZE) {
            this.phaser = phaser;
            this.THREAD_POOL_SIZE = THREAD_POOL_SIZE;
        }

        public void init() {
            ex = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            IntStream.range(0, THREAD_POOL_SIZE).forEach(i -> ex.submit(this));
        }

        @Override
        public void run() {
            try {
                process();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        private void process() throws InterruptedException {
            do {
                Integer data;
                if (!readBlockingQueue.isEmpty()) {
                    data = readBlockingQueue.poll();
                    processedBlockingQueue.put(data + " is processed");
                }
            } while (true);
        }
    }


    private class WriteTask implements Runnable{
        private Phaser phaser;
        private final int THREAD_POOL_SIZE;
        ExecutorService ex;
        public WriteTask(Phaser phaser, int THREAD_POOL_SIZE) {
            this.phaser = phaser;
            this.THREAD_POOL_SIZE = THREAD_POOL_SIZE;
        }

        public void init() {
            ex = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
            IntStream.range(0, THREAD_POOL_SIZE).forEach(i -> ex.submit(this));
        }

        @Override
        public void run() {
            while (true) {
                String data;
                if (!processedBlockingQueue.isEmpty()) {
                    data = processedBlockingQueue.poll();

                    // 처리되는데 걸리는 시간
                    try {
                        if (data.startsWith("1")) {
                            logger.info("wait, large data");
                            Thread.sleep(1000);
                        }
                        Thread.sleep(r.nextInt(80,200));
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    logger.info("data " + data + "/ processed queue sizs :" + processedBlockingQueue.size());
                    System.out.println(phaser.getPhase() + " " + phaser.getRegisteredParties() + " " + phaser.getArrivedParties());
                    if (phaser.getRegisteredParties() > 15) {
                        phaser.arrive();
                    }

                    if(phaser.getRegisteredParties() > 1) {
                        phaser.arriveAndDeregister();
                    }
                }
            }
        }
    }
}

