package org.asheldo.streams

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Paths

class LocalOutputsSpec extends Specification {

    @Unroll
    def "test outputs files"() {
        given:
        ObjectMapper mapper = new ObjectMapper()
        File valid = Files.createTempFile("pre", "suff").toFile()
        File invalid = Files.createTempFile("pre", "suff").toFile()
        valid.deleteOnExit()
        invalid.deleteOnExit()
        LocalOutputs outputs = LocalOutputs.builder()
                .validated(valid)
                .invalidated(invalid)
                .mapper(mapper)
                .build()

        when:
        outputs.validatedOutput.write(line1)
        outputs.close()
        List<String> lines = Paths.get(valid.absolutePath).readLines("UTF-8")

        then:
        lines.contains(line1)

        where:
        line1 | _
        """{"partner":"p","sku":"s","miscDetails":"xyz"}""" | _
    }

}
