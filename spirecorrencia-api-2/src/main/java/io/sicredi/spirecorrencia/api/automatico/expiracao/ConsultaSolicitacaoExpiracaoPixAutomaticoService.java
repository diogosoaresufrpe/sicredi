package io.sicredi.spirecorrencia.api.automatico.expiracao;

import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ConsultaSolicitacaoExpiracaoPixAutomaticoService {
    Page<SolicitacaoAutorizacaoRecorrencia> buscarSolicitacoesExpiradas(Pageable pageable);
}
