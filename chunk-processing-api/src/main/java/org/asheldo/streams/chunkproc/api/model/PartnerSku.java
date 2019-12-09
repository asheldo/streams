package org.asheldo.streams.chunkproc.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class PartnerSku extends PartnerSkuKey {

    @JsonProperty
    @Getter
    private String miscDetails;

    public PartnerSku(String partner, String sku, String miscDetails) {
        super(partner, sku);
        this.miscDetails = miscDetails;
    }

}
