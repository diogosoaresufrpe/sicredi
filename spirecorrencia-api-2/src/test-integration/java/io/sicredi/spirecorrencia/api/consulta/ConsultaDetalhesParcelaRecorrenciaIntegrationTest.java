package io.sicredi.spirecorrencia.api.consulta;

import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Pagador.Validations.*;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class ConsultaDetalhesParcelaRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_DETALHES_PARCELAS = "/v1/recorrencias/parcelas/{identificadorParcela}";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";
    private static final String IDENTIFICADOR_PARCELA = "2DDF2A2BCF9FE8B2E063C50A17ACB3A6";
    private static final String QUERY_PARAM_AGENCIA_PAGADOR = "agenciaPagador";
    private static final String QUERY_PARAM_CONTA_PAGADOR = "contaPagador";
    private static final String COOPERATIVA = "0101";
    private static final String CONTA = "000023";

    @Test
    void dadoTodosFiltrosPreenchidos_quandoConsultarDetalheRecorrencia_deveRetornarDetalhesDaParcela () throws Exception {

        var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_PARCELA)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.parcela.identificadorParcela").value(IDENTIFICADOR_PARCELA));
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", "\n", "\r", "\t\n\r"})
        void dadoFiltroIdentificadorParcelaInvalido_quandoConsultarParcelas_deveRetornarBadRequest(String identificadorParcela) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(identificadorParcela)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Preenchimento do identificador único da parcela é obrigatório"));
        }

        @ParameterizedTest
        @MethodSource("errosContaPagadorInvalida")
        void dadoFiltroContaPagadorInvalido_quandoConsultarParcelas_deveRetornarBadRequest(String contaPagador, String mensagemEsperada) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_PARCELA)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, contaPagador);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value(mensagemEsperada));
        }

        private static Stream<Arguments> errosContaPagadorInvalida() {
            return Stream.of(
                    Arguments.of("111111111111111111111", CONTA_PATTERN_MESSAGE),
                    Arguments.of("A123456", CONTA_PATTERN_MESSAGE),
                    Arguments.of("BBBBBB", CONTA_PATTERN_MESSAGE),
                    Arguments.of(null, CONTA_NOTBLANK)
            );
        }

        @ParameterizedTest
        @MethodSource("errosAgenciaPagadorInvalida")
        void dadoFiltroAgenciaPagadorInvalido_quandoConsultarParcelas_deveRetornarBadRequest(String agenciaPagador, String mensagemEsperada) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_PARCELA)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, agenciaPagador)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value(mensagemEsperada));
        }

        private static Stream<Arguments> errosAgenciaPagadorInvalida() {
            return Stream.of(
                    Arguments.of("111", AGENCIA_PATTERN_MESSAGE),
                    Arguments.of("11111", AGENCIA_PATTERN_MESSAGE),
                    Arguments.of("A123", AGENCIA_PATTERN_MESSAGE),
                    Arguments.of("BBBB", AGENCIA_PATTERN_MESSAGE),
                    Arguments.of(null, AGENCIA_PATTERN)
            );
        }

    }

    private static MockHttpServletRequestBuilder executarGet(String identificadorParcela) {
        var url = URL_DETALHES_PARCELAS.replace("{identificadorParcela}", identificadorParcela);
        return MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }

}
