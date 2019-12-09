package org.asheldo.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import org.asheldo.streams.service.S3StreamService;
import org.asheldo.streams.service.S3StreamServiceImpl;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StreamsModule implements Module {

    @Override
    public void configure(Binder binder) {

    }

    @Provides
    @Singleton
    public ObjectMapper provideObjectMapper() {
        return new ObjectMapper();
    }

    @Provides
    @Singleton
    public S3StreamService providesS3StreamService() {
        return new S3StreamServiceImpl();
    }

    @Provides
    @Singleton
    public Executor providesExecutor() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService;
    }

    @Provides
    @Singleton
    @Inject
    public RemoteInputProcessorService provideFileHandlerService(ObjectMapper mapper, Executor executor) {
        return new RemoteInputProcessorService(mapper, executor);
    }
}
