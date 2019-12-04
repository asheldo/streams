package org.asheldo.streams;

import lombok.Builder;
import lombok.Getter;

import java.io.File;

@Builder
@Getter
public class RemoteOutputs {
    private File invalidated;
    private File validated;
}
