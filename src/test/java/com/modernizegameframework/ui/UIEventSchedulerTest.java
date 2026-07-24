package com.modernizegameframework.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * UIEventScheduler 单元测试
 * 验证 throttle（节流）与 debounce（防抖）的行为。
 */
class UIEventSchedulerTest {

    @Test
    void throttle_在间隔内只执行第一次调用() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Consumer<String> throttled = UIEventScheduler.throttle(
                s -> counter.incrementAndGet(), 100);

        // 连续快速调用 5 次
        for (int i = 0; i < 5; i++) {
            throttled.accept("call" + i);
        }
        assertEquals(1, counter.get(), "第一次调用应立即执行");

        // 等待间隔过去
        Thread.sleep(120);
        throttled.accept("after-interval");
        assertEquals(2, counter.get(), "间隔后再次调用应执行");
    }

    @Test
    void throttle_间隔内后续调用被忽略() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        Consumer<String> throttled = UIEventScheduler.throttle(
                s -> counter.incrementAndGet(), 200);

        throttled.accept("first");
        Thread.sleep(50);
        throttled.accept("second");
        Thread.sleep(50);
        throttled.accept("third");

        assertEquals(1, counter.get(), "间隔内多次调用只执行一次");
    }

    @Test
    void debounce_连续调用只执行最后一次() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger lastValue = new AtomicInteger(0);
        UIEventScheduler.DebouncedAction<Integer> debounced = UIEventScheduler.debounce(
                value -> {
                    counter.incrementAndGet();
                    lastValue.set(value);
                }, 50);

        // 连续调用多次，每次传入不同值
        for (int i = 1; i <= 5; i++) {
            debounced.accept(i);
            Thread.sleep(10);
        }

        // 此时不应立即执行
        assertEquals(0, counter.get(), "防抖期间动作不应执行");

        // 等待最后一次调用后的延迟结束
        Thread.sleep(100);
        assertEquals(1, counter.get(), "停止调用后应只执行一次");
        assertEquals(5, lastValue.get(), "应执行最后一次传入的值");
    }

    @Test
    void debounce_取消后不再执行() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        UIEventScheduler.DebouncedAction<Integer> debounced = UIEventScheduler.debounce(
                value -> counter.incrementAndGet(), 50);

        debounced.accept(1);
        Thread.sleep(10);
        debounced.cancel();

        Thread.sleep(100);
        assertEquals(0, counter.get(), "取消后动作不应执行");
    }

    @Test
    void debounce_多次独立调用间隔足够时各执行一次() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        UIEventScheduler.DebouncedAction<Integer> debounced = UIEventScheduler.debounce(
                value -> counter.incrementAndGet(), 30);

        debounced.accept(1);
        Thread.sleep(80);
        debounced.accept(2);
        Thread.sleep(80);

        assertEquals(2, counter.get(), "两次间隔足够大的调用应各执行一次");
    }
}
