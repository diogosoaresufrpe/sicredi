package io.sicredi.spirecorrencia.api.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.Temporal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

@ExtendWith(MockitoExtension.class)
class SystemDateUtilTest {

    private static Stream<Arguments> dataProvider() {
        return Stream.of(
                of(LocalDate.of(2018, 12, 30), "30/12"),
                of(LocalDateTime.of(2018, 2, 22, 15, 36), "22/02")
        );
    }


    @MethodSource("dataProvider")
    @DisplayName("Dado data recebida quando formatar data deve retornar data formatada")
    @ParameterizedTest(name = "Dado data recebida {0} quando formatar data deve retornar data formatada {1}")
    void dadoDataRecebida_quandoFormatarData_deveRetornarDataFormatada(Temporal dataRecebida, String dataFormatadaEsperada) {
        String dataFormatadaRecebida = SystemDateUtil.formatarData(dataRecebida);

        assertEquals(dataFormatadaEsperada, dataFormatadaRecebida);
    }


}