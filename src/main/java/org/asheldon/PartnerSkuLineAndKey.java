package org.asheldon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder
@Getter
public class PartnerSkuLineAndKey {
    @NonNull
    private String skuLine;
    @NonNull
    private PartnerSkuKey partnerSkuKey;
}
