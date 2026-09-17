/*
 * Copyright © 2025-2026 Markus Spann, SpeedBankingDe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.speedbanking.bankdata.refresh;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared background executor for asynchronous country data refreshes.
 * <p>
 * A single, module-wide fixed thread pool of two threads is used for all countries: refresh work
 * is I/O-bound (network download + disk write), and with only a handful of countries shipped in
 * this module there is no benefit in parallelizing beyond two concurrent refreshes. Threads are
 * marked as daemon threads so they never prevent JVM shutdown; there is deliberately no explicit
 * {@code shutdown()} exposed in this version.
 *
 * @since 1.8.11
 */
public final class RefreshExecutors {

    private static final int             POOL_SIZE = 2;

    private static final ExecutorService INSTANCE  = Executors.newFixedThreadPool(POOL_SIZE, new NamedDaemonThreadFactory());

    private RefreshExecutors() {
        throw new UnsupportedOperationException(
            String.format("Utility class %s cannot be instantiated", getClass().getSimpleName()));
    }

    /**
     * Returns the shared executor used for background country data refreshes.
     *
     * @return the shared {@link ExecutorService}
     */
    public static ExecutorService get() {
        return INSTANCE;
    }

    /**
     * Thread factory producing named, daemon background-refresh threads.
     */
    private static final class NamedDaemonThreadFactory implements ThreadFactory {

        private static final String THREAD_NAME_PREFIX = "bankdata-refresh-";

        private final AtomicInteger threadCount        = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, THREAD_NAME_PREFIX + threadCount.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

}
