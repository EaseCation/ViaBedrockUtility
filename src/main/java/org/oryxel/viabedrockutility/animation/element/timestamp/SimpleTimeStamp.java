package org.oryxel.viabedrockutility.animation.element.timestamp;

import java.util.Arrays;

public record SimpleTimeStamp(float timestamp, String[] value) {
    @Override
    public String toString() {
        return "SimpleTimeStamp{" +
                "timestamp=" + timestamp +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}