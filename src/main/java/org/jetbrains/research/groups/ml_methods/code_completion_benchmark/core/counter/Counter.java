package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter;

import java.io.Externalizable;
import java.util.List;

public interface Counter extends Externalizable {

    int getCount();

    long[] getCounts(List<Integer> indices);

    int getCountOfCount(int n, int count);

    int getSuccessorCount();

    int getSuccessorCount(List<Integer> indices);

    List<Integer> getTopSuccessors(List<Integer> indices, int limit);

    int[] getDistinctCounts(int range, List<Integer> indices);

    void count(List<Integer> indices);

    void unCount(List<Integer> indices);

    default void countBatch(List<List<Integer>> indices) {
        indices.forEach(this::count);
    }

    default void unCountBatch(List<List<Integer>> indices) {
        indices.forEach(this::unCount);
    }
}
