package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Setter
@Getter
@Builder
class ParcelaRecorrenciaMock implements ListagemParcelaRecorrenciaProjection {
    private String identificadorParcela;
    private String identificadorRecorrencia;
    private BigDecimal valor;
    private LocalDate dataTransacao;
    private LocalDateTime dataExclusao;
    private TipoStatusEnum status;
    private TipoRecorrencia tipoRecorrencia;
    private String nomeRecebedor;
}
