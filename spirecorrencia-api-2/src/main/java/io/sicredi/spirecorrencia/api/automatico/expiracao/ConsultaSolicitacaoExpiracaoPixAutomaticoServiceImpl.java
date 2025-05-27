package io.sicredi.spirecorrencia.api.automatico.expiracao;

import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO;
import static java.time.LocalTime.MAX;
import static java.time.LocalTime.MIN;

@Slf4j
@Service
@RequiredArgsConstructor
class ConsultaSolicitacaoExpiracaoPixAutomaticoServiceImpl implements ConsultaSolicitacaoExpiracaoPixAutomaticoService {

    private final SolicitacaoAutorizacaoRecorrenciaRepository repository;

    public Page<SolicitacaoAutorizacaoRecorrencia> buscarSolicitacoesExpiradas(Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(0, 100);
        var transacoesPage = repository.buscaSolicitacaoDeAutorizacaoPixAutomaticoExpirada(
                pageRequest,
                LocalDateTime.now().with(MIN),
                LocalDateTime.now().with(MAX),
                PENDENTE_CONFIRMACAO
        );

        log.debug("Página {} carregada com {} solicitações.", pageRequest.getPageNumber(), transacoesPage.getNumberOfElements());
        return transacoesPage;
    }
}
