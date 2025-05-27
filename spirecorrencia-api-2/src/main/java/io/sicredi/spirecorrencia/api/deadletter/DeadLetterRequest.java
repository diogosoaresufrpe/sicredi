package io.sicredi.spirecorrencia.api.deadletter;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import lombok.Builder;

@Builder
public record DeadLetterRequest(ProtocoloDTO protocoloDTO,
                                String mensagemErro,
                                String causaException,
                                TipoResponseIdempotente tipoResponse,
                                String payload) {
}
