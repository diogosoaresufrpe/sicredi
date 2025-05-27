package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventoExclusaoPayloadDTO(Long oidRecorrenciaTransacao, TipoMotivoExclusao tipoMotivoExclusao, LocalDateTime dataExclusao, String identificadorParcela, String idFimAFim) {
}
