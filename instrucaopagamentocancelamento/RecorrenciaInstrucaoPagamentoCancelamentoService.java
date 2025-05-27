package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spi.dto.Camt055Dto;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;

public interface RecorrenciaInstrucaoPagamentoCancelamentoService {

    IdempotentResponse<?> processarSolicitacaoCancelamento(IdempotentRequest<Camt055Dto> request);
}