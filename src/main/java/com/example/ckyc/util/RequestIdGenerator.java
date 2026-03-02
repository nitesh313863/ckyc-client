package com.example.ckyc.util;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RequestIdGenerator {

    private final AtomicInteger counter = new AtomicInteger(0);
    private final ReentrantLock lock = new ReentrantLock();
    private LocalDate activeDate = LocalDate.now();

    public String nextRequestId() {
        LocalDate today = LocalDate.now();
        if (!today.equals(activeDate)) {
            lock.lock();
            try {
                if (!today.equals(activeDate)) {
                    activeDate = today;
                    counter.set(0);
                }
            } finally {
                lock.unlock();
            }
        }
        int value = counter.updateAndGet(v -> (v >= 99_999_999) ? 1 : v + 1);
        return String.format("%08d", value);
    }
}
