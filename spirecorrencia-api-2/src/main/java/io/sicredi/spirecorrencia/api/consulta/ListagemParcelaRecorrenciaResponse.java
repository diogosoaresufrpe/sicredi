package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Response para listagem de parcelas da recorrência Pix")
class ListagemParcelaRecorrenciaResponse {

    @Schema(title = "ID da Parcela da Recorrência")
    private String identificadorParcela;

    @Schema(title = "ID da Recorrência")
    private String identificadorRecorrencia;

    @Schema(title = "Valor da parcela", example = "89.90")
    private BigDecimal valor;

    @Schema(title = "Data para efetivação da parcela", example = "2025-10-10")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataTransacao;

    @Schema(title = "Data de exclusão da parcela", example = "2025-09-10")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataExclusao;

    @Schema(title = "Status da parcela", example = "CRIADO")
    private TipoStatusEnum status;

    @Schema(title = "Tipo de recorrência")
    private TipoRecorrencia tipoRecorrencia;

    @Schema(title = "Dados do recebedor")
    private RecebedorResponse recebedor;

    public static ListagemParcelaRecorrenciaResponse fromTransacaoRecorrencia(ListagemParcelaRecorrenciaProjection recorrenciaTransacao) {
        return Optional.ofNullable(recorrenciaTransacao)
                .map(recorrenciaTransacaoFilter -> ListagemParcelaRecorrenciaResponse.builder()
                        .identificadorParcela(recorrenciaTransacaoFilter.getIdentificadorParcela())
                        .identificadorRecorrencia(recorrenciaTransacaoFilter.getIdentificadorRecorrencia())
                        .tipoRecorrencia(recorrenciaTransacaoFilter.getTipoRecorrencia())
                        .dataTransacao(recorrenciaTransacaoFilter.getDataTransacao())
                        .dataExclusao(recorrenciaTransacaoFilter.getDataExclusao())
                        .recebedor(RecebedorResponse.fromNomeRecebedor(recorrenciaTransacao.getNomeRecebedor()))
                        .valor(recorrenciaTransacaoFilter.getValor())
                        .status(recorrenciaTransacaoFilter.getStatus())
                        .build()
                ).orElse(null);
    }

}
