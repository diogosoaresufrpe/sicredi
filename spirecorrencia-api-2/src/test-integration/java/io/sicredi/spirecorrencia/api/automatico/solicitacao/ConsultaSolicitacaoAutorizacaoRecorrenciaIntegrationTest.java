package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
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
                @Sql(scripts = {"/db/clear.sql", "/db/consulta_solicitacao_autorizacao.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
public class ConsultaSolicitacaoAutorizacaoRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_RECORRENCIAS = "/v1/automaticos/autorizacoes/solicitacoes";
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
    private static final String JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA = "$.solicitacoes[%s].idSolicitacaoRecorrencia";
    private static final String JSON_PATH_AUTOMATICO_INDEX_NOME_RECEBEDOR_SOLICITACAO_RECORRENCIA ="$.solicitacoes[%s].nomeRecebedor";
    private static final String JSON_PATH_AUTOMATICO_INDEX_DATA_EXPIRACAO_SOLICITACAO_RECORRENCIA ="$.solicitacoes[%s].dataExpiracao";
    private static final String JSON_PATH_AUTOMATICO_INDEX_DATA_CRIACAO_SOLICITACAO_RECORRENCIA ="$.solicitacoes[%s].dataCriacao";
    private static final String JSON_PATH_AUTOMATICO_INDEX_CONTRATO_SOLICITACAO_RECORRENCIA ="$.solicitacoes[%s].contrato";
    private static final String JSON_PATH_AUTOMATICO_INDEX_DESCRICAO_SOLICITACAO_RECORRENCIA ="$.solicitacoes[%s].descricao";
    private static final String JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS = "$.solicitacoes[%s].tpoStatus";
    private static final String JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS = "$.solicitacoes[%s].tpoSubStatus";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";

    @Test
    void dadoFiltroAgenciaContaExistentes_quandoConsultarSolicitacoesAutorizacao_deveRetornarPaginaSolicitacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(7))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusSolicitacaoAutorizacao.CRIADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 0)).value("SC0118152120250425041bYqAj6ef"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_NOME_RECEBEDOR_SOLICITACAO_RECORRENCIA, 0)).value("João da Silva"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_DATA_EXPIRACAO_SOLICITACAO_RECORRENCIA, 0)).value("2025-07-18T15:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_DATA_CRIACAO_SOLICITACAO_RECORRENCIA, 0)).value("2025-07-18T13:00:00"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_CONTRATO_SOLICITACAO_RECORRENCIA, 0)).value("CT-2025-0001"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_DESCRICAO_SOLICITACAO_RECORRENCIA, 0)).value("teste"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 1)).value(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 1)).value("SC0118152120250425041bYqAj6eg"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 2)).value(TipoStatusSolicitacaoAutorizacao.CONFIRMADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 2)).value("SC0118152120250425041bYqAj6eh"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_SUB_STATUS, 2)).value("AGUARDANDO_RETORNO"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 3)).value(TipoStatusSolicitacaoAutorizacao.ACEITA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 3)).value("SC0118152120250425041bYqAj6ei"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 4)).value(TipoStatusSolicitacaoAutorizacao.REJEITADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 4)).value("SC0118152120250425041bYqAj6ej"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 5)).value(TipoStatusSolicitacaoAutorizacao.CANCELADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 5)).value("SC0118152120250425041bYqAj6ek"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 6)).value(TipoStatusSolicitacaoAutorizacao.EXPIRADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 6)).value("SC0118152120250425041bYqAj6el"))
                .andDo(print());
    }

    @Test
    void dadoTodosFiltrosPreenchidos_quandoConsultarSolicitacoesAutorizacao_deveRetornarPaginaComAsSolicitacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_CPF_CNPJ_PAGADOR, "12690422115")
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_TIPO_PESSOA_PAGADOR, "PJ")
                .queryParam(QUERY_PARAM_STATUS, TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO.toString())
                .queryParam(QUERY_PARAM_DATA_INICIAL, "2025-07-18T12:00:00")
                .queryParam(QUERY_PARAM_DATA_FINAL, "2025-07-18T14:00:00")
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 0)).value("SC0118152120250425041bYqAj6eg"))
                .andDo(print());
    }

    @Test
    void dadoStatusAceitaOuCancelada_quandoConsultarSolicitacoesAutorizacao_deveRetornarPaginaComAsSolicitacoes() throws Exception {
        var mockHttpServletRequestBuilder = executarGet()
                .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                .queryParam(QUERY_PARAM_STATUS, TipoStatusSolicitacaoAutorizacao.ACEITA.toString())
                .queryParam(QUERY_PARAM_STATUS, TipoStatusSolicitacaoAutorizacao.CANCELADA.toString())
                .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_ELEMENTOS).value(2))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_TOTAL_PAGINAS).value(1))
                .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_PAGINACAO_NUMERO_PAGINA).value(0))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 0)).value(TipoStatusSolicitacaoAutorizacao.ACEITA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 0)).value("SC0118152120250425041bYqAj6ei"))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_TIPO_STATUS, 1)).value(TipoStatusSolicitacaoAutorizacao.CANCELADA.toString()))
                .andExpect(MockMvcResultMatchers.jsonPath(String.format(JSON_PATH_AUTOMATICO_INDEX_IDENTIFICADOR_SOLICITACAO_RECORRENCIA, 1)).value("SC0118152120250425041bYqAj6ek"))
                .andDo(print());
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {"", " ", "1234567890", "123456789012345"})
        void dadoFiltroCpfCnpjPagadorInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String cpfCnpjPagador) throws Exception {
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
        void dadoFiltroAgenciaPagadorInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String agenciaPagador) throws Exception {
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
        void dadoFiltroContaPagadorInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String contaPagador) throws Exception {
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
        void dadoFiltroStatusInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String status) throws Exception {
            var mockHttpServletRequestBuilder = executarGet()
                    .queryParam(QUERY_PARAM_NUMERO_PAGINA, NUMERO_PAGINA_ZERO)
                    .queryParam(QUERY_PARAM_TAMANHO_PAGINA, TAMANHO_PAGINA_DEZ)
                    .queryParam(QUERY_PARAM_AGENCIA_PAGADOR, COOPERATIVA)
                    .queryParam(QUERY_PARAM_CONTA_PAGADOR, CONTA)
                    .queryParam(QUERY_PARAM_STATUS, status);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Lista de tipo de status de solicitação de autorização inválida"));
        }


        @Test
        void dadoFiltroNumeroPaginaNulo_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest() throws Exception {
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
        void dadoFiltroNumeroPaginaInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String numeroPagina) throws Exception {
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
        void dadoFiltroTamanhoPaginaNulo_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest() throws Exception {
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
        void dadoFiltroTamanhoPaginaInvalido_quandoConsultarSolicitacoesAutorizacao_deveRetornarBadRequest(String tamanhoPagina) throws Exception {
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