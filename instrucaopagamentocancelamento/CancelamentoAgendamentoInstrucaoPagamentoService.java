package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentRequest;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.idempotente.ErroDTO;
import io.sicredi.spirecorrencia.api.idempotente.ErroWrapperDTO;
import io.sicredi.spirecorrencia.api.idempotente.EventoResponseDTO;

import java.util.Optional;

public interface CancelamentoAgendamentoInstrucaoPagamentoService {

    IdempotentResponse<?> processarCancelamentoDebito(IdempotentRequest<CancelamentoAgendamentoDebitoRequest> request);

    ErroWrapperDTO<EventoResponseDTO> processarCancelamentoDebito(CancelamentoAgendamentoWrapperDTO wrapperDTO);

    Optional<ErroDTO> processarCancelamentoDebitoSemIdempotencia(CancelamentoAgendamentoWrapperDTO wrapperDTO);
}