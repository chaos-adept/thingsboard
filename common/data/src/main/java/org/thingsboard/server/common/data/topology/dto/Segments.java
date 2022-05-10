package org.thingsboard.server.common.data.topology.dto;

import lombok.Getter;

public enum Segments {
    TERRITORY("Territory"),
    BUILDING("Building"),
    ROOM("Room");

    @Getter
    private final String key;

    Segments(String key) {
        this.key = key;
    }
}
