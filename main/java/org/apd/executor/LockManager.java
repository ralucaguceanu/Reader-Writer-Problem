package org.apd.executor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LockManager {

    private static final Map<Integer, ReentrantLock> mutexByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Semaphore> semaphoresByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, AtomicInteger> readersByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, AtomicInteger> writersByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, AtomicInteger> waitingReadersByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, AtomicInteger> waitingWritersByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Semaphore> semWriterByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Semaphore> semReaderByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Semaphore> enterByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Lock> locksByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Condition> okToReadByIndex = Collections.synchronizedMap(new HashMap<>());
    private static final Map<Integer, Condition> okToWriteByIndex = Collections.synchronizedMap(new HashMap<>());


    public static ReentrantLock getMutex(int index) {
        return mutexByIndex.computeIfAbsent(index, k -> new ReentrantLock());
    }

    public static Semaphore getSemaphore(int index) {
        return semaphoresByIndex.computeIfAbsent(index, k -> new Semaphore(1));
    }

    public static AtomicInteger getReaders(int index) {
        return readersByIndex.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    public static AtomicInteger getWriters(int index) {
        return writersByIndex.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    public static AtomicInteger getWaitingReaders(int index) {
        return waitingReadersByIndex.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    public static AtomicInteger getWaitingWriters(int index) {
        return waitingWritersByIndex.computeIfAbsent(index, k -> new AtomicInteger(0));
    }

    public static Semaphore getSemWriter(int index) {
        return semWriterByIndex.computeIfAbsent(index, k -> new Semaphore(0));
    }

    public static Semaphore getSemReader(int index) {
        return semReaderByIndex.computeIfAbsent(index, k -> new Semaphore(0));
    }

    public static Semaphore getEnter(int index) {
        return enterByIndex.computeIfAbsent(index, k -> new Semaphore(1));
    }

    public static Lock getLock(int index) {
        return locksByIndex.computeIfAbsent(index, k -> new ReentrantLock());
    }

    public static Condition getOkToRead(int index) {
        return okToReadByIndex.computeIfAbsent(index, k -> getLock(index).newCondition());
    }

    public static Condition getOkToWrite(int index) {
        return okToWriteByIndex.computeIfAbsent(index, k -> getLock(index).newCondition());
    }

}
