package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.spi.dto.Pain011Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentAsyncRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;

public interface AutorizacaoCancelamentoService {

        IdempotentResponse<?> processarPedidoCancelamentoRecebedor(IdempotentRequest<Pain011Dto> request);

        IdempotentResponse<?> processarPedidoCancelamentoPagador(IdempotentAsyncRequest<Pain011Dto> request);
}
