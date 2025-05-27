package io.sicredi.spirecorrencia.api.gestentconector;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.framework.web.spring.exception.NotFoundException;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GestentConectorServiceTest {

    @Mock
    private GestentConectorClient gestentConectorClient;

    @InjectMocks
    private GestentConectorService gestentConectorService;

    private static final String AGENCIA = "02";
    private static final String COOPERATIVA = "1234";

    @Test
    void dadoDadosValidosParaConsulta_quandoConsultarDadosCooperativa_deveRetornarCodigoIbge() {
        InformacoesCooperativaDTO dto = new InformacoesCooperativaDTO();
        String codigoIbge = "9876543";
        dto.setCodigoIbge(codigoIbge);
        Page<InformacoesCooperativaDTO> page = new PageImpl<>(List.of(dto));

        when(gestentConectorClient.consultarDadosCooperativa(eq(GestentConectorService.TIPO_ENTIDADE_AGENCIA), eq(AGENCIA), eq(COOPERATIVA), any(PageRequest.class))).thenReturn(page);

        String resultado = gestentConectorService.consultarCodigoMunicipio(AGENCIA, COOPERATIVA);

        assertThat(resultado).isEqualTo(codigoIbge);
    }

    @Test
    void dadoAgenciaECoopInvalidas_quandoConsultarDadosCooperativa_deveRetornarNull() {
        NotFoundException notFoundException = mock(NotFoundException.class);

        when(gestentConectorClient.consultarDadosCooperativa(anyString(), anyString(), anyString(), any(PageRequest.class))).thenThrow(notFoundException);

        String resultado = gestentConectorService.consultarCodigoMunicipio(AGENCIA, COOPERATIVA);

        assertThat(resultado).isNull();
    }

    @Test
    void dadoExcecaoGenerica_quandoConsultarDadosCooperativa_deveLancarExececaoTechnicalException() {
        RetryableException timeout = mock(RetryableException.class);

        when(gestentConectorClient.consultarDadosCooperativa(anyString(), anyString(), anyString(), any(PageRequest.class))).thenThrow(timeout);

        assertThatThrownBy(() -> gestentConectorService.consultarCodigoMunicipio(AGENCIA, COOPERATIVA))
                .isInstanceOf(TechnicalException.class)
                .hasCause(timeout);
    }
}