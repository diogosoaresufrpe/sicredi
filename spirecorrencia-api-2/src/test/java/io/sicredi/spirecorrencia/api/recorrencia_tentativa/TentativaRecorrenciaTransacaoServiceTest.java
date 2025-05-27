package io.sicredi.spirecorrencia.api.recorrencia_tentativa;

import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TentativaRecorrenciaTransacaoServiceTest {
    @InjectMocks
    private TentativaRecorrenciaTransacaoService tentativaRecorrenciaTransacaoService;

    @Mock
    private RecorrenciaTransacaoTentativaRepository tentativaRepository;

    @Test
    void dadoRecorrenciaTransacaoValida_quandoRegistrarRecorrenciaTransacaoTentativa_deveSalvarTentativa() {
        RecorrenciaTransacao recorrenciaTransacao = mock(RecorrenciaTransacao.class);
        when(recorrenciaTransacao.getIdFimAFim()).thenReturn("idFimAFim");

        tentativaRecorrenciaTransacaoService.registrarRecorrenciaTransacaoTentativa("motivo", "codigo", recorrenciaTransacao);

        verify(tentativaRepository, times(1)).save(any(RecorrenciaTransacaoTentativa.class));
    }

    @Test
    void dadoRecorrenciaTransacaoNula_quandoRegistrarRecorrenciaTransacaoTentativa_deveLancarExcecao() {
        assertThrows(NullPointerException.class, () ->
                tentativaRecorrenciaTransacaoService.registrarRecorrenciaTransacaoTentativa("motivo", "codigo", null)
        );
    }

}