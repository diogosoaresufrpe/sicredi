package io.sicredi.spirecorrencia.api.utils;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IdentificadorTransacaoUtils {

    public static final DateTimeFormatter IDENTIFICADOR_TRANSACAO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Integer POSICAO_INICIAL_DATA_IDENTIFICADOR_TRANSACAO = 9;
    private static final Integer POSICAO_FINAL_DATA_IDENTIFICADOR_TRANSACAO = 21;
    private static final String TIME_ZONE_AMERICA_SAO_PAULO = "America/Sao_Paulo";

    public static LocalDateTime extrairData(String identificadorTransacao) {
        var dataString = identificadorTransacao.substring(POSICAO_INICIAL_DATA_IDENTIFICADOR_TRANSACAO, POSICAO_FINAL_DATA_IDENTIFICADOR_TRANSACAO);
        var dataIdentificadorTransacaoUTC = LocalDateTime.parse(dataString, IDENTIFICADOR_TRANSACAO_FORMATTER);
        var zonedGMT = dataIdentificadorTransacaoUTC.atZone(ZoneOffset.UTC);
        var zonedBrasilia = zonedGMT.withZoneSameInstant(ZoneId.of(TIME_ZONE_AMERICA_SAO_PAULO));
        return zonedBrasilia.toLocalDateTime();
    }
}
