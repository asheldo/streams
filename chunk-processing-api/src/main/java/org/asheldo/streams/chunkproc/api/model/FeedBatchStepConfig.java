package org.asheldo.streams.chunkproc.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
public class FeedBatchStepConfig {

    @JsonProperty
    @Getter
    @Setter
    private String channel;

}
