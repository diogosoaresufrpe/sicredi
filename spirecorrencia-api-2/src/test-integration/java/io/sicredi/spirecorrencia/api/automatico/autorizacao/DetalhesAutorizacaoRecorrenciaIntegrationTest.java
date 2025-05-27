package io.sicredi.spirecorrencia.api.automatico.autorizacao;


import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/consulta_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class DetalhesAutorizacaoRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_CONSULTA_DETALHES_AUTORIZACAO = "/v1/automaticos/autorizacoes/{oidRecorrenciaAutorizacao}";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";
    private static final String JSON_PATH_MESSAGE = "$.message";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_1 = "2";

    @Test
    void dadoIdAutorizacao_quandoConsultarDetalheAutorizacaoRecorrencia_deveRetornarARecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_RECORRENCIA_OID_1);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.oidRecorrenciaAutorizacao").value(IDENTIFICADOR_RECORRENCIA_OID_1))
                .andDo(print());
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {"nulo", "FORMATO_INVALIDO"})
        void dadoOidRecorrenciaAutorizacaoEmFormatoStringInvalida_quandoConsultarDetalheRecorrencia_deveRetornarBadRequest(String oidRecorrenciaAutorizacao) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(oidRecorrenciaAutorizacao);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_MESSAGE).value(String.format("Erro ao converter atributo oidRecorrenciaAutorizacao com valor '%s'", oidRecorrenciaAutorizacao)));
        }

        @ParameterizedTest
        @ValueSource(strings = {"501", "907", "-1"})
        void dadoOIdRecorrenciaAutorizacaoNaoExiste_quandoConsultarDetalheRecorrencia_deveRetornarNotFound(String oidRecorrenciaAutorizacao) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(oidRecorrenciaAutorizacao);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_MESSAGE).value("Não foi possível localizar sua autorização do Pix Automático. Por favor, revise as informações e tente novamente."));
        }
    }

    private static MockHttpServletRequestBuilder executarGet(String oidRecorrenciaAutorizacao) {
        var url = URL_CONSULTA_DETALHES_AUTORIZACAO.replace("{oidRecorrenciaAutorizacao}", oidRecorrenciaAutorizacao);
        return MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }

}
