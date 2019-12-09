package org.asheldo.streams.chunkproc.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedInputBase {

    @JsonProperty
    @Getter
    private String s3Prefix;

    @JsonProperty
    @Getter
    private String channel;

    @JsonProperty
    @Getter
    private FeedBatchStepConfig feedBatchStepConfig;
}
