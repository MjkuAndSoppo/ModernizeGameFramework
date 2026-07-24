package com.modernizegameframework.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * UI 事件调度工具
 * 提供 throttle（节流）与 debounce（防抖）功能，用于优化窗口 resize 等高频事件。
 *
 * 实现原理：
 * - throttle：记录上次执行时间，在指定间隔内只执行第一次调用，后续调用被忽略。
 * - debounce：使用单线程调度器延迟执行，连续调用时取消上一次 pending 的任务，
 *   只有在停止调用超过指定间隔后才真正执行最后一次动作。
 */
public final class UIEventScheduler {

    /** 全局单线程调度器，所有 debounce 任务共用，避免每个动作都创建新线程 */
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "modernizegameframework-ui-scheduler");
        thread.setDaemon(true);
        return thread;
    });

    private UIEventScheduler() {
    }

    /**
     * 可取消的防抖动作接口。
     *
     * @param <T> 动作参数类型
     */
    public interface DebouncedAction<T> extends Consumer<T> {
        /**
         * 取消当前 pending 的延迟任务。
         * 调用后即使到达延迟时间也不会执行动作。
         */
        void cancel();
    }

    /**
     * 节流：在指定时间间隔内只执行第一次调用，后续调用被忽略。
     *
     * @param action   要执行的动作
     * @param interval 间隔时间（毫秒）
     * @param <T>      动作参数类型
     * @return 节流后的动作
     */
    public static <T> Consumer<T> throttle(Consumer<T> action, long interval) {
        return new Consumer<T>() {
            private long lastExecutionTime = 0L;

            @Override
            public void accept(T t) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastExecutionTime >= interval) {
                    lastExecutionTime = currentTime;
                    action.accept(t);
                }
            }
        };
    }

    /**
     * 防抖：在指定时间间隔内如果连续调用，只执行最后一次。
     *
     * <p>内部使用单线程调度器管理延迟任务，避免旧实现中每次调用都创建新线程的问题，
     * 适合窗口 resize 等可能高频触发的事件。</p>
     *
     * @param action   要执行的动作
     * @param interval 间隔时间（毫秒）
     * @param <T>      动作参数类型
     * @return 防抖后的动作，支持取消 pending 任务
     */
    public static <T> DebouncedAction<T> debounce(Consumer<T> action, long interval) {
        AtomicReference<ScheduledFuture<?>> pendingFuture = new AtomicReference<>();
        return new DebouncedAction<T>() {
            @Override
            public void accept(T t) {
                ScheduledFuture<?> old = pendingFuture.getAndSet(null);
                if (old != null) {
                    old.cancel(false);
                }
                pendingFuture.set(EXECUTOR.schedule(() -> action.accept(t), interval, TimeUnit.MILLISECONDS));
            }

            @Override
            public void cancel() {
                ScheduledFuture<?> old = pendingFuture.getAndSet(null);
                if (old != null) {
                    old.cancel(false);
                }
            }
        };
    }

    /**
     * 带键值的节流器，可以为不同键独立节流。
     */
    public static class KeyedThrottle<K, T> {
        private final Consumer<T> action;
        private final long interval;
        private final Map<K, Long> lastExecutionTimes = new HashMap<>();

        public KeyedThrottle(Consumer<T> action, long interval) {
            this.action = action;
            this.interval = interval;
        }

        public void accept(K key, T arg) {
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastExecutionTimes.get(key);
            if (lastTime == null || currentTime - lastTime >= interval) {
                lastExecutionTimes.put(key, currentTime);
                action.accept(arg);
            }
        }
    }
}
