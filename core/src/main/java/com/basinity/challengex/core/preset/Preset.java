package com.basinity.challengex.core.preset;

import com.basinity.challengex.core.model.Challenge;
import java.util.Objects;

/**
 * A named, shareable challenge. The JSON form of this record is the durable
 * artifact and the contract with the web builder: it must stay fully usable
 * even if the website is ever discontinued.
 */
public record Preset(String name, Challenge challenge) {

    public Preset {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A preset requires a name");
        }
        Objects.requireNonNull(challenge, "challenge");
    }
}
