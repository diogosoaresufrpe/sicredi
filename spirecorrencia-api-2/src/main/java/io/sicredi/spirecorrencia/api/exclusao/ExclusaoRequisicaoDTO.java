package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaParcelaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, ExclusaoRequisicaoDTO.class})
public final class ExclusaoRequisicaoDTO extends ProtocoloBaseRequest implements IdempotenteRequest {

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.ID_RECORRENCIA_EXCLUSAO_NOTNULL, groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Titles.ID_RECORRENCIA_EXCLUSAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Exemplos.ID_RECORRENCIA_EXCLUSAO)
    private String identificadorRecorrencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_MOTIVO_EXCLUSAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_MOTIVO_EXCLUSAO, implementation = TipoMotivoExclusao.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_MOTIVO_EXCLUSAO_NOTNULL)
    private TipoMotivoExclusao tipoMotivoExclusao;

    @UniqueElements(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.LIST_PARCELAS_UNIQUE, groups = PrimaryOrder.class)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.LIST_PARCELAS_EXCLUSAO_NOTNULL, groups = PrimaryOrder.class)
    @Size(min = 1, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.LIST_PARCELAS_EXCLUSAO_SIZE, groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Titles.LIST_PARCELAS_EXCLUSAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Exemplos.LIST_PARCELAS_EXCLUSAO)
    private List<String> identificadoresParcelas;

    @JsonIgnore
    private boolean fluxoLiquidacao;

    @Override
    public ProtocoloDTO getProtocoloDTO() {
        return this.protocoloDTO;
    }

    @Override
    public ZonedDateTime getDataHoraRecepcao() {
        return this.dataHoraRecepcao;
    }

    @Override
    public String getIdentificadorTransacao() {
        return this.identificadorTransacao;
    }

    private static LocalDateTime criarDataHoraInicioCanal(String dataHoraInicioCanal) {
        return Optional.ofNullable(dataHoraInicioCanal)
                .map(header -> ZonedDateTime.parse(header, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);
    }

    public static ExclusaoRequisicaoDTO fromEmissaoProtocolo(ProtocoloDTO protocolo,
                                                             List<ExclusaoRecorrenciaParcelaTransacaoDTO> listaParcelas,
                                                             String identificadorTransacao,
                                                             String dataHoraInicioCanal,
                                                             ZonedDateTime dataHoraRecepcao) {
        var exclusaoDTO = listaParcelas.getFirst();

        var identificadoresParcelas = listaParcelas.stream()
                .map(ExclusaoRecorrenciaParcelaTransacaoDTO::getIdentificadorParcela)
                .toList();

        return  ExclusaoRequisicaoDTO.builder()
                    .identificadorRecorrencia(exclusaoDTO.getIdentificadorRecorrencia())
                    .tipoMotivoExclusao(exclusaoDTO.getTipoMotivoExclusao())
                    .identificadoresParcelas(identificadoresParcelas)
                    .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                    .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                    .dataHoraRecepcao(dataHoraRecepcao)
                    .protocoloDTO(protocolo)
                    .fluxoLiquidacao(CODIGO_PROTOCOLO_EXCLUSAO_INTEGRADA.equals(protocolo.getCodigoTipoTransacao()))
                    .identificadorTransacao(identificadorTransacao)
                    .build();
    }
}
