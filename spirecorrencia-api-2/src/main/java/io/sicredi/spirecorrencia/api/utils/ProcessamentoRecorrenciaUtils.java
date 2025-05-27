package io.sicredi.spirecorrencia.api.utils;

import feign.Response;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProcessamentoRecorrenciaUtils {
    public static String getResponseBodyAsString(final Response.Body body) throws IOException {
        if (body == null) {
            return "";
        }
        return IOUtils.toString(body.asReader(StandardCharsets.UTF_8));
    }
}
