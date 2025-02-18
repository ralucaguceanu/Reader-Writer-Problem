package org.apd.solution;

import org.apd.executor.StorageTask;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apd.executor.LockManager.getEnter;
import static org.apd.executor.LockManager.getReaders;
import static org.apd.executor.LockManager.getSemReader;
import static org.apd.executor.LockManager.getSemWriter;
import static org.apd.executor.LockManager.getWaitingReaders;
import static org.apd.executor.LockManager.getWaitingWriters;
import static org.apd.executor.LockManager.getWriters;

public class WriterPreferredSolution1 implements ReaderWriterSolution {

    private final SharedDatabase sharedDatabase;
    private final StorageTask task;
    private final List<EntryResult> results;

    public WriterPreferredSolution1(SharedDatabase sharedDatabase, StorageTask task, List<EntryResult> results) {
        this.sharedDatabase = sharedDatabase;
        this.task = task;
        this.results = results;
    }

    public void read(int index) {
        Semaphore enter = getEnter(index);
        Semaphore semReader = getSemReader(index);
        Semaphore semWriter = getSemWriter(index);

        AtomicInteger readers = getReaders(index);
        AtomicInteger writers = getWriters(index);
        AtomicInteger waitingReaders = getWaitingReaders(index);
        AtomicInteger waitingWriters = getWaitingWriters(index);

        try {
            enter.acquire();

            if (writers.get() > 0 || waitingWriters.get() > 0) {
                waitingReaders.incrementAndGet();
                enter.release();
                semReader.acquire();
            }

            readers.incrementAndGet();

            if (waitingReaders.get() > 0) {
                waitingReaders.decrementAndGet();
                semReader.release();
            } else if (waitingReaders.get() == 0) {
                enter.release();
            }

            results.add(sharedDatabase.getData(index));

            enter.acquire();

            if (readers.decrementAndGet() == 0 && waitingWriters.get() > 0) {
                waitingWriters.decrementAndGet();
                semWriter.release();
            } else if (readers.get() > 0 || waitingWriters.get() == 0) {
                enter.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void write(int index) {
        Semaphore enter = getEnter(index);
        Semaphore semReader = getSemReader(index);
        Semaphore semWriter = getSemWriter(index);

        AtomicInteger readers = getReaders(index);
        AtomicInteger writers = getWriters(index);
        AtomicInteger waitingReaders = getWaitingReaders(index);
        AtomicInteger waitingWriters = getWaitingWriters(index);

        try {
            enter.acquire();

            if (readers.get() > 0 || writers.get() > 0) {
                waitingWriters.incrementAndGet();
                enter.release();
                semWriter.acquire();
            }

            writers.incrementAndGet();
            enter.release();

            results.add(sharedDatabase.addData(index, task.data()));

            enter.acquire();
            writers.decrementAndGet();

            if (waitingReaders.get() > 0 && waitingWriters.get() == 0) {
                waitingReaders.decrementAndGet();
                semReader.release();
            } else if (waitingWriters.get() > 0) {
                waitingWriters.decrementAndGet();
                semWriter.release();
            } else if (waitingReaders.get() == 0 && waitingWriters.get() == 0) {
                enter.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
