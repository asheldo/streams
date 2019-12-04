package org.asheldo.streams

import com.fasterxml.jackson.databind.ObjectMapper
import org.asheldon.streams.Outputs
import org.asheldon.streams.model.PartnerSkuKey
import org.asheldon.streams.RemoteInputProcessorService
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
        input = Files.createTempFile("pre","suff").toFile()
        input.deleteOnExit()
        writer = new FileWriter(input)
    }

    /**
     public void run(final File input) throws Exception {
     RemoteInputProcessorService service = injector.getInstance(RemoteInputProcessorService.class);
     Outputs outputs = service.read(input);
     log.info("Outputs: {}", outputs);
     }
     */

    @Unroll
    def "test handler service valid sku"() {
        given:
        partnerLines.entrySet().forEach { lines ->
            String partner = lines.key
            (1..(lines.value)).eachWithIndex { it, ix ->
                writer.write(
                        """{"partner":"${partner}","sku":"${partner}${it}","miscDetails":"xyz ${it}"}
""")
            }
        }
        writer.close()
        ObjectMapper mapper = new ObjectMapper()
        ExecutorService executorService = Executors.newSingleThreadExecutor()
        RemoteInputProcessorService service = new RemoteInputProcessorService(mapper, executorService)

        when:
        Outputs outputs = service.read(input)
        outputs.close()
        File valid = outputs.getValidated()

        then:
        List<String> validLines = Paths.get(valid.getAbsolutePath()).readLines("UTF-8")
        List<String> validSkus = validLines.stream().map { entry ->
            return mapper.readValue(entry, PartnerSkuKey).sku
        }.collect(Collectors.toList())
        validSkus.get(0) == "p1"
        validSkus.get(2) == "q2"

        where:
        partnerLines  | _
        ["p":1,"q":2] | _
    }
}
