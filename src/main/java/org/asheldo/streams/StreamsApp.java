package org.asheldo.streams;

import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
@Builder
public class StreamsApp {

    public static void main(String ... args) {
        Injector injector = Guice.createInjector(new StreamsModule());
        File input = new File(args[0]);
        StreamsApp app = StreamsApp.builder()
                .injector(injector)
                .build();
        try {
            app.run(input);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    private Injector injector;

    public void run(final File input) throws Exception {
        RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
        RemoteOutputs remoteOutputs = service.doTerminations(input);
        log.info("LocalOutputs: {}", remoteOutputs);
    }
}
