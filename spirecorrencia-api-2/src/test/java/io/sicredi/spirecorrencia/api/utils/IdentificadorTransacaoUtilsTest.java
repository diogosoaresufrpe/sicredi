package io.sicredi.spirecorrencia.api.utils;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static io.sicredi.spirecorrencia.api.testconfig.TestFactory.IdentificadorTransacaoTestFactory.gerarIdFimAFim;
import static org.junit.jupiter.api.Assertions.assertEquals;

class IdentificadorTransacaoUtilsTest {

    @Test
    void dadoIdentificadorTransacaoComDataUTC_quandoExtrairData_deveExtrairDataComSucesso() {
        var data = LocalDateTime.of(2023, 10, 10, 9, 30);
        String identificadorTransacao = gerarIdFimAFim(data);

        LocalDateTime dataExtraida = IdentificadorTransacaoUtils.extrairData(identificadorTransacao);

        assertEquals(data.minusHours(3), dataExtraida);
    }

}