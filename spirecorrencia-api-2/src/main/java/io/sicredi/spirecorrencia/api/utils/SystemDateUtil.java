package io.sicredi.spirecorrencia.api.utils;

import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;

@UtilityClass
public class SystemDateUtil {
    public String formatarData(Temporal data) {
        return DateTimeFormatter.ofPattern("dd/MM").format(data);
    }

    public String formatarData(LocalDateTime data) {
        return DateTimeFormatter.ofPattern("dd/MM/yyyy").format(data);
    }
}
