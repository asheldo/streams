package org.asheldo.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.asheldo.streams.data.PartnerSkusInput;
import org.asheldo.streams.data.PartnerSkusOutput;
import org.asheldo.streams.model.PartnerSkuKey;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
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
    public RemoteInputProcessorService(final ObjectMapper mapper, final Executor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }

    // Imperative 1-4; versus ?
    public RemoteOutputs doTerminations(final File remoteS3Input, final String channel) throws Exception {
        File localTemp = new File(Files.createTempDir(), File.separator + channel + File.separator);
        localTemp.mkdirs();
        LocalOutputs localOutputs = buildOutputFiles(remoteS3Input, localTemp);
        return createRemoteOutputs(remoteS3Input, localOutputs);
    }

    private LocalOutputs buildOutputFiles(final File remoteS3Input, final File localTemp)
            throws Exception {
        // 1: cache chunk<N>.jsonl line-by-line and map it
        // TODO NEXT: create LocalInput with per-partner stuff like simple keys set, and local file
        Map<String,PartnerSkusInput> partnerSkusInputPartitions = partitionLocalInput(remoteS3Input, localTemp);


        LocalOutputs localOutputs = LocalOutputs.builder()
                .mapper(mapper)
                .invalidated(new File(localTemp, "local-invalidated-subchunk"))
                .validated(new File(localTemp, "local-validated-subchunk"))
                .build();


        // Keep order
        for (PartnerSkusInput local : partnerSkusInputPartitions.values()) {

            // 2: what to do with lines? stream 'em ...
            // TODO: Change this to consume input stream of per-partner local files
            Stream<String> lines = local.getLines().stream(); // new BufferedReader(new FileReader(local.getLocalFile())).lines();
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

    private RemoteOutputs createRemoteOutputs(File remoteS3Input, LocalOutputs localOutputs) throws IOException {
        // TODO Real remote
        File remoteInvalid = new File(remoteS3Input.getParentFile(), "remote-invalidated-subchunk.jsonl");
        File remoteValid = new File(remoteS3Input.getParentFile(), "remote-validated-subchunk.jsonl");
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
    private Map<String, PartnerSkusInput> partitionLocalInput(File remoteS3Input, File localTemp) throws Exception {
        File local = new File(localTemp, "local-chunkN.jsonl");

        // TODO Abstract this operation

        // TODO Stream each line to local

        Files.copy(remoteS3Input, local);
        Stream<String> lines = new BufferedReader(new FileReader(local)).lines();

        boolean parallel = true;
        Map<String,PartnerSkusInput> result = new ConcurrentSkipListMap<>();
        Map<String,PartnerSkusInput> parts = StreamSupport
                .stream(new PartnerSkusInputPartitioner(lines), parallel)
                .reduce(result, accumulator(localTemp), combiner());
        // closeOrError(parts);
        return parts;
    }

    private BiFunction<Map<String,PartnerSkusInput>, String,
            Map<String,PartnerSkusInput>> accumulator(File localTemp) {
        return (aMap, line) -> {
            try {
                PartnerSkuKey key = mapper.readValue(line, PartnerSkuKey.class); // Just the facts Ma'am
                aMap.compute(key.getPartner(), (partner, old) -> {
                            if (old == null) {
                                old = PartnerSkusInput.builder()
                                        .partner(partner)
                                        .skusByInputIndex(new LinkedList<>())
                                        .build();
                            }
                            old.getLines().add(line + System.lineSeparator());
                            old.getSkusByInputIndex().add(key);
                            return old;
                        }
                );
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
