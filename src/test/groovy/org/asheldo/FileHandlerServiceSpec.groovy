package org.asheldo

import com.fasterxml.jackson.databind.ObjectMapper
import org.asheldon.FileHandlerService
import org.asheldon.Outputs
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class FileHandlerServiceSpec extends Specification {

    /**
     public void run(final File input) throws Exception {
     FileHandlerService service = injector.getInstance(FileHandlerService.class);
     Outputs outputs = service.read(input);
     log.info("Outputs: {}", outputs);
     }
     */

    @Unroll
    def "test handler service"() {
        given:
        String line1 = """{"partner":"p","sku":"s","miscDetails":"xyz"} """
        ObjectMapper mapper = new ObjectMapper()
        ExecutorService executorService = Executors.newSingleThreadExecutor()
        File input = Files.createTempFile("pre","suff").toFile()
        input.deleteOnExit()
        FileWriter writer = new FileWriter(input)
        writer.write(line1)
        writer.close()
        FileHandlerService service = new FileHandlerService(mapper, executorService)

        when:
        Outputs outputs = service.read(input)
        File valid = outputs.getValidated()
        List<String> lines = Paths.get(valid.getAbsolutePath()).readLines("UTF-8")

        then:
        mapper.readTree(line1) == mapper.readTree(lines.get(0))
    }
}
