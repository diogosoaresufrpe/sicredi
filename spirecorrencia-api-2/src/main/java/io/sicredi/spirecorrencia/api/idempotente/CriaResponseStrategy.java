package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import io.sicredi.engineering.libraries.idempotent.transaction.IdempotentResponse;
import io.sicredi.spirecorrencia.api.deadletter.DeadLetterRequest;

import java.util.List;
import java.util.Map;

public interface CriaResponseStrategy<T extends IdempotenteRequest> {

    default  IdempotentResponse<?> criarResponseIdempotentSucesso(T ordemRequest,
                                                                  String transactionId,
                                                                  Map<String, String> headers,
                                                                  List<EventoResponseDTO> listaEventos) {
        throw new UnsupportedOperationException("Este método não foi implementado.");
    }

    default  IdempotentResponse<?> criarResponseIdempotentErro(T ordemRequest,
                                                               String transactionId,
                                                               ErroDTO erro) {
        throw new UnsupportedOperationException("Este método não foi implementado.");
    }

    default IdempotentResponse<?> criarResponseIdempotentSucesso(ProtocoloDTO protocoloDTO,
                                                                 String transactionId,
                                                                 Map<String, String> headers,
                                                                 List<EventoResponseDTO> listaEventos) {
        throw new UnsupportedOperationException("Este método não foi implementado.");
    }

    default void criarResponseReprocessamentoIdempotentTransactionDuplicated(DeadLetterRequest deadLetterRequest,
                                                                            ErroDTO erro) {
        throw new UnsupportedOperationException("Este método não foi implementado.");
    }

    default void criarResponseReprocessamentoOutrasExceptions(DeadLetterRequest reprocessamentoRequest,
                                                              ErroDTO erro) {

    }

    TipoResponseIdempotente obterTipoResponse();
}
