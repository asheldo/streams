package org.asheldon.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.asheldon.streams.data.PartnerSkus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RemoteInputProcessorService {

    private final Executor executor;

    private final ObjectMapper mapper;

    @Inject
    public RemoteInputProcessorService(final ObjectMapper mapper, final Executor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }

    // Imperative 1-4; versus ?
    public Outputs read(final File remoteS3Input) throws Exception {
        // 1: cache chunk<N>.jsonl
        File local = localize(remoteS3Input);
        Outputs outputs = Outputs.builder()
                .mapper(mapper)
                .invalidated(getInvalidated(local))
                .validated(getValidated(local))
                .build();
        // 2: what to do with lines? stream 'em ...
        Stream<String> lines = new BufferedReader(new FileReader(local))
                .lines(); // .collect(Collectors.toList());
        // 3: partition/index lines by partner
        InputPartnersPartitioner partitioner = new InputPartnersPartitioner(executor, mapper);
        BlockingQueue<PartnerSkus> partners = partitioner.process(lines);
        // 4: process sub-chunks
        partners.stream().forEachOrdered(outputs.handler());
        return outputs;
    }

    private File localize(File remoteS3Input) {
        // TODO
        return new File(remoteS3Input.getAbsolutePath());
    }

    private File getInvalidated(File input) {
        return new File(input.getParentFile(), "invalidated.json");
    }

    private File getValidated(File input) {
        return new File(input.getParentFile(), "validated.json");
    }
}
