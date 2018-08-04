package bnw.abm.intg.util.math;

import java.util.ArrayList;
import java.util.List;

import bnw.abm.intg.util.Log;

public class Combinations {

    /**
     * Computes different sized combinations that can be formed by selecting elements, with repetition
     * 
     * @param elements
     *            list of elements
     * @param repeat
     *            how many times each element can repeat
     * @param maxSelectionSize
     *            maximum size of the the combination
     * @return list of combinations
     */
    public static <E> List<Combination<E>> compute(List<E> elements, int repeat, int maxSelectionSize) {
        List<Combination<E>> allCombinations = new ArrayList<>();
        allCombinations.add(new Combination<E>());
        List<Integer> offset = new ArrayList<>(elements.size()), prevOffset = new ArrayList<>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            offset.add(0);
            prevOffset.add(0);
        }
        int start = 0, a = 0, listSize = 0;
        int selecSize = 0;
        for (selecSize = 0; selecSize < repeat & selecSize < maxSelectionSize; selecSize++) {
            Log.debug("Selection size: " + selecSize);
            start = listSize;
            listSize = allCombinations.size();
            for (int i = 0; i < elements.size(); i++) {
                a = 0;
                for (int combI = start; combI < listSize; combI++, a++) {
                    Combination<E> newComb = new Combination<E>(allCombinations.get(combI));
                    newComb.add(elements.get(i));
                    allCombinations.add(newComb);
                }
                start += offset.get(i);
                prevOffset.set(i, offset.get(i));
                offset.set(i, a);

            }
            Log.debug("Combinations: " + (allCombinations.size() - listSize));
        }

        for (; selecSize < maxSelectionSize; selecSize++) {
            Log.debug("Selection size: " + selecSize);
            start = listSize;
            int newStart = start;
            listSize = allCombinations.size();
            for (int i = 0; i < elements.size(); i++) {
                newStart = start + prevOffset.get(i);
                a = 0;
                for (int combI = newStart; combI < listSize; combI++, a++) {
                    Combination<E> newComb = new Combination<E>(allCombinations.get(combI));
                    newComb.add(elements.get(i));
                    allCombinations.add(newComb);
                }
                start += offset.get(i);
                if (selecSize == repeat) {
                    prevOffset.set(i, offset.get(i) - 1);
                } else {
                    prevOffset.set(i, offset.get(i));
                }
                offset.set(i, a);
            }
            Log.debug("Combinations: " + (allCombinations.size() - listSize));
        }

        Log.debug("Total combinations: " + allCombinations.size());
        allCombinations.remove(0);
        return allCombinations;
    }

    /**
     * Computes combinations of elements from multiple lists by selecting 1 element from each list
     * 
     * @param lists
     *            List of lists of elements
     * @return List of combinations
     */
    public static <T> List<List<T>> fromMultipleLists(List<List<T>> lists) {
        List<List<T>> combinations = new ArrayList<List<T>>();
        List<List<T>> newCombinations;

        int index = 0;

        // extract each of the integers in the first list
        // and add each to ints as a new list
        for (T i : lists.get(0)) {
            List<T> newList = new ArrayList<T>();
            newList.add(i);
            combinations.add(newList);
        }
        index++;
        while (index < lists.size()) {
            List<T> nextList = lists.get(index);
            newCombinations = new ArrayList<List<T>>();
            for (List<T> first : combinations) {
                for (T second : nextList) {
                    List<T> newList = new ArrayList<T>();
                    newList.addAll(first);
                    newList.add(second);
                    newCombinations.add(newList);
                }
            }
            combinations = newCombinations;

            index++;
        }

        return combinations;
    }

    // public static <E> List<Class<? extends ArrayList<?>>> compute(List<E> elements, int repeat, int maxSelectionSize,
    // Class<? extends ArrayList<?>> outElementClass) {
    // List<Class<? extends ArrayList<?>>> allCombinations = new ArrayList<>();
    //
    // // Creating empty Combination instance
    // Constructor<?> cons = outElementClass.getConstructor();
    // Class<? extends ArrayList<?>> object = (Class<? extends ArrayList<?>>) cons.newInstance();
    //
    // allCombinations.add(object);// Adding combination instance to all combinations
    // List<Integer> offset = new ArrayList<>(elements.size());
    // for (int i = 0; i < elements.size(); i++) {
    // offset.add(0);
    // }
    // int x = 0, y = 0, a = 0, i = 0, s = 0, lastEnd = 0;
    // for (int selecSize = 0; selecSize < maxSelectionSize; selecSize++) {
    //
    // i = 0;
    // x = lastEnd;
    // lastEnd = allCombinations.size();
    // do {
    // a = 0;
    // for (int combI = x; combI < lastEnd; combI++) {
    //
    // // Creating empty Combination instance
    // Constructor<?> newConstructor = outElementClass.getConstructor();
    // Class<? extends ArrayList<?>> newComb = (Class<? extends ArrayList<?>>) newConstructor.newInstance(allCombinations.get(combI));
    //
    //
    // newComb.add(elements.get(i));
    // allCombinations.add(newComb);
    // a++;
    // }
    // x = offset.get(i) + x;
    // offset.set(i, a);
    //
    // } while (elements.size() > ++i);
    // // s++;
    //
    // }
    // allCombinations.remove(0);
    // return allCombinations;
    // }

    public static class Combination<E> extends ArrayList<E> {

        /**
         * 
         */
        private static final long serialVersionUID = -3403812442946385002L;

        protected Combination(List<E> combination) {
            for (E e : combination) {
                this.add(e);
            }
        }

        public Combination() {
            super(10);
        }
    }

}
