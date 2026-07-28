package com.basinity.challengex.common.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Writes the website's support file. Run by {@code :common:exportSupport}. */
public final class SupportExportMain {

    private SupportExportMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: SupportExportMain <output file>");
        }
        Path out = Path.of(args[0]);
        Files.createDirectories(out.getParent());
        Files.writeString(out, SupportJson.write());
    }
}
