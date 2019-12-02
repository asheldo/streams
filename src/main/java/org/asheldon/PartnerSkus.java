package org.asheldon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

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
