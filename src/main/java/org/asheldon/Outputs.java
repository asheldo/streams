package org.asheldon;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.MissingResourceException;
import java.util.function.Consumer;

@Slf4j
@Builder
public class Outputs {

    @NonNull
    private ObjectMapper mapper;
    @NonNull
    @Getter
    private File validated;
    @NonNull
    @Getter
    private File invalidated;

    @Getter(lazy = true)
    private final Writer validatedOutput = writer(validated);

    @Getter(lazy = true)
    private final Writer invalidatedOutput = writer(invalidated);

    private Writer writer(final File f) {
        try {
            Writer w = new BufferedWriter(new FileWriter(f));
            return w;
        } catch (IOException e) {
            throw new MissingResourceException("Unable to create", Writer.class.getName(), f.getPath());
        }
    }

    public Consumer<PartnerSkus> handler() throws IOException {
        return (partner) -> {
            partner.getValidSkus().stream()
                    .map(validSku -> writeValid(validSku))
                    .filter(wrote -> !wrote)
                    .forEach(wrote -> log.warn("Failed to write valid"));
            partner.getInvalidSkus().stream()
                    .map(invalidSku -> writeInvalid(invalidSku))
                    .filter(wrote -> !wrote)
                    .forEach(wrote -> log.warn("Failed to write invalid"));
        };
    }

    private boolean writeValid(PartnerSku validSku) {
        try {
            mapper.writeValue(getValidatedOutput(), validSku);
            return true;
        } catch (IOException e) {
            log.error("Will try to write as invalid, problem writing valid sku: " + validSku, e);
            writeInvalid(validSku);
        }
        return false;
    }

    private boolean writeInvalid(PartnerSku invalidSku) {
        try {
            mapper.writeValue(getInvalidatedOutput(), invalidSku);
            return true;
        } catch (IOException e) {
            log.error("Problem writing invalid sku: " + invalidSku, e);
        }
        return false;
    }
}
