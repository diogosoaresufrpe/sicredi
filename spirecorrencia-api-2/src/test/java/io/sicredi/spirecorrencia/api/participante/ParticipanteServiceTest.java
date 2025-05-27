package io.sicredi.spirecorrencia.api.participante;

import feign.RetryableException;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipanteServiceTest {

    @InjectMocks
    private ParticipanteService participanteService;
    @Mock
    private ParticipanteClient participanteClient;

    @Test
    void dadoParticipanteExistente_quandoBuscarNomeInstituicao_deveRetornarNomeEsperado() {
        var nomeEsperado = "Nome Instituicao "+ RandomStringUtils.randomAlphabetic(4,4);
        var ispb = RandomStringUtils.randomNumeric(8,8);
        var participanteWrapperDTO = new ParticipanteWrapperDTO();
        var participanteDTO = new ParticipanteDTO(RandomStringUtils.randomNumeric(8,8), nomeEsperado);
        participanteWrapperDTO.setContent(List.of(participanteDTO));
        when(participanteClient.consultarInstituicao(any(), any(), any(), any())).thenReturn(participanteWrapperDTO);

        var retorno = participanteService.buscarNomeInstituicao(ispb);

        assertEquals(retorno, nomeEsperado);
        verify(participanteClient, times(1)).consultarInstituicao(0, 1, "ENABLE", ispb);
    }

    @Test
    void dadoParticipanteNaoExistente_quandoBuscarNomeInstituicao_deveRetornarNulo() {
        var participanteWrapperDTO = new ParticipanteWrapperDTO();
        participanteWrapperDTO.setContent(Collections.emptyList());
        when(participanteClient.consultarInstituicao(any(), any(), any(), any())).thenReturn(participanteWrapperDTO);

        var retorno = participanteService.buscarNomeInstituicao(RandomStringUtils.randomNumeric(8,8));

        assertNull(retorno);
    }

    @Test
    void dadoErroFeign_quandoBuscarNomeInstituicao_deveRetornarNulo() {
        when(participanteClient.consultarInstituicao(any(), any(), any(), any()))
                .thenThrow(mock(RetryableException.class));

        var retorno = participanteService.buscarNomeInstituicao(RandomStringUtils.randomNumeric(8,8));

        assertNull(retorno);
    }
}