package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import wiremock.org.checkerframework.checker.units.qual.A;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoServiceImplTest {

    private RecorrenciaInstrucaoPagamentoCancelamentoRepository repository;
    private ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(RecorrenciaInstrucaoPagamentoCancelamentoRepository.class);

        // Simula uma instância do service que tenha acesso ao método privado/protegido
        service = spy(new ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoServiceImpl(null));
        doReturn(repository).when(service).getRecorrenciaInstrucaoPagamentoCancelamentoRepository(); // adapte conforme necessário
    }

    @Test
    void deveProcessarPaginasComConteudo() {
        // Arrange
        var dataLimite = LocalDateTime.now().minusHours(12);
        var conteudoPagina1 = List.of("abc123", "def456");
        var conteudoPagina2 = List.of("ghi789");

        Slice<String> slice1 = mock(Slice.class);
        when(slice1.getContent()).thenReturn(conteudoPagina1);
        when(slice1.hasContent()).thenReturn(true);

        Slice<String> slice2 = mock(Slice.class);
        when(slice2.getContent()).thenReturn(conteudoPagina2);
        when(slice2.hasContent()).thenReturn(true);

        Slice<String> sliceVazio = mock(Slice.class);
        when(sliceVazio.hasContent()).thenReturn(false);

        // Simula retorno paginado
        when(repository.getListaCodFimAFimByListStatusAndTipoPSPSolicitanteCancelamento(
                any(), any(), any(), any()))
                .thenReturn(slice1)  // página 0
                .thenReturn(slice2)  // página 1
                .thenReturn(sliceVazio); // página 2

        // Mock do método que processa os dados
        doNothing().when(service).atualizarStatusCancelamento(anyList());

        // Act
        service.atualizarConfirmacaoCancelamentoPixAutomaticoJobService();

        // Assert: verifica que o método foi chamado para cada página com conteúdo
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(service, times(2)).atualizarStatusCancelamento(captor.capture());

        List<List<String>> chamadas = captor.getAllValues();
        assertEquals(2, chamadas.size());
        assertEquals(conteudoPagina1, chamadas.get(0));
        assertEquals(conteudoPagina2, chamadas.get(1));
    }
}