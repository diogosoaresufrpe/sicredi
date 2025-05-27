package io.sicredi.spirecorrencia.api.consulta;


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
                @Sql(scripts = {"/db/clear.sql", "/db/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class ConsultaListagemRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_RECORRENCIAS = "/v1/recorrencias";
    private static final String QUERY_PARAM_NUMERO_PAGINA = "numeroPagina";
    private static final String QUERY_PARAM_TAMANHO_PAGINA = "tamanhoPagina";
    private static final String QUERY_PARAM_AGENCIA_PAGADOR = "agenciaPagador";
    private static final String QUERY_PARAM_CONTA_PAGADOR = "contaPagador";
    private static final String JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA = "$.recorrencias[%s].identificadorRecorrencia";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";
    private static final String COOPERATIVA = "0101";
    private static final String CONTA = "000023";
    private static final String NUMERO_PAGINA_ZERO = "0";
    private static final String TAMANHO_PAGINA_DEZ = "10";
    private static final String QUERY_PARAM_CPF_CNPJ_PAGADOR = "cpfCnpjPagador";
    private static final String QUERY_PARAM_TIPO_PESSOA_PAGADOR = "tipoPessoaPagador";
    private static final String QUERY_PARAM_STATUS = "status";
    private static final String QUERY_PARAM_TIPO_RECORRENCIA = "tipoRecorrencia";
    private static final String QUERY_PARAM_DATA_INICIAL = "dataInicial";
    private static final String QUERY_PARAM_DATA_FINAL = "dataFinal";
    private static final String JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS = "$.paginacao.numeroElementos";
    private static final String JSON_PATH_PAGINACAO_TOTAL_PAGINAS = "$.paginacao.totalPaginas";
    private static final String JSON_PATH_PAGINACAO_NUMERO_PAGINA = "$.paginacao.numeroPagina";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_1 = "2f335153-4d6a-4af1-92a0-c52c5c827af9";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_3 = "2f335153-4d6a-4af1-92a0-c52c5c827af2";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_2 = "2f335153-4d6a-4af1-92a0-c52c5c827af1";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_4 = "5y335153-4d6a-4af1-92a0-c52c5c827af9";

    @Test
    void dadoFiltroAgenciaContaExistentes_quandoConsultarRecorrencias_deveRetornarPaginaComAsRecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(4))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 0)).value(IDENTIFICADOR_RECORRENCIA_OID_3))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 1)).value(IDENTIFICADOR_RECORRENCIA_OID_1))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 2)).value(IDENTIFICADOR_RECORRENCIA_OID_4))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 3)).value(IDENTIFICADOR_RECORRENCIA_OID_2))
                .andDo(print());
    }

    @Test
    void dadoTodosFiltrosPreenchidos_quandoConsultarRecorrencias_deveRetornarPaginaComAsRecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_CPF_CNPJ_PAGADOR, "24110287000170")
                .queryParam(QUERY_PARAM_TIPO_PESSOA_PAGADOR, "PJ")
                .queryParam(QUERY_PARAM_STATUS, "CRIADO")
                .queryParam(QUERY_PARAM_TIPO_RECORRENCIA, "AGENDADO")
                .queryParam(QUERY_PARAM_DATA_INICIAL, "2025-02-03T10:54:32")
                .queryParam(QUERY_PARAM_DATA_FINAL, "2025-02-05T10:55:32");


        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(2))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 0)).value(IDENTIFICADOR_RECORRENCIA_OID_1))
                .andDo(print());
    }

    @Test
    void dadoTodosTipoRecorrenciaAgendadoOuAgendadoRecorrente_quandoConsultarRecorrencias_deveRetornarPaginaComAsRecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_CPF_CNPJ_PAGADOR, "24110287000170")
                .queryParam(QUERY_PARAM_TIPO_PESSOA_PAGADOR, "PJ")
                .queryParam(QUERY_PARAM_STATUS, "CRIADO")
                .queryParam(QUERY_PARAM_STATUS, "CONCLUIDO")
                .queryParam(QUERY_PARAM_TIPO_RECORRENCIA, "AGENDADO")
                .queryParam(QUERY_PARAM_TIPO_RECORRENCIA, "AGENDADO_RECORRENTE")
                .queryParam(QUERY_PARAM_DATA_INICIAL, "2025-02-03T10:54:32")
                .queryParam(QUERY_PARAM_DATA_FINAL, "2025-02-10T10:55:32");


        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(3))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 0)).value(IDENTIFICADOR_RECORRENCIA_OID_1))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 1)).value(IDENTIFICADOR_RECORRENCIA_OID_4))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_RECORRENCIAS_INDEX_IDENTIFICADORRECORRENCIA, 2)).value(IDENTIFICADOR_RECORRENCIA_OID_2))
                .andDo(print());
    }


    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "1234567890", "123456789012345"})
        void dadoFiltroCpfCnpjPagadorInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String cpfCnpjPagador) throws Exception {
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
        void dadoFiltroAgenciaPagadorInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String agenciaPagador) throws Exception {
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
        void dadoFiltroContaPagadorInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String contaPagador) throws Exception {
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
        @ValueSource(strings = {"", " ", "JP", "FP"})
        void dadoFiltroTipoPessoaPagadorInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String tipoPessoaPagador) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_TIPO_PESSOA_PAGADOR, tipoPessoaPagador);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Tipo pessoa associado inválido."));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PENDENTE", "REMOVIDO"})
        void dadoFiltroStatusInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String status) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_STATUS, status);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Lista de Status da recorrencia inválida"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"AGENDADO_AUTOMATICO", "RECORRENTE_AUTOMATICO", "RECORRENTE_AGENDADO"})
        void dadoFiltroTipoRecorrenciaInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String tipoRecorrencia) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_TIPO_RECORRENCIA, tipoRecorrencia);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Lista de tipo de recorrência inválida"));
        }

        @Test
        void dadoFiltroNumeroPaginaNulo_quandoConsultarRecorrencias_deveRetornarBadRequest() throws Exception {
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
        void dadoFiltroNumeroPaginaInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String numeroPagina) throws Exception {
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
        void dadoFiltroTamanhoPaginaNulo_quandoConsultarRecorrencias_deveRetornarBadRequest() throws Exception {
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
        void dadoFiltroTamanhoPaginaInvalido_quandoConsultarRecorrencias_deveRetornarBadRequest(String tamanhoPagina) throws Exception {
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
