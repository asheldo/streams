package org.asheldo.streams.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.asheldo.streams.model.PartnerSku;

import java.util.List;

@Builder(toBuilder = true)
@Getter
public class PartnerSkus {

    private boolean cancelled;

    @NonNull
    private String partner;

    @NonNull
    @ToString.Exclude
    private List<PartnerSku> validSkus;

    @NonNull
    @ToString.Exclude
    private List<PartnerSku> invalidSkus;
}
