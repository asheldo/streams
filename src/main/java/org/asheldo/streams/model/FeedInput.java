package org.asheldo.streams.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.asheldo.streams.LastExposureSettingsImpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FeedInput {

    @JsonProperty
    @Getter
    private String s3Prefix;

    @JsonProperty
    @Getter
    private String channel;

    @JsonProperty
    @Getter
    private LastExposureSettingsImpl lastExposureSettings;
}
