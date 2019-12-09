package org.asheldo.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.asheldo.streams.chunkproc.api.model.FeedBatchStepConfig;
import org.asheldo.streams.data.PartnerSkusInput;
import org.asheldo.streams.data.PartnerSkusOutput;
import org.asheldo.streams.chunkproc.api.model.PartnerSkuKey;
import org.asheldo.streams.partition.PartnerSkusInputPartitioner;
import org.asheldo.streams.partition.StringWithIndex;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
public class RemoteInputProcessorService {

    private final Executor executor;

    private final ObjectMapper mapper;


    @Inject
    public RemoteInputProcessorService(final ObjectMapper mapper,
                                       final Executor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }


    public RemoteOutputs doTerminations(final Stream<String> chunkLines,
                                        final FeedBatchStepConfig feedBatchStepConfig,
                                        final File localTemp) throws Exception {
        LocalOutputs localOutputs = buildOutputFiles(chunkLines, localTemp);
        return createRemoteOutputs(localTemp, localOutputs);
    }

    private LocalOutputs buildOutputFiles(final Stream<String> chunkLines,
                                          final File localTemp)
            throws Exception {
        // 1: cache chunk<N>.jsonl line-by-line and map it
        // TODO NEXT: create LocalInput with per-partner stuff like simple keys set, and local file
        Map<String,PartnerSkusInput> partnerSkusInputPartitions
                = partitionLocalInput(chunkLines);

        LocalOutputs localOutputs = LocalOutputs.builder()
                .mapper(mapper)
                .invalidated(new File(localTemp, "local-invalidated-subchunk"))
                .validated(new File(localTemp, "local-validated-subchunk"))
                .build();
        // Keep order
        for (PartnerSkusInput local : partnerSkusInputPartitions.values()) {
            // 2: what to do with lines? stream 'em ...
            Stream<String> lines = local.getLines()
                    .values() // from map
                    .stream()
                    .map(StringWithIndex::getString);
            // 3: partition/index lines by partner
            InputPartnersPartitioner partitioner = new InputPartnersPartitioner(executor, mapper);
            BlockingQueue<PartnerSkusOutput> partners = partitioner.process(lines);
            // 4: process sub-chunks
            // TODO:
            partners.stream().forEachOrdered(localOutputs.handler());
        }
        localOutputs.close();
        return localOutputs;
    }

    private RemoteOutputs createRemoteOutputs(File localTemp, LocalOutputs localOutputs) throws IOException {
        // TODO Real remote
        File remoteInvalid = new File(localTemp, "remote-invalidated-subchunk.jsonl");
        File remoteValid = new File(localTemp, "remote-validated-subchunk.jsonl");
        Files.copy(localOutputs.getInvalidated(), remoteInvalid);
        Files.copy(localOutputs.getValidated(), remoteValid);
        return RemoteOutputs.builder()
                .invalidated(remoteInvalid)
                .validated(remoteValid)
                .build();
    }

    // Semi-costly
    // break up input chunk into natural (single-partner), organized (we have keys) subchunks
    // TODO will be collection/stream instead someday
    private Map<String, PartnerSkusInput> partitionLocalInput(Stream<String> chunkLines) throws Exception {
        boolean parallel = true;
        Map<String,PartnerSkusInput> result = new ConcurrentSkipListMap<>();
        Map<String,PartnerSkusInput> parts = StreamSupport
                .stream(new PartnerSkusInputPartitioner(chunkLines), parallel)
                .reduce(result, accumulator(), combiner());
        // closeOrError(parts);
        return parts;
    }

    private BiFunction<Map<String,PartnerSkusInput>, StringWithIndex,
            Map<String,PartnerSkusInput>> accumulator() {
        return (aMap, stringWithIndex) -> {
            try {
                PartnerSkuKey key = mapper.readValue(stringWithIndex.getString(), PartnerSkuKey.class); // Just the facts Ma'am
                aMap.compute(key.getPartner(), (partner, old) -> {
                    if (old == null) {
                        old = PartnerSkusInput.builder()
                                .partner(partner)
                                .build();
                    }
                    int i = stringWithIndex.getIndex();
                    old.getLines().put(i, stringWithIndex);
                    old.getSkusByInputIndex().put(i, key);
                    return old;
                });
                return aMap;
            } catch (Exception e) {
                return null; // broke
            }
        };
    }

    private BinaryOperator<Map<String,PartnerSkusInput>> combiner() {
        return (aMap, bMap) -> {
            if (aMap != bMap) {
                bMap.forEach((bPartner, b) -> aMap.compute(bPartner, (key, a) -> (a == null) ? b : a.merge(b)));
            }
            return aMap;
        };
    }

}
