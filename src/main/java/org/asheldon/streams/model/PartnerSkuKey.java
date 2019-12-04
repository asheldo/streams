package org.asheldon.streams.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerSkuKey {

    @JsonProperty
    private String partner;
    @JsonProperty
    private String sku;

    public PartnerSkuKey() {
    }

    public PartnerSkuKey(String partner, String sku) {
        this.partner = partner;
        this.sku = sku;
    }

    public String getPartner() {
        return partner;
    }

    public String getSku() {
        return sku;
    }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }
}

