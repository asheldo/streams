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
        if (args.length < 2) {
            System.out.println("Usage: StreamApp <channel> /path/to/input.file");
        }
        Injector injector = Guice.createInjector(new StreamsModule());
        String channel = args[0];
        File input = new File(args[1]);
        StreamsApp app = StreamsApp.builder()
                .injector(injector)
                .build();
        try {
            app.run(input, channel);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    private Injector injector;

    public void run(final File input, final String channel) throws Exception {
        RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
        RemoteOutputs remoteOutputs = service.doTerminations(input, channel);
        log.info("LocalOutputs: {}", remoteOutputs);
    }
}
