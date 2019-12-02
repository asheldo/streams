package org.asheldon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileHandlerService {

    private final Executor executor;

    private final ObjectMapper mapper;

    @Inject
    public FileHandlerService(final ObjectMapper mapper, final Executor executor) {
        this.mapper = mapper;
        this.executor = executor;
    }

    /**
     * please close after use
     */
    public Outputs read(final File input) throws Exception {
        Outputs outputs = Outputs.builder()
                .mapper(mapper)
                .invalidated(getInvalidated(input))
                .validated(getValidated(input))
                .build();

        List<String> lines = new BufferedReader(new FileReader(input))
                .lines().collect(Collectors.toList());
        PartnerPartitioner partitioner = new PartnerPartitioner(executor, mapper);
        BlockingQueue<PartnerSkus> partners = partitioner.process(lines);
        partners.stream().forEachOrdered(outputs.handler());
        return outputs;
    }

    private File getInvalidated(File input) {
        return new File(input.getParentFile(), "invalidated.json");
    }

    private File getValidated(File input) {
        return new File(input.getParentFile(), "validated.json");
    }
}
