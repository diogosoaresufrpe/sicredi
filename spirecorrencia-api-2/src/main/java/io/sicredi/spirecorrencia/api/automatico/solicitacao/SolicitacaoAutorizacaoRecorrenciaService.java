package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.spi.dto.Pain009Dto;
import br.com.sicredi.spi.dto.Pain012Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;

import java.time.LocalDateTime;

public interface SolicitacaoAutorizacaoRecorrenciaService {

    IdempotentResponse<?> processarSolicitacaoAutorizacao(IdempotentRequest<Pain009Dto> request);

    IdempotentResponse<?> processarRetornoBacenSolicitacaoAutorizacao(IdempotentRequest<Pain012Dto> request);

    SolicitacaoAutorizacaoRecorrenciaResponseWrapper consultarTodas(final ConsultaSolicitacaoAutorizacaoRecorrenciaRequest request);

    SolicitacaoAutorizacaoRecorrenciaResponse consultarDetalhes(String idSolicitacaoRecorrencia);

    SolicitacaoAutorizacaoRecorrencia buscarSolicitacaoAutorizacaoPorIdSolicitacaoEStatus(String idSolicitacao, TipoStatusSolicitacaoAutorizacao status);

    void atualizaRecorrenciaAutorizacaoSolicitacao(String id, LocalDateTime dataHoraInicioCanal, String codErro, TipoStatusSolicitacaoAutorizacao status,
                                                   String subStatus);
}
