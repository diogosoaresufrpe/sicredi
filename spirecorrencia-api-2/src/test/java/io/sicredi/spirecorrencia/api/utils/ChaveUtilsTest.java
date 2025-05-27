package io.sicredi.spirecorrencia.api.utils;

import br.com.sicredi.framework.exception.BusinessException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ChaveUtilsTest {

    @ParameterizedTest
    @MethodSource("getCombinacoesChaves")
    void dadoChaveValida_quandoDeterminarTipoChave_deveRetornarTipoCorreto(String chave, TipoChaveEnum tipoChave) {
        assertEquals(tipoChave, ChaveUtils.determinarTipoChave(chave));
    }

    private static Stream<Arguments> getCombinacoesChaves() {
        return Stream.of(
                Arguments.of("65005141065", TipoChaveEnum.CPF),
                Arguments.of("83386096000103", TipoChaveEnum.CNPJ),
                Arguments.of("+5510998765432", TipoChaveEnum.TELEFONE),
                Arguments.of("40e34bef-62eb-4219-af31-7aa461698388", TipoChaveEnum.EVP),
                Arguments.of("pix@bcb.gov.br", TipoChaveEnum.EMAIL)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"1234567891", "123456789101", "123456789101234", "umaoutrachavequalquer"})
    void dadoChaveInvalida_quandoDeterminarTipoChave_deveLancarException(String chave) {
        var exception = assertThrows(BusinessException.class, () -> ChaveUtils.determinarTipoChave(chave));

        assertEquals("A chave informada não possui um formato válido", exception.getMessage());
    }

}