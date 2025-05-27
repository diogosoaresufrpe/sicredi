package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.testconfig.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
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
public class ConsultaAutorizacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_RECORRENCIAS = "/v1/automaticos/autorizacoes";
    private static final String QUERY_PARAM_NUMERO_PAGINA = "numeroPagina";
    private static final String QUERY_PARAM_TAMANHO_PAGINA = "tamanhoPagina";
    private static final String QUERY_PARAM_AGENCIA_PAGADOR = "agenciaPagador";
    private static final String QUERY_PARAM_CONTA_PAGADOR = "contaPagador";
    private static final String QUERY_PARAM_CPF_CNPJ_PAGADOR = "cpfCnpjPagador";
    private static final String QUERY_PARAM_TIPO_PESSOA_PAGADOR = "tipoPessoaPagador";
    private static final String QUERY_PARAM_STATUS = "status";
    private static final String QUERY_PARAM_DATA_INICIAL = "dataInicial";
    private static final String QUERY_PARAM_DATA_FINAL = "dataFinal";
    private static final String NUMERO_PAGINA_ZERO = "0";
    private static final String TAMANHO_PAGINA_DEZ = "10";
    private static final String COOPERATIVA = "0101";
    private static final String CONTA = "223190";
    private static final String JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS = "$.paginacao.numeroElementos";
    private static final String JSON_PATH_PAGINACAO_TOTAL_PAGINAS = "$.paginacao.totalPaginas";
    private static final String JSON_PATH_PAGINACAO_NUMERO_PAGINA = "$.paginacao.numeroPagina";
    private static final String JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS = "$.autorizacoes[%s].tpoStatus";
    private static final String JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA = "$.autorizacoes[%s].oidRecorrenciaAutorizacao";
    private static final String JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS = "$.autorizacoes[%s].tpoSubStatus";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";

    @Test
    void dadoFiltroAgenciaContaExistentes_quandoConsultarAutorizacoes_deveRetornarPaginaAutorizacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(5))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusAutorizacao.CRIADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 0)).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS, 0)).value("AGUARDANDO_RETORNO"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 1)).value(TipoStatusAutorizacao.APROVADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 1)).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS, 1)).value("AGUARDANDO_ENVIO"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 2)).value(TipoStatusAutorizacao.REJEITADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 2)).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS, 2)).value("AGUARDANDO_CANCELAMENTO"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 3)).value(TipoStatusAutorizacao.CANCELADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 3)).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 4)).value(TipoStatusAutorizacao.EXPIRADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 4)).isNotEmpty())
                .andDo(print());
    }

    @Test
    void dadoTodosFiltrosPreenchidos_quandoConsultarAutorizacoes_deveRetornarPaginaComAsSolicitacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_CPF_CNPJ_PAGADOR, "40111443040")
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_TIPO_PESSOA_PAGADOR, "PF")
                .queryParam(QUERY_PARAM_STATUS, TipoStatusAutorizacao.APROVADA.toString())
                .queryParam(QUERY_PARAM_DATA_INICIAL, "2025-07-18T12:00:00")
                .queryParam(QUERY_PARAM_DATA_FINAL, "2025-07-18T14:00:00")
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusAutorizacao.APROVADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 0)).isNotEmpty())
                .andDo(print());
    }

    @Test
    void dadoStatusRejeitadaOuExpirada_quandoConsultarAutorizacoes_deveRetornarPaginaComAsSolicitacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_STATUS, TipoStatusAutorizacao.REJEITADA.toString())
                .queryParam(QUERY_PARAM_STATUS, TipoStatusAutorizacao.EXPIRADA.toString())
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(2))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusAutorizacao.REJEITADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 0)).isNotEmpty())
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 1)).value(TipoStatusAutorizacao.EXPIRADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_AUTORIZACAO_RECORRENCIA, 1)).isNotEmpty())
                .andDo(print());
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "1234567890", "123456789012345"})
        void dadoFiltroCpfCnpjPagadorInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String cpfCnpjPagador) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_CPF_CNPJ_PAGADOR, cpfCnpjPagador);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Número do CPF ou CNPJ do pagador inválido."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"111", "11111", "A123", "BBBB"})
        void dadoFiltroAgenciaPagadorInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String agenciaPagador) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, agenciaPagador)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Número da cooperativa do associado inválido."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"012345678901234567890123", "A123456", "BBBBBB"})
        void dadoFiltroContaPagadorInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String contaPagador) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, contaPagador);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Número da conta do associado inválido."));
        }


        @ParameterizedTest
        @ValueSource(strings = {"PENDENTE", "REMOVIDO"})
        void dadoFiltroStatusInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String status) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_STATUS, status);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Lista de tipo de status de autorização inválida"));
        }


        @Test
        void dadoFiltroNumeroPaginaNulo_quandoConsultarAutorizacoes_deveRetornarBadRequest() throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Número da página atual não pode ser nulo."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"-10", "-1"})
        void dadoFiltroNumeroPaginaInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String numeroPagina) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, numeroPagina)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Número da página atual deve ser maior ou igual a 0."));
        }

        @Test
        void dadoFiltroTamanhoPaginaNulo_quandoConsultarAutorizacoes_deveRetornarBadRequest() throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Tamanho da pagina não pode ser nulo."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "31", "50"})
        void dadoFiltroTamanhoPaginaInvalido_quandoConsultarAutorizacoes_deveRetornarBadRequest(String tamanhoPagina) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, tamanhoPagina)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Tamanho da pagina deve possuir minimo 1 e máximo 30"));
        }
    }

    private static MockHttpServletRequestBuilder executarGet() {
        return MockMvcRequestBuilders.get(URL_RECORRENCIAS)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }
}