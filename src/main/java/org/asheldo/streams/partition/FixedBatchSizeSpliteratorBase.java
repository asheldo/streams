package org.asheldo.streams.partition;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

import static java.util.Spliterators.spliterator;

public abstract class FixedBatchSizeSpliteratorBase<T> implements Spliterator<T> {
    private final int batchSize;
    private final int characteristics;
    private long estimatedSize;

    public FixedBatchSizeSpliteratorBase(int characteristics, int batchSize, int estimatedSize) {
        this.characteristics = characteristics;
        this.batchSize = batchSize;
        this.estimatedSize = estimatedSize;
    }

    public FixedBatchSizeSpliteratorBase(int batchSize, int estimatedSize) {
        this(IMMUTABLE | ORDERED | NONNULL, batchSize, estimatedSize);
    }

    @Override
    public Spliterator<T> trySplit() {
        final HoldingConsumer<T> holder = new HoldingConsumer<>();
        if (!tryAdvance(holder)) {
            return null;
        }
        final Object [] array = new Object[batchSize];
        int j = 0;
        do {
            array[j] = holder.value;
        } while (++j < batchSize && tryAdvance(holder));
        if (estimatedSize != Long.MAX_VALUE) {
            estimatedSize -= j;
        }
        return spliterator(array, 0, j, characteristics() | SIZED);
    }

    @Override
    public Comparator<? super T> getComparator() {
        if (hasCharacteristics(SORTED)) {
            return null;
        }
        throw new IllegalStateException("Only for already sorted sources");
    }

    @Override
    public long estimateSize() {
        return estimatedSize;
    }

    @Override
    public int characteristics() {
        return characteristics;
    }

    static final class HoldingConsumer<T> implements Consumer<T> {
        Object value;

        @Override
        public void accept(T value) {
            this.value = value;
        }
    }
}
