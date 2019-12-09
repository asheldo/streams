package org.asheldo.streams.chunkproc.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@ToString
@NoArgsConstructor
public class PartnerSkuKey {

    @JsonProperty
    @Getter
    private String partner;

    @JsonProperty
    @Getter
    private String sku;

    public PartnerSkuKey(String partner, String sku) {
        this.partner = partner;
        this.sku = sku;
    }
}

