package org.asheldo.streams.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.asheldo.streams.model.PartnerSku;
import org.asheldo.streams.model.PartnerSkuKey;

import java.io.*;
import java.util.*;

@Builder(toBuilder = true)
@Getter
public class PartnerSkusInput {

    @NonNull
    private String partner;

    private final List<String> lines = new LinkedList<>();

    @NonNull
    @ToString.Exclude
    private List<PartnerSkuKey> skusByInputIndex;

    public PartnerSkusInput merge(PartnerSkusInput b) {
        if (b != this) {
            skusByInputIndex.addAll(b.getSkusByInputIndex());
            lines.addAll(b.getLines());
        }
        return this;
    }

}
