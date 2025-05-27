package io.sicredi.spirecorrencia.api.automatico.solicitacao;


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
                @Sql(scripts = {"/db/clear.sql", "/db/solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class DetalhesSolicitacaoAutorizacaoRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_RECORRENCIAS = "/v1/automaticos/autorizacoes/solicitacoes/{idSolicitacaoAutorizacaoRecorrencia}";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";
    private static final String JSON_PATH_MESSAGE = "$.message";
    private static final String IDENTIFICADOR_SOLICITACAO_RECORRENCIA_1 = "SC0118152120250425041bYqAj6ef";

    @Test
    void dadoIdAutorizacao_quandoConsultarDetalheSolicitacaoAutorizacaoRecorrencia_deveRetornarARecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_SOLICITACAO_RECORRENCIA_1);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.idSolicitacaoRecorrencia").value(IDENTIFICADOR_SOLICITACAO_RECORRENCIA_1))
                .andDo(print());
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", "\n", "\r", "\t\n\r"})
        void dadoIdSolicitacaoAutorizacaoRecorrenciaInvalido_quandoConsultarDetalheSolicitacaoAutorizacaoRecorrencia_deveRetornarBadRequest(String idSolicitacaoRecorrencia) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(idSolicitacaoRecorrencia);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Preenchimento do identificador da solicitação de autorização é obrigatório"))
                    .andDo(print());
        }

        @ParameterizedTest
        @ValueSource(strings = {"501", "907", "-1"})
        void dadoIdSolicitacaoAutorizacaoRecorrenciaNaoExiste_quandoConsultarDetalheSolicitacaoAutorizacaoRecorrencia_deveRetornarNotFound(String idSolicitacaoRecorrencia) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(idSolicitacaoRecorrencia);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isNotFound())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_MESSAGE).value("O detalhamento de solicitação de autorização do Pix Automático não foi encontrado."))
                    .andDo(print());
        }
    }

    private static MockHttpServletRequestBuilder executarGet(String idSolicitacaoRecorrencia) {
        var url = URL_RECORRENCIAS.replace("{idSolicitacaoAutorizacaoRecorrencia}", idSolicitacaoRecorrencia);
        return MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }

}
