package org.asheldo.streams.data;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.asheldo.streams.model.PartnerSkuKey;

@Builder
@Getter
public class PartnerSkuLineAndKey {
    @NonNull
    private String skuLine;
    @NonNull
    private PartnerSkuKey partnerSkuKey;
}
