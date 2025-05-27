package io.sicredi.spirecorrencia.api.dict;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ChannelDataDTO {
    private Long oidRecorrencia;
    private Long oidRecorrenciaTransacao;
    private Long oidPagador;
    private Long oidRecebedor;
}
