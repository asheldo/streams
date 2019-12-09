package org.asheldo.streams;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.asheldo.streams.data.PartnerSkuLineAndKey;
import org.asheldo.streams.data.PartnerSkusOutput;
import org.asheldo.streams.data.PartnerSkusLinesAndKeys;
import org.asheldo.streams.chunkproc.api.model.PartnerSku;
import org.asheldo.streams.chunkproc.api.model.PartnerSkuKey;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class InputPartnersPartitioner implements Spliterator<PartnerSkusOutput> {

    private ObjectMapper mapper;

    private Executor executor;

    private int initialSize;

    private final BlockingQueue<PartnerSkusOutput> completed = new LinkedBlockingQueue<>();

    private int remaining;

    @Inject
    public InputPartnersPartitioner(final Executor executor, final ObjectMapper mapper) {
        this.executor = executor;
        this.mapper = mapper;
    }

    /*
    return CompletableFuture.allOf(cfs)
            .thenApply(ignored -> futures.stream()
                                    .map(CompletableFuture::join)
                                    .collect(Collectors.toList())
            );
     */

    public BlockingQueue<PartnerSkusOutput> process(Stream<String> lines) throws InterruptedException {
        List<CompletableFuture<PartnerSkusOutput>> futures
                = createPartnersFutures(lines);
        this.initialSize = futures.size();
        this.remaining = initialSize;
        CompletableFuture
                .allOf(futures.toArray(new CompletableFuture[0]))
                .join();
        futures.forEach(f -> f.whenComplete((t, __) -> {
            try {
                completed.add(f.get()); // todo future should implement a NULL object, for orElse?
            } catch (Exception e) {
                log.error("Unable to process PartnerSkusOutput");
            }
        }));
        return completed;
    }

    private PartnerSkuLineAndKey readPartnerSkuLineAndKey(String line) {
        PartnerSkuKey key;
        try {
            key = mapper.readValue(line, PartnerSkuKey.class);
        } catch (JsonProcessingException e) {
            log.error("Unhandled, add to unknown: " + line, e);
            key = new PartnerSkuKey("unknown", null);
        }
        return PartnerSkuLineAndKey.builder().partnerSkuKey(key).skuLine(line).build();
    }

    private List<CompletableFuture<PartnerSkusOutput>> createPartnersFutures(Stream<String> lines) {
        Map<String, PartnerSkusLinesAndKeys> map = new HashMap<>();
        List<CompletableFuture<PartnerSkusOutput>> futures = lines
                .map(line -> readPartnerSkuLineAndKey(line))
                .reduce(map,
                        new PartnerLinesAccumulator(),
                        (a, b) -> { a.putAll(b); return a; })
                .values()
                .stream()
                .map(new PartnerSkusConsolidator(mapper))
                .collect(Collectors.toList());
        return futures;
    }

    /**
     * Supplies futures on consolidated, expensive-to-complete PartnerSkusOutput aggregations
     */
    @AllArgsConstructor
    static class PartnerSkusConsolidator implements Function<PartnerSkusLinesAndKeys, CompletableFuture<PartnerSkusOutput>> {

        @NonNull
        private ObjectMapper mapper;

        @Override
        public CompletableFuture<PartnerSkusOutput> apply(PartnerSkusLinesAndKeys linesAndKeys) {
            PartnerSkusOutput partnerSkusOutput = PartnerSkusOutput.builder()
                    .partner(linesAndKeys.getPartner())
                    .validSkus(new LinkedList<>())
                    .invalidSkus(new LinkedList<>())
                    .build();
            return CompletableFuture.supplyAsync(handler(linesAndKeys, partnerSkusOutput));
        }

        private Supplier<PartnerSkusOutput> handler(final PartnerSkusLinesAndKeys linesAndKeys,
                                                    final PartnerSkusOutput partnerSkusOutput) {
            return () -> {
                linesAndKeys.getLinesAndKeys().stream()
                        .forEachOrdered(lineAndKey -> {
                            PartnerSku sku = expensiveProcessing(lineAndKey.getSkuLine());
                            if (Optional.ofNullable(sku.getSku()).isPresent()) {
                                partnerSkusOutput.getValidSkus().add(sku);
                            } else {
                                partnerSkusOutput.getInvalidSkus().add(sku);
                            }
                        });
                return partnerSkusOutput;
            };
        }

        private PartnerSku expensiveProcessing(String line) {
            return readPartnerSku(line);
        }

        private PartnerSku readPartnerSku(String line) {
            try {
                return mapper.readValue(line, PartnerSku.class);
            } catch (JsonProcessingException e) {
                log.error("Unhandled, add to unknown: " + line, e);
                return new PartnerSku("unknown", null, "");
            }
        }

    }

    static class PartnerLinesAccumulator implements BiFunction<Map<String, PartnerSkusLinesAndKeys>,
                PartnerSkuLineAndKey,
                Map<String, PartnerSkusLinesAndKeys>> {
        @Override
        public Map<String, PartnerSkusLinesAndKeys> apply(
                Map<String, PartnerSkusLinesAndKeys> mapPartnerSkusLinesAndKeys,
                PartnerSkuLineAndKey keyAndLine) {
            String key = keyAndLine.getPartnerSkuKey().getPartner();
            PartnerSkusLinesAndKeys newValue = mapPartnerSkusLinesAndKeys.compute(
                    key,
                    (partner, skus) -> addTo(skus, keyAndLine));
            return mapPartnerSkusLinesAndKeys;
        }

        private PartnerSkusLinesAndKeys addTo(PartnerSkusLinesAndKeys list, PartnerSkuLineAndKey partnerSkuKey) {
            if (list == null) {
                list = PartnerSkusLinesAndKeys.builder()
                        .partner(partnerSkuKey.getPartnerSkuKey().getPartner())
                        .linesAndKeys(new LinkedList<>())
                        .build();
            }
            list.getLinesAndKeys().add(partnerSkuKey);
            return list;
        }
    }

    // Spliterator:

    @Override
    public boolean tryAdvance(Consumer<? super PartnerSkusOutput> action) {
        return remaining > 0
                ? !nextCompleted().isCancelled()
                // If we wanted series of dependent futures:
                //
                // .thenAccept(action).thenApply(__ -> true).join()
                : false;
    }

    private PartnerSkusOutput nextCompleted() {
        remaining--;
        PartnerSkusOutput next = completed.poll();
        while (next == null) {
            try {
                Thread.sleep(0,1);
            } catch (InterruptedException e) {
                log.error("Error", e);
            }
            next = completed.poll();
        }
        return next;

    }
    @Override
    public Spliterator<PartnerSkusOutput> trySplit() {
        return null;
    }

    @Override
    public long estimateSize() {
        return 0;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
