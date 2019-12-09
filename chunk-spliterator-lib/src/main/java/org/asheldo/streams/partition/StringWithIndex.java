package org.asheldo.streams.partition;

import lombok.Value;

@Value
public class StringWithIndex {
    private final String string;
    private final int index;
}
