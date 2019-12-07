package org.asheldo.streams.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.asheldo.streams.model.PartnerSkuKey;
import org.asheldo.streams.partition.StringWithIndex;

import java.util.*;

@Builder(toBuilder = true)
@Getter
public class PartnerSkusInput {

    @NonNull
    private String partner;

    private final Map<Integer, StringWithIndex> lines = new TreeMap<>();

    @ToString.Exclude
    private final Map<Integer,PartnerSkuKey> skusByInputIndex = new TreeMap<>();

    public PartnerSkusInput merge(PartnerSkusInput b) {
        if (b != this) {
            skusByInputIndex.putAll(b.getSkusByInputIndex());
            lines.putAll(b.getLines());
        }
        return this;
    }

}
