package io.sicredi.spirecorrencia.api.commons;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@SuperBuilder
@Getter
@Setter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ProtocoloBaseRequest {

    @JsonIgnore
    protected String identificadorTransacao;

    @JsonIgnore
    protected LocalDateTime dataHoraInicioCanal;

    @JsonIgnore
    protected TipoResponseIdempotente tipoResponse;

    @JsonIgnore
    protected ProtocoloDTO protocoloDTO;

    @JsonIgnore
    protected ZonedDateTime dataHoraRecepcao;
}
