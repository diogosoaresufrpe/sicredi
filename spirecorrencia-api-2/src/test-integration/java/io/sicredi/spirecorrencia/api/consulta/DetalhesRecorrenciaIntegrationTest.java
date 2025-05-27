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

@SqlGroup(
        value = {
                @Sql(scripts = {"/db/clear.sql", "/db/data.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD),
                @Sql(scripts = "/db/clear.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_CLASS)
        }
)
class DetalhesRecorrenciaIntegrationTest extends AbstractIntegrationTest {

    private static final String URL_RECORRENCIAS = "/v1/recorrencias/{identificadorRecorrencia}";
    private static final String JSON_PATH_DETAILS_MESSAGE = "$.details[0].message";
    private static final String IDENTIFICADOR_RECORRENCIA_OID_1 = "2f335153-4d6a-4af1-92a0-c52c5c827af9";

    @Test
    void dadoFiltroAgenciaContaExistentes_quandoConsultarDetalheRecorrencia_deveRetornarPaginaComAsRecorrencias () throws Exception {

        var mockHttpServletRequestBuilder = executarGet(IDENTIFICADOR_RECORRENCIA_OID_1);

        mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.identificadorRecorrencia").value(IDENTIFICADOR_RECORRENCIA_OID_1));
    }

    @Nested
    class ValidacaoConstraints {

        @ParameterizedTest
        @ValueSource(strings = {" ", "\t", "\n", "\r", "\t\n\r"})
        void dadoIdentificadorRecorrenciaInvalido_quandoConsultarDetalheRecorrencia_deveRetornarBadRequest(String identificadorRecorrencia) throws Exception {
            var mockHttpServletRequestBuilder = executarGet(identificadorRecorrencia);

            mockMvc.perform(mockHttpServletRequestBuilder)
                    .andExpect(MockMvcResultMatchers.status().isBadRequest())
                    .andExpect(MockMvcResultMatchers.jsonPath(JSON_PATH_DETAILS_MESSAGE).value("Preenchimento do identificador único da recorrência é obrigatório"));
        }
    }

    private static MockHttpServletRequestBuilder executarGet(String identificadorRerecorrencia) {
        var url = URL_RECORRENCIAS.replace("{identificadorRecorrencia}", identificadorRerecorrencia);
        return MockMvcRequestBuilders.get(url)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .characterEncoding(StandardCharsets.UTF_8.name())
                .accept(MediaType.APPLICATION_JSON);
    }

}
