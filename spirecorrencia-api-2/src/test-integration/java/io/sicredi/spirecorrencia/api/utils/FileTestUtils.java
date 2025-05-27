package io.sicredi.spirecorrencia.api.utils;

import org.springframework.core.io.InputStreamSource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class FileTestUtils {

    private FileTestUtils() {
    }

    public static String asString(final InputStreamSource resource) {
        try (var reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}