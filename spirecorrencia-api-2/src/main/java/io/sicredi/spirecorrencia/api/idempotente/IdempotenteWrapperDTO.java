package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;

import java.time.LocalDateTime;

public record IdempotenteWrapperDTO<T>(
        String identificador,
        T objeto,
        LocalDateTime dataRecepcao,
        ProtocoloDTO protocoloDTO) {
}
