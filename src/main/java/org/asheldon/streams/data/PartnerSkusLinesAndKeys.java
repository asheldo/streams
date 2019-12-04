package org.asheldon.streams.data;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Builder
@Getter
public class PartnerSkusLinesAndKeys {

    private String partner;

    @ToString.Exclude
    private List<PartnerSkuLineAndKey> linesAndKeys;
}
