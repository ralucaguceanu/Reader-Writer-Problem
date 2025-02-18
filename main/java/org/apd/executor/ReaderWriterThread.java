package org.apd.executor;

import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ReaderWriterThread extends Thread {
    private final BlockingQueue<StorageTask> taskQueue;
    private final AtomicInteger runningTasks;
    private final SharedDatabase sharedDatabase;
    private final LockType lockType;
    private final List<EntryResult> results;
    private final AtomicBoolean isShutdown;

    public ReaderWriterThread(BlockingQueue<StorageTask> taskQueue, AtomicInteger runningTasks,
                              SharedDatabase sharedDatabase, LockType lockType,
                              List<EntryResult> results, AtomicBoolean isShutdown) {
        this.taskQueue = taskQueue;
        this.runningTasks = runningTasks;
        this.sharedDatabase = sharedDatabase;
        this.lockType = lockType;
        this.results = results;
        this.isShutdown = isShutdown;
    }

    @Override
    public void run() {
        while (!isShutdown.get() || !taskQueue.isEmpty()) {
            try {
                StorageTask task = taskQueue.take();
                runningTasks.incrementAndGet();
                try {
                    if (task.isWrite()) {
                        new ReaderWriterManager(sharedDatabase, task, lockType, results).write();
                    } else {
                        new ReaderWriterManager(sharedDatabase, task, lockType, results).read();
                    }
                } finally {
                    if (runningTasks.decrementAndGet() == 0) {
                        synchronized (runningTasks) {
                            runningTasks.notifyAll();
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
