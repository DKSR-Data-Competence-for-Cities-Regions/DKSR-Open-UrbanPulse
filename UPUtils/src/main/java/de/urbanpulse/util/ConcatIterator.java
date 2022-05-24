package de.urbanpulse.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * concatenates multiple iterators
 *
 * @param <T> generic type for ConcatIterator
 *
 * This code is published by DKSR Gmbh under the German Free Software License.
 * Please refer to the document in the link for usage, change and distribution information
 * https://www.hbz-nrw.de/produkte/open-access/lizenzen/dfsl/german-free-software-license
 */
public class ConcatIterator<T> implements Iterator<T> {

    private int index;
    private final Iterator<T>[] innerIterators;

    /**
     *
     * @param innerIterators iterators to concatenate in the given order
     */
    public ConcatIterator(Iterator<T>... innerIterators) {
        this.innerIterators = innerIterators;
    }

    @Override
    public boolean hasNext() {
        if (innerIterators.length == 0) {
            return false;
        }

        advanceIndexToNextNonDepletedOrEnd();
        return innerIterators[index].hasNext();
    }

    private void advanceIndexToNextNonDepletedOrEnd() {
        Iterator<T> iterator = innerIterators[index];
        while (!iterator.hasNext() && (index + 1) < innerIterators.length) {
            index++;
            iterator = innerIterators[index];
        }
    }

    /**
     * Returns the next element in the iteration.
     *
     * @return the next element in the iteration
     * @throws NoSuchElementException if the iteration has no more elements
     */
    @Override
    public T next() {
        if (innerIterators.length == 0) {
            throw new NoSuchElementException();
        }

        advanceIndexToNextNonDepletedOrEnd();
        return innerIterators[index].next();
    }
}
