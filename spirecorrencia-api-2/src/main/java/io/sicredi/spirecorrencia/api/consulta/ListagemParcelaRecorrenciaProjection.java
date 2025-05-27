package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface ListagemParcelaRecorrenciaProjection {

    String getIdentificadorParcela();
    String getIdentificadorRecorrencia();
    BigDecimal getValor();
    LocalDate getDataTransacao();
    LocalDateTime getDataExclusao();
    TipoStatusEnum getStatus();
    TipoRecorrencia getTipoRecorrencia();
    String getNomeRecebedor();
}