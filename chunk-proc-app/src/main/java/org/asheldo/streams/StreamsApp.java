package org.asheldo.streams;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;
import com.google.inject.Guice;
import com.google.inject.Injector;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.asheldo.streams.chunkproc.api.model.FeedInputBase;
import org.asheldo.streams.service.S3StreamService;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.*;
import java.util.stream.Stream;

@Slf4j
@Builder
public class StreamsApp {

    public static void main(String ... args) {
        if (args.length < 2) {
            System.out.println("Usage: StreamApp <channel> /path/to/input.file");
        }
        Injector injector = Guice.createInjector(new StreamsModule());
        String bucketName = args[0];
        String feedInputLocation = args[1];
        String awsU = args[2];
        String awsP = args[3];
        S3StreamService s3StreamService = injector.getInstance(S3StreamService.class);
        ObjectMapper objectMapper = injector.getInstance(ObjectMapper.class);
        StreamsApp app = StreamsApp.builder()
                .injector(injector)
                .s3StreamService(s3StreamService)
                .objectMapper(objectMapper)
                .build();
        try {
            app.run(bucketName, feedInputLocation, awsU, awsP);
        } catch (Exception e) {
            log.error("Error", e);
        }
    }

    private S3StreamService s3StreamService;
    private ObjectMapper objectMapper;
    private Injector injector;

    // TODO V2
    public void run(final String bucketName,
                    final String feedInputLocation,
                    final String awsU, final String awsP) throws Exception {
        //

        S3Client s3Client = s3StreamService.getS3Client(AwsBasicCredentials.create(awsU, awsP));
        GetObjectRequest s3Request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(feedInputLocation)
                .build();
        ResponseInputStream<GetObjectResponse> s3objectResponse = s3Client.getObject(s3Request);

        // BufferedReader reader = new BufferedReader(new InputStreamReader(s3objectResponse));
        FeedInputBase feedInputBase = readFeedInput(s3objectResponse, objectMapper);
        LastExposureSettingsImpl lastExposureSettings = feedInputBase.getFeedBatchStepConfig();
        String channel = feedInputBase.getChannel();

        File localTemp = new File(Files.createTempDir(), File.separator + channel + File.separator);
        localTemp.mkdirs();
        String remoteS3Name = new File(feedInputLocation).getName();
        File local = new File(localTemp, "local-" + remoteS3Name);


        Stream<String> lines = new BufferedReader(new FileReader(local)).lines();
        RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);


        RemoteOutputs remoteOutputs = service.doTerminations(lines, lastExposureSettings, localTemp);
        log.info("LocalOutputs: {}", remoteOutputs);
    }

    private FeedInputBase readFeedInput(final InputStream s3objectResponse,
                                        final ObjectMapper objectMapper) throws IOException {
        File inputTemp = File.createTempFile("feedInputBase", "");
        FileUtils.copyInputStreamToFile(s3objectResponse, inputTemp);
        //        FeedInputBase feedInputBase = objectMapper.readValue(s3objectResponse, FeedInputBase.class);
        FeedInputBase feedInputBase = objectMapper.readValue(inputTemp, FeedInputBase.class);
        return feedInputBase;
    }

    // V2
    /*
    public void run(final File input, final String channel) throws Exception {
        File localTemp = new File(Files.createTempDir(), File.separator + channel + File.separator);
        localTemp.mkdirs();
        File local = new File(localTemp, "local-" + input.getName());


        Files.copy(remoteS3Input, local);
        Stream<String> lines = new BufferedReader(new FileReader(local)).lines();

        RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
        RemoteOutputs remoteOutputs = service.doTerminations(input, settings, localTemp);
        log.info("LocalOutputs: {}", remoteOutputs);
    }
     */
}
