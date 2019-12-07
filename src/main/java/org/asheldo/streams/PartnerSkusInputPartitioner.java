package org.asheldo.streams;

import org.asheldo.streams.partition.FixedBatchSizeSpliteratorBase;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class PartnerSkusInputPartitioner extends FixedBatchSizeSpliteratorBase<String> {

    static final int BATCH_SIZE = 1000;
    static final int ESTIMATED_SIZE = 1000;

    private final Iterator<String> lines;

    public PartnerSkusInputPartitioner(Stream<String> lines) {
        super(BATCH_SIZE, ESTIMATED_SIZE);
        this.lines = lines.iterator();
    }

    @Override
    public boolean tryAdvance(Consumer<? super String> action) {
        if (!lines.hasNext()) {
            return false;
        }
        action.accept(lines.next());
        return true;
    }
}
