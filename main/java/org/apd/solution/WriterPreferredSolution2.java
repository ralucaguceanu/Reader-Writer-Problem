package org.apd.solution;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import static org.apd.executor.LockManager.getLock;
import static org.apd.executor.LockManager.getOkToRead;
import static org.apd.executor.LockManager.getOkToWrite;
import static org.apd.executor.LockManager.getReaders;
import static org.apd.executor.LockManager.getWaitingReaders;
import static org.apd.executor.LockManager.getWaitingWriters;
import static org.apd.executor.LockManager.getWriters;

public class WriterPreferredSolution2 implements ReaderWriterSolution {

    private final SharedDatabase sharedDatabase;
    private final StorageTask task;
    private final List<EntryResult> results;

    public WriterPreferredSolution2(SharedDatabase sharedDatabase, StorageTask task, List<EntryResult> results) {
        this.sharedDatabase = sharedDatabase;
        this.task = task;
        this.results = results;
    }

    public void read(int index) {
        Lock lock = getLock(index);
        Condition readCondition = getOkToRead(index);
        Condition writeCondition = getOkToWrite(index);
        AtomicInteger activeReaders = getReaders(index);
        AtomicInteger activeWriters = getWriters(index);
        AtomicInteger waitingWriters = getWaitingWriters(index);
        AtomicInteger waitingReaders = getWaitingReaders(index);

        lock.lock();
        try {
            while (activeWriters.get() + waitingWriters.get() > 0) {
                waitingReaders.incrementAndGet();
                readCondition.await();
                waitingReaders.decrementAndGet();
            }

            activeReaders.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        results.add(sharedDatabase.getData(index));

        lock.lock();
        try {
            activeReaders.decrementAndGet();
            if (activeReaders.get() == 0 && waitingWriters.get() > 0) {
                writeCondition.signal();
            }
        } finally {
            lock.unlock();
        }
    }

    public void write(int index) {
        Lock lock = getLock(index);
        Condition readCondition = getOkToRead(index);
        Condition writeCondition = getOkToWrite(index);
        AtomicInteger activeReaders = getReaders(index);
        AtomicInteger activeWriters = getWriters(index);
        AtomicInteger waitingWriters = getWaitingWriters(index);

        lock.lock();
        try {
            waitingWriters.incrementAndGet();
            while (activeWriters.get() + activeReaders.get() > 0) {
                writeCondition.await();
            }
            waitingWriters.decrementAndGet();
            activeWriters.incrementAndGet();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        results.add(sharedDatabase.addData(index, task.data()));

        lock.lock();
        try {
            activeWriters.decrementAndGet();
            if (waitingWriters.get() > 0) {
                writeCondition.signal();
            } else {
                readCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }
}
