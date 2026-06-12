package com.xingchen.backend.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RedisLockUtil 单元测试
 * <p>
 * 测试策略：
 * - RedisLockUtil 使用 RedisTemplate.execute() 执行 Lua 脚本
 * - 通过 mock redisTemplate.execute() 的返回值来模拟不同的锁状态
 * - tryLock 执行 TRY_LOCK_SCRIPT，返回 Long 值（1=成功，0=失败）
 * - unlock 执行 RELEASE_LOCK_SCRIPT
 * - completeLock 执行 COMPLETE_LOCK_SCRIPT
 * - 使用 doAnswer/doReturn 方式 mock execute() 方法以兼容 varargs 签名
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisLockUtil 测试")
class RedisLockUtilTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    private RedisLockUtil redisLockUtil;

    @BeforeEach
    void setUp() {
        redisLockUtil = new RedisLockUtil(redisTemplate);
    }

    /**
     * 通用 mock：execute 始终返回 1L（锁操作成功）
     */
    @SuppressWarnings("unchecked")
    private void stubExecuteSuccess() {
        doReturn(1L).when(redisTemplate).execute(any(), any(), (Object[]) any());
    }

    /**
     * 通用 mock：execute 始终返回 0L（锁已被占用）
     */
    @SuppressWarnings("unchecked")
    private void stubExecuteFailure() {
        doReturn(0L).when(redisTemplate).execute(any(), any(), (Object[]) any());
    }

    /**
     * 通用 mock：通过回调控制返回值
     */
    @SuppressWarnings("unchecked")
    private void stubExecuteWithAnswer(org.mockito.stubbing.Answer<Long> answer) {
        doAnswer(answer).when(redisTemplate).execute(any(), any(), (Object[]) any());
    }

    // ==================== executeWithLock 测试 ====================

    @Nested
    @DisplayName("executeWithLock - 带锁执行业务逻辑")
    class ExecuteWithLockTests {

        @Test
        @DisplayName("成功获取锁并执行 supplier")
        void executeWithLock_SuccessfulLockAcquisition() {
            // Arrange
            String key = "test-key";
            Supplier<String> supplier = () -> "result";
            stubExecuteSuccess();

            // Act
            String result = redisLockUtil.executeWithLock(key, supplier);

            // Assert
            assertEquals("result", result);
            // tryLock + completeLock = 至少 2 次调用
            verify(redisTemplate, atLeast(2)).execute(any(), any(), (Object[]) any());
        }

        @Test
        @DisplayName("锁已被占用，等待重试后获取成功")
        void executeWithLock_LockAlreadyHeldWaitsAndRetries() {
            // Arrange
            String key = "retry-key";
            AtomicInteger callCount = new AtomicInteger(0);
            Supplier<String> supplier = () -> "result-after-retry";

            stubExecuteWithAnswer(invocation -> {
                int count = callCount.incrementAndGet();
                if (count <= 2) {
                    return 0L; // 锁已被占用
                }
                return 1L; // 第 3 次获取成功
            });

            // Act - 使用较短的重试间隔
            String result = redisLockUtil.executeWithLock(key, 30, 5, 50, supplier);

            // Assert
            assertEquals("result-after-retry", result);
            assertTrue(callCount.get() >= 3, "should have retried at least 3 times");
        }

        @Test
        @DisplayName("锁超时无法获取，抛出异常")
        void executeWithLock_LockTimeoutThrowsException() {
            // Arrange
            String key = "timeout-key";
            Supplier<String> supplier = () -> "should-not-reach";
            stubExecuteFailure();

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> redisLockUtil.executeWithLock(key, 30, 3, 50, supplier));
            assertTrue(exception.getMessage().contains("系统繁忙"),
                    "异常消息应包含'系统繁忙'");
        }

        @Test
        @DisplayName("supplier 抛出异常，锁仍然被释放")
        void executeWithLock_SupplierThrowsExceptionLockStillReleased() {
            // Arrange
            String key = "exception-key";
            AtomicInteger callCount = new AtomicInteger(0);

            stubExecuteWithAnswer(invocation -> {
                callCount.incrementAndGet();
                return 1L; // 始终返回成功
            });

            Supplier<String> failingSupplier = () -> {
                throw new IllegalStateException("业务异常");
            };

            // Act & Assert
            IllegalStateException exception = assertThrows(IllegalStateException.class,
                    () -> redisLockUtil.executeWithLock(key, failingSupplier));
            assertEquals("业务异常", exception.getMessage());

            // 验证至少被调用 2 次（tryLock + unlock）
            assertTrue(callCount.get() >= 2, "should have called execute at least twice (tryLock + unlock)");
        }

        @Test
        @DisplayName("executeWithLock 无返回值版本执行 Runnable")
        void executeWithLock_RunnableVersion() {
            // Arrange
            String key = "runnable-key";
            AtomicBoolean executed = new AtomicBoolean(false);
            Runnable runnable = () -> executed.set(true);
            stubExecuteSuccess();

            // Act
            redisLockUtil.executeWithLock(key, runnable);

            // Assert
            assertTrue(executed.get(), "runnable 应该被执行");
        }
    }

    // ==================== tryLock 测试 ====================

    @Nested
    @DisplayName("tryLock - 尝试获取锁")
    class TryLockTests {

        @Test
        @DisplayName("成功获取锁")
        void tryLock_SuccessfulAcquisition() {
            // Arrange
            String key = "lock-key";
            stubExecuteSuccess();

            // Act
            boolean result = redisLockUtil.tryLock(key);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("锁已被占用，返回 false")
        void tryLock_LockAlreadyHeldReturnsFalse() {
            // Arrange
            String key = "occupied-key";
            stubExecuteFailure();

            // Act
            boolean result = redisLockUtil.tryLock(key);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("execute 返回 null 时，返回 false")
        void tryLock_NullResultReturnsFalse() {
            // Arrange
            String key = "null-key";
            doReturn(null).when(redisTemplate).execute(any(), any(), (Object[]) any());

            // Act
            boolean result = redisLockUtil.tryLock(key);

            // Assert
            assertFalse(result);
        }

        @Test
        @DisplayName("使用自定义过期时间获取锁")
        void tryLock_WithCustomExpireSeconds() {
            // Arrange
            String key = "custom-expire-key";
            long expireSeconds = 60;
            stubExecuteSuccess();

            // Act
            boolean result = redisLockUtil.tryLock(key, expireSeconds);

            // Assert
            assertTrue(result);
        }

        @Test
        @DisplayName("tryLockWithRetry 第一次就成功")
        void tryLockWithRetry_FirstAttemptSuccess() {
            // Arrange
            String key = "retry-success-key";
            stubExecuteSuccess();

            // Act
            boolean result = redisLockUtil.tryLockWithRetry(key, 30, 3, 100);

            // Assert
            assertTrue(result);
            // 只调用了一次 execute（第一次就成功了）
            verify(redisTemplate, times(1)).execute(any(), any(), (Object[]) any());
        }

        @Test
        @DisplayName("tryLockWithRetry 所有重试都失败")
        void tryLockWithRetry_AllAttemptsFail() {
            // Arrange
            String key = "retry-fail-key";
            stubExecuteFailure();

            // Act
            boolean result = redisLockUtil.tryLockWithRetry(key, 30, 3, 50);

            // Assert
            assertFalse(result);
            // 调用了 3 次 execute
            verify(redisTemplate, times(3)).execute(any(), any(), (Object[]) any());
        }
    }

    // ==================== unlock 测试 ====================

    @Nested
    @DisplayName("unlock - 释放锁")
    class UnlockTests {

        @Test
        @DisplayName("释放已获取的锁")
        void unlock_ReleasesAcquiredLock() {
            // Arrange
            String key = "release-key";
            stubExecuteSuccess();

            // Act
            redisLockUtil.unlock(key);

            // Assert - 没有异常抛出
            verify(redisTemplate).execute(any(), any(), (Object[]) any());
        }

        @Test
        @DisplayName("释放未持有的锁不抛出异常")
        void unlock_ReleasingNonHeldLockDoesNotThrow() {
            // Arrange
            String key = "non-held-key";
            stubExecuteFailure();

            // Act & Assert - 不应该抛出异常
            assertDoesNotThrow(() -> redisLockUtil.unlock(key));
        }

        @Test
        @DisplayName("释放锁时 Redis 异常不抛出")
        void unlock_RedisExceptionDoesNotThrow() {
            // Arrange
            String key = "exception-key";
            doThrow(new RuntimeException("Redis connection error"))
                    .when(redisTemplate).execute(any(), any(), (Object[]) any());

            // Act & Assert - 不应该抛出异常
            assertDoesNotThrow(() -> redisLockUtil.unlock(key));
        }
    }

    // ==================== completeLock 测试 ====================

    @Nested
    @DisplayName("completeLock - 标记锁完成")
    class CompleteLockTests {

        @Test
        @DisplayName("成功标记锁完成")
        void completeLock_Success() {
            // Arrange
            String key = "complete-key";
            stubExecuteSuccess();

            // Act
            redisLockUtil.completeLock(key);

            // Assert
            verify(redisTemplate).execute(any(), any(), (Object[]) any());
        }

        @Test
        @DisplayName("标记完成时 Redis 异常不抛出")
        void completeLock_RedisExceptionDoesNotThrow() {
            // Arrange
            String key = "complete-exception-key";
            doThrow(new RuntimeException("Redis error"))
                    .when(redisTemplate).execute(any(), any(), (Object[]) any());

            // Act & Assert
            assertDoesNotThrow(() -> redisLockUtil.completeLock(key));
        }
    }

    // ==================== 并发测试 ====================

    @Nested
    @DisplayName("并发场景测试")
    class ConcurrencyTests {

        @Test
        @DisplayName("多线程竞争锁，只有一个能成功执行")
        void concurrentLockOnlyOneSucceeds() throws InterruptedException {
            // Arrange
            AtomicInteger executeCount = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);

            // 只有第一次 tryLock 返回 1，后续都返回 0
            stubExecuteWithAnswer(invocation -> {
                int count = executeCount.incrementAndGet();
                return count == 1 ? 1L : 0L;
            });

            int threadCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicReference<String> winner = new AtomicReference<>();

            // Act
            for (int i = 0; i < threadCount; i++) {
                final String threadName = "thread-" + i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        redisLockUtil.executeWithLock(
                                "concurrent-key", 30, 1, 10,
                                () -> {
                                    winner.set(threadName);
                                    return "done";
                                });
                        successCount.incrementAndGet();
                    } catch (RuntimeException e) {
                        // 获取锁失败
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            endLatch.await(5, TimeUnit.SECONDS);
            executor.shutdown();

            // Assert
            assertEquals(1, successCount.get(), "应该只有一个线程成功获取锁");
            assertNotNull(winner.get());
        }
    }

    // ==================== 锁 key 前缀测试 ====================

    @Nested
    @DisplayName("锁 Key 前缀验证")
    class LockKeyPrefixTests {

        @Test
        @DisplayName("tryLock 使用 lock: 前缀")
        void tryLock_UsesLockPrefix() {
            // Arrange
            String key = "my-resource";
            stubExecuteSuccess();

            // Act
            redisLockUtil.tryLock(key);

            // Assert - 验证传入的 key 列表包含 "lock:my-resource"
            verify(redisTemplate).execute(
                    any(),
                    argThat(list -> {
                        @SuppressWarnings("unchecked")
                        java.util.List<?> keys = (java.util.List<?>) list;
                        return keys.size() == 1 && keys.get(0).equals("lock:my-resource");
                    }),
                    (Object[]) any());
        }

        @Test
        @DisplayName("unlock 使用 lock: 前缀")
        void unlock_UsesLockPrefix() {
            // Arrange
            String key = "my-resource";
            stubExecuteSuccess();

            // Act
            redisLockUtil.unlock(key);

            // Assert
            verify(redisTemplate).execute(
                    any(),
                    argThat(list -> {
                        @SuppressWarnings("unchecked")
                        java.util.List<?> keys = (java.util.List<?>) list;
                        return keys.size() == 1 && keys.get(0).equals("lock:my-resource");
                    }),
                    (Object[]) any());
        }
    }
}
