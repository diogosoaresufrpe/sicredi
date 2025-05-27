package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import br.com.sicredi.spi.dto.Pain013Dto;
import br.com.sicredi.spi.dto.Pain014Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;

import java.util.Optional;

public interface RecorrenciaInstrucaoPagamentoService {

    IdempotentResponse<?> processarPedidoAgendamentoDebito(IdempotentAsyncRequest<Pain013Dto> request);

    IdempotentResponse<?> processarRetornoPedidoAgendamentoDebito(IdempotentAsyncRequest<Pain014Dto> request);

    Optional<RecorrenciaInstrucaoPagamento> buscarPorCodFimAFimComAutorizacao(String idFimAFim);

    void atualizaTpoStatusETpoSubStatus(String id, String status, String subStatus);

    RecorrenciaInstrucaoPagamento salvarInstrucaoPagamento(RecorrenciaInstrucaoPagamento recorrenciaInstrucaoPagamento);
}
