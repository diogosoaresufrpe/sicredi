package io.sicredi.spirecorrencia.api.notificacao;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMarcaEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificacaoUtilsTest {

    public static final String SICREDI_APP = "SICREDI_APP";

    @ParameterizedTest(name = "{index} - canal: {0}, sistema: {1} => canal esperado: {2}")
    @MethodSource("canalESistemaProvider")
    @DisplayName("Dado um canal e um sistema, quando converter para notificação, deve retornar o canal esperado")
    void dadoCanalESistema_quandoConverterCanalParaNotificacao_deveRetornarCanalEsperado(TipoCanalEnum canal, OrigemEnum sistema, String canalEsperado) {
        var canalParaNotificacao = NotificacaoUtils.converterCanalParaNotificacao(canal, sistema);

        assertEquals(canalEsperado, canalParaNotificacao);
    }

    @ParameterizedTest(name = "{index} - canal: {0}, marca: {1} => canal esperado: {2}")
    @MethodSource("canalEMarcaProvider")
    @DisplayName("Dado um canal e uma marca, quando converter para notificação, deve retornar o canal esperado")
    void dadoCanalEMarca_quandoConverterCanalParaNotificacao_deveRetornarCanalEsperado(TipoCanalEnum canal, TipoMarcaEnum marca, String canalEsperado) {
        var canalParaNotificacao = NotificacaoUtils.converterCanalParaNotificacao(canal, marca);

        assertEquals(canalEsperado, canalParaNotificacao);
    }

    private static Stream<Arguments> canalESistemaProvider() {
        return Stream.of(
                Arguments.of(TipoCanalEnum.SICREDI_INTERNET, OrigemEnum.LEGADO, "INTERNET_BANKING"),
                Arguments.of(TipoCanalEnum.MOBI, OrigemEnum.LEGADO, TipoCanalEnum.MOBI.name()),
                Arguments.of(TipoCanalEnum.SICREDI_X, OrigemEnum.FISITAL, SICREDI_APP),
                Arguments.of(TipoCanalEnum.WOOP, OrigemEnum.FISITAL, TipoCanalEnum.WOOP.name()),
                Arguments.of(TipoCanalEnum.WEB_OPENBK, OrigemEnum.LEGADO, TipoCanalEnum.MOBI.name()),
                Arguments.of(TipoCanalEnum.WEB_OPENBK, OrigemEnum.FISITAL, SICREDI_APP)
        );
    }

    private static Stream<Arguments> canalEMarcaProvider() {
        return Stream.of(
                Arguments.of(TipoCanalEnum.SICREDI_INTERNET, TipoMarcaEnum.SICREDI, "INTERNET_BANKING"),
                Arguments.of(TipoCanalEnum.MOBI, TipoMarcaEnum.SICREDI, TipoCanalEnum.MOBI.name()),
                Arguments.of(TipoCanalEnum.SICREDI_X, TipoMarcaEnum.SICREDI_X, SICREDI_APP),
                Arguments.of(TipoCanalEnum.WOOP, TipoMarcaEnum.WOOP, TipoCanalEnum.WOOP.name()),
                Arguments.of(TipoCanalEnum.WEB_OPENBK, TipoMarcaEnum.SICREDI, TipoCanalEnum.MOBI.name()),
                Arguments.of(TipoCanalEnum.WEB_OPENBK, TipoMarcaEnum.SICREDI_X, SICREDI_APP)
        );
    }
}