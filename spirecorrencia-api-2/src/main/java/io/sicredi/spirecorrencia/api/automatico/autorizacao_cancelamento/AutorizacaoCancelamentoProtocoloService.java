package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;

public interface AutorizacaoCancelamentoProtocoloService {

    IdempotentResponse<?> processaCancelamentoRecorrenciaAutorizacao(IdempotentRequest<CancelamentoAutorizacaoRequest> request);

}
