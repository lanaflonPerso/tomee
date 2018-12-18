package org.superbiz.executor;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedThreadFactory;
import javax.enterprise.context.RequestScoped;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@RequestScoped
public class ThreadFactoryService {

    private static final Logger LOGGER = Logger.getLogger(ThreadFactoryService.class.getSimpleName());

    @Resource
    private ManagedThreadFactory factory;

    /**
     * Happy path.
     *
     * @param value to compute
     * @return The thread we created
     */
    public int asyncTask(final int value) throws InterruptedException {
        LOGGER.info("Create asyncTask");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final LongTask longTask = new LongTask(value, 1000000, countDownLatch);

        final Thread thread = factory.newThread(longTask);
        thread.setName("pretty asyncTask");
        thread.start();

        countDownLatch.await(200, TimeUnit.MILLISECONDS);

        return longTask.getResult();
    }

    /**
     * Example where we have to stop a thread.
     *
     * @param value
     * @return The thread we created
     * @throws InterruptedException
     */
    public int asyncHangingTask(final int value) throws InterruptedException {
        LOGGER.info("Create asyncHangingTask");
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        final LongTask longTask = new LongTask(value, 1000000, countDownLatch);

        final Thread thread = factory.newThread(longTask);
        thread.setName("pretty asyncHangingTask");
        thread.start();

        countDownLatch.await(200, TimeUnit.MILLISECONDS);

        if (thread.isAlive()) {
            // This will cause any wait in the thread to resume.
            // This will call the InterruptedException block in the longRunnableTask method.
            thread.interrupt();
        }
        return longTask.getResult();
    }

    public static class LongTask implements Runnable {
        private final int value;
        private final long taskDurationMs;
        private final CountDownLatch countDownLatch;
        private int result;

        public LongTask(final int value,
                        final long taskDurationMs,
                        final CountDownLatch countDownLatch) {
            this.value = value;
            this.taskDurationMs = taskDurationMs;
            this.countDownLatch = countDownLatch;
        }

        public int getResult() {
            return result;
        }

        @Override
        public void run() {
            try {
                // Simulate a long processing task using TimeUnit to sleep.
                TimeUnit.MILLISECONDS.sleep(taskDurationMs);
            } catch (InterruptedException e) {
                throw new RuntimeException("Problem while waiting");
            }

            result = value + 1;
            LOGGER.info("longRunnableTask complete. Value is " + result);
            countDownLatch.countDown();
            // Cannot return result with a Runnable. Must store and access it later.
        }
    }
}
