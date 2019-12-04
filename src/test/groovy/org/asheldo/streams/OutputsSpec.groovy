package org.asheldo.streams

import com.fasterxml.jackson.databind.ObjectMapper
import org.asheldon.streams.Outputs
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class OutputsSpec extends Specification {

    @Unroll
    def "test outputs files"() {
        given:
        ObjectMapper mapper = new ObjectMapper()
        File valid = Files.createTempFile("pre","suff").toFile()
        File invalid = Files.createTempFile("pre","suff").toFile()
        valid.deleteOnExit()
        invalid.deleteOnExit()
        Outputs outputs = Outputs.builder()
                .validated(valid)
                .invalidated(invalid)
                .mapper(mapper)
                .build()

        when:
        outputs.getValidatedOutput().write(line1)
        List<String> lines = Paths.get(valid.getAbsolutePath()).readLines("UTF-8")

        then:
        lines.contains(line1)

        where:
        line1 | _
        """{"partner":"p","sku":"s","miscDetails":"xyz"} """ | _
    }


}
