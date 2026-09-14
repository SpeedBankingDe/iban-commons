package de.speedbanking.bankdata.refresh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link RefreshExecutors}.
 */
@SuppressWarnings("checkstyle:MethodName")
final class RefreshExecutorsTest {

    @Test
    void get_returnsSameSharedInstanceEveryTime() {
        ExecutorService first = RefreshExecutors.get();
        ExecutorService second = RefreshExecutors.get();

        assertThat(first).isSameAs(second);
    }

    @Test
    void get_executesSubmittedTaskOnNamedDaemonThread() throws InterruptedException {
        AtomicReference<Thread> executingThread = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        RefreshExecutors.get().execute(() -> {
            executingThread.set(Thread.currentThread());
            latch.countDown();
        });

        latch.await();
        assertThat(executingThread.get().getName()).startsWith("bankdata-refresh-");
        assertThat(executingThread.get().isDaemon()).isTrue();
    }

    @Test
    void constructor_private_throwsUnsupportedOperationException() throws NoSuchMethodException {
        Constructor<RefreshExecutors> constructor = RefreshExecutors.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertThat(exception.getCause()).isInstanceOf(UnsupportedOperationException.class);
    }

}
