package org.apd.solution;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import static org.apd.executor.LockManager.getMutex;
import static org.apd.executor.LockManager.getReaders;
import static org.apd.executor.LockManager.getSemaphore;

public class ReaderPreferredSolution implements ReaderWriterSolution {

    private final SharedDatabase sharedDatabase;
    private final StorageTask task;
    private final List<EntryResult> results;

    public ReaderPreferredSolution(SharedDatabase sharedDatabase, StorageTask task, List<EntryResult> results) {
        this.sharedDatabase = sharedDatabase;
        this.task = task;
        this.results = results;
    }

    public void read(int index) {
        ReentrantLock mutexNumberOfReaders = getMutex(index);
        Semaphore readWrite = getSemaphore(index);
        AtomicInteger readers = getReaders(index);

        mutexNumberOfReaders.lock();
        try {
            if (readers.incrementAndGet() == 1) {
                readWrite.acquire();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            mutexNumberOfReaders.unlock();
        }

        results.add(sharedDatabase.getData(index));

        mutexNumberOfReaders.lock();
        if (readers.decrementAndGet() == 0) {
            readWrite.release();
        }
        mutexNumberOfReaders.unlock();
    }

    public void write(int index) {
        Semaphore semaphore = getSemaphore(index);

        try {
            semaphore.acquire();
            results.add(sharedDatabase.addData(index, task.data()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            semaphore.release();
        }
    }
}
