package org.jetbrains.research.groups.ml_methods.code_completion_benchmark.core.counter.storage;

import java.util.List;

class ArrayStorage {
    static boolean checkExactSequence(List<Integer> indices, int index, int[] successor) {
        boolean valid = successor.length == indices.size() - index;
        if (valid) {
            for (int i = 1; i < successor.length; i++) {
                if (indices.get(index + i) != successor[i]) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }

    static boolean checkPartialSequence(List<Integer> indices, int index, int[] successor) {
        boolean valid = successor.length >= indices.size() - index;
        if (valid) {
            for (int i = 1; i < indices.size() - index; i++) {
                if (indices.get(index + i) != successor[i]) {
                    valid = false;
                    break;
                }
            }
        }
        return valid;
    }
}