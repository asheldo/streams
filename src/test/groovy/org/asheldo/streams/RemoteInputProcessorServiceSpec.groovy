package org.asheldo.streams

import com.fasterxml.jackson.databind.ObjectMapper
import org.asheldo.streams.model.PartnerSkuKey
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.stream.Collectors

class RemoteInputProcessorServiceSpec extends Specification {

    def setup() {
    }

    /**
     public void run(final File input) throws Exception {
     RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
     LocalOutputs outputs = service.doTerminations(input);
     log.info("LocalOutputs: {}", outputs);
     }
     */

    // def "test service with s3 object stream and valid skus"() {
/*
GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
S3Object s3Object = amazonS3Client.getObject(getObjectRequest);
S3ObjectInputStream objectInputStream = s3Object.getObjectContent();
 */

    @Unroll
    def "test service with local file and valid skus"() {
        given:
        List<String> chunkLines = new LinkedList<>()
        String sku = """{"partner":"%s","sku":"%s%d","miscDetails":"xyz %d"}\n""".toString()
        partnerLines.entrySet().forEach { partnerLine ->
            String partner = partnerLine.key
            (1..(partnerLine.value)).eachWithIndex { it, ix ->
                chunkLines.add(String.format(sku, partner, partner, it, it))
            }
        }
        ObjectMapper mapper = new ObjectMapper()
        ExecutorService executorService = Executors.newSingleThreadExecutor()
        RemoteInputProcessorService service = new RemoteInputProcessorService(mapper, executorService)
        File localTemp = Files.createTempDirectory("stuff-ing").toFile()
        LastExposureSettingsImpl settings = new LastExposureSettingsImpl("stuff-ing")

        when:
        RemoteOutputs outputs = service.doTerminations(chunkLines.stream(), settings, localTemp); // input, "stuff-ing")
        File valid = outputs.validated
        List<String> validLines = Paths.get(valid.absolutePath).readLines("UTF-8")
        List<String> validSkus = validLines.stream()
                .map { entry -> mapper.readValue(entry, PartnerSkuKey).sku }
                .collect(Collectors.toList())

        then:
        validSkus.contains("p1")
        validSkus.contains("q" + partnerLines.get("q"))
        validSkus.first() == "p1"
        validSkus.last() == "q" + partnerLines.get("q")

        where:
        partnerLines        | _
        ["p":1, "q":2]      | _
        ["p":5000, "q":5000] | _
    }

}
