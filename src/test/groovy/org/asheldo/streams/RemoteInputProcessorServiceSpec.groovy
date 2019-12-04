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

    File input
    FileWriter writer

    def setup() {
        input = Files.createTempFile("pre", "suff").toFile()
        input.deleteOnExit()
        writer = new FileWriter(input)
    }

    /**
     public void run(final File input) throws Exception {
     RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
     LocalOutputs outputs = service.doTerminations(input);
     log.info("LocalOutputs: {}", outputs);
     }
     */

    @Unroll
    def "test handler service valid sku"() {
        given:
        partnerLines.entrySet().forEach { lines ->
            String partner = lines.key
            (1..(lines.value)).eachWithIndex { it, ix ->
                writer.write(
                        """{"partner":"${partner}","sku":"${partner}${it}","miscDetails":"xyz ${it}"}\n""")
            }
        }
        writer.close()
        ObjectMapper mapper = new ObjectMapper()
        ExecutorService executorService = Executors.newSingleThreadExecutor()
        RemoteInputProcessorService service = new RemoteInputProcessorService(mapper, executorService)

        when:
        RemoteOutputs outputs = service.doTerminations(input)
        File valid = outputs.validated

        then:
        List<String> validLines = Paths.get(valid.absolutePath).readLines("UTF-8")
        List<String> validSkus = validLines.stream().map { entry ->
            mapper.readValue(entry, PartnerSkuKey).sku
        }.collect(Collectors.toList())
        validSkus.get(0) == "p1"
        validSkus.get(2) == "q2"

        where:
        partnerLines   | _
        ["p":1, "q":2] | _
    }

}
