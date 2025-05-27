package io.sicredi.spirecorrencia.api.automatico.expiracao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultaSolicitacaoExpiracaoPixAutomaticoServiceImplTest {

    @InjectMocks
    private ConsultaSolicitacaoExpiracaoPixAutomaticoServiceImpl notificacaoService;

    @Mock
    private SolicitacaoAutorizacaoRecorrenciaRepository repository;

    @Test
    void dadoConsultaSolicitacaoExpiracaoPixAutomatico_quandoHouverRegistroExpirado_deveRetornarListaDeSolicitacoes() {
        Pageable pageable = PageRequest.of(0, 10);
        SolicitacaoAutorizacaoRecorrencia solicitacao = getSolicitacao();

        Page<SolicitacaoAutorizacaoRecorrencia> mockPage = new PageImpl<>(List.of(solicitacao), pageable, 1);
        when(repository.buscaSolicitacaoDeAutorizacaoPixAutomaticoExpirada(
                any(Pageable.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO)
        )).thenReturn(mockPage);

        Page<SolicitacaoAutorizacaoRecorrencia> result = notificacaoService.buscarSolicitacoesExpiradas(pageable);

        assertEquals(1, result.getNumberOfElements());
        assertEquals("12345", result.getContent().get(0).getIdSolicitacaoRecorrencia());
        assertEquals("Test Pagador", result.getContent().get(0).getNomePagador());
    }

    @Test
    void dadoConsultaSolicitacaoExpiracaoPixAutomatico_quandoNaoHouverRegistros_deveRetornarListaVazia() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<SolicitacaoAutorizacaoRecorrencia> mockPage = Page.empty(pageable);

        when(repository.buscaSolicitacaoDeAutorizacaoPixAutomaticoExpirada(
                any(Pageable.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO)
        )).thenReturn(mockPage);

        Page<SolicitacaoAutorizacaoRecorrencia> result = notificacaoService.buscarSolicitacoesExpiradas(pageable);

        assertEquals(0, result.getNumberOfElements());
    }

    private static SolicitacaoAutorizacaoRecorrencia getSolicitacao() {
        return SolicitacaoAutorizacaoRecorrencia.builder()
                .idSolicitacaoRecorrencia("12345")
                .idRecorrencia("54321")
                .idInformacaoStatus("98765")
                .tipoStatus(TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO)
                .cpfCnpjPagador("12345678901")
                .nomePagador("Test Pagador")
                .agenciaPagador("1234")
                .contaPagador("00012345")
                .instituicaoPagador("12345678")
                .tipoPessoaPagador(TipoPessoaEnum.PF)
                .nomeRecebedor("Test Recebedor")
                .cpfCnpjRecebedor("10987654321")
                .instituicaoRecebedor("87654321")
                .dataCriacaoRecorrencia(LocalDateTime.now())
                .dataExpiracaoConfirmacaoSolicitacao(LocalDateTime.now().minusDays(1))
                .build();
    }
}