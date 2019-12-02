package org.asheldon;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PartnerSku {

    @JsonProperty
    private String partner;
    @JsonProperty
    private String sku;
    @JsonProperty
    private String miscDetails;

    public PartnerSku() {
    }

    public PartnerSku(String partner, String sku, String miscDetails) {
        this.partner = partner;
        this.sku = sku;
        this.miscDetails = miscDetails;
    }

    public String getPartner() {
        return partner;
    }

    public String getSku() {
        return sku;
    }

    public String getMiscDetails() { return miscDetails; }

    public void setPartner(String partner) {
        this.partner = partner;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public void setMiscDetails(String miscDetails) { this.miscDetails = miscDetails; }
}
