package org.apd.executor;

import org.apd.solution.ReaderPreferredSolution;
import org.apd.solution.ReaderWriterSolution;
import org.apd.solution.WriterPreferredSolution1;
import org.apd.solution.WriterPreferredSolution2;
import org.apd.storage.EntryResult;
import org.apd.storage.SharedDatabase;

import java.util.List;

public class ReaderWriterManager {

    private final SharedDatabase sharedDatabase;
    private final StorageTask task;
    private final List<EntryResult> results;
    private final ReaderWriterSolution solution;

    public ReaderWriterManager(SharedDatabase sharedDatabase, StorageTask task, LockType lockType, List<EntryResult> results) {
        this.sharedDatabase = sharedDatabase;
        this.task = task;
        this.results = results;
        this.solution = getSolutionByLockType(lockType);
    }

    public void read() {
        solution.read(task.index());
    }

    public void write() {
        solution.write(task.index());
    }

    private ReaderWriterSolution getSolutionByLockType(LockType lockType) {
        return switch (lockType) {
            case ReaderPreferred -> new ReaderPreferredSolution(sharedDatabase, task, results);
            case WriterPreferred1 -> new WriterPreferredSolution1(sharedDatabase, task, results);
            case WriterPreferred2 -> new WriterPreferredSolution2(sharedDatabase, task, results);
        };
    }

}
