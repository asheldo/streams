package org.asheldo.streams.partition;

import org.asheldo.streams.partition.FixedBatchSizeSpliteratorBase;
import org.asheldo.streams.partition.StringWithIndex;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PartnerSkusInputPartitioner extends FixedBatchSizeSpliteratorBase<StringWithIndex> {

    static final int BATCH_SIZE = 1000;
    static final int ESTIMATED_SIZE = 1000;

    private final AtomicInteger index = new AtomicInteger(0);
    private final Iterator<String> lines;

    public PartnerSkusInputPartitioner(Stream<String> lines) {
        super(BATCH_SIZE, ESTIMATED_SIZE);
        this.lines = lines.iterator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super StringWithIndex> action) {
        if (!lines.hasNext()) {
            return false;
        }
        action.accept(new StringWithIndex(lines.next(), index.getAndIncrement()));
        return true;
    }
}
