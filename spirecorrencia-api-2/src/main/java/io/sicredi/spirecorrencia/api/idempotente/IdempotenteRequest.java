package io.sicredi.spirecorrencia.api.idempotente;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;

import java.time.ZonedDateTime;

public interface IdempotenteRequest {
    default ProtocoloDTO getProtocoloDTO() {
        return null;
    }
    default ZonedDateTime getDataHoraRecepcao() {
        return null;
    }
    default String getIdentificadorTransacao() {
        return null;
    }
    default Boolean deveSinalizacaoSucessoProtocolo() {
        return Boolean.TRUE;
    }
    default void setTipoResponse(TipoResponseIdempotente tipoResponse) {
    }
    default void setProtocoloDTO(ProtocoloDTO protocoloDTO) {
    }
}
