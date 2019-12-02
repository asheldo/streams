package org.asheldon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;

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
    public Executor providesExecutor() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService;
    }

    @Provides
    @Singleton
    @Inject
    public FileHandlerService provideFileHandlerService(ObjectMapper mapper, Executor executor) {
        return new FileHandlerService(mapper, executor);
    }
}
