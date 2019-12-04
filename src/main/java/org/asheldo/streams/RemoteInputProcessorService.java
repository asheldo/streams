package org.asheldo.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.inject.Inject;
import org.asheldo.streams.data.PartnerSkus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
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
    public RemoteOutputs doTerminations(final File remoteS3Input) throws Exception {
        // 1: cache chunk<N>.jsonl
        LocalOutputs localOutputs = buildOutputFiles(remoteS3Input);
        return createRemoteOutputs(remoteS3Input, localOutputs);
    }

    private LocalOutputs buildOutputFiles(File remoteS3Input) throws InterruptedException, IOException {
        File local = createLocalInput(remoteS3Input);
        LocalOutputs localOutputs = LocalOutputs.builder()
                .mapper(mapper)
                .invalidated(addFile(local, "local-invalidated-subchunk"))
                .validated(addFile(local, "local-validated-subchunk"))
                .build();
        // 2: what to do with lines? stream 'em ...
        Stream<String> lines = new BufferedReader(new FileReader(local))
                .lines(); // .collect(Collectors.toList());
        // 3: partition/index lines by partner
        InputPartnersPartitioner partitioner = new InputPartnersPartitioner(executor, mapper);
        BlockingQueue<PartnerSkus> partners = partitioner.process(lines);
        // 4: process sub-chunks
        partners.stream().forEachOrdered(localOutputs.handler());
        localOutputs.close();
        return localOutputs;
    }

    private RemoteOutputs createRemoteOutputs(File remoteS3Input, LocalOutputs localOutputs) throws IOException {
        File remoteInvalid = addFile(remoteS3Input, "remote-invalidated-subchunk.jsonl");
        File remoteValid = addFile(remoteS3Input, "remote-validated-subchunk.jsonl");
        Files.copy(localOutputs.getInvalidated(), remoteInvalid);
        Files.copy(localOutputs.getValidated(), remoteValid);
        return RemoteOutputs.builder().invalidated(remoteInvalid).validated(remoteValid).build();
    }

    private File createLocalInput(File remoteS3Input) throws IOException {
        // TODO
        File local = File.createTempFile("local-", "-chunk.jsonl");
        Files.copy(remoteS3Input, local);
        return local;
    }

    private File addFile(File sibling, String name) {
        return new File(sibling.getParentFile(), name + ".jsonl");
    }
}
