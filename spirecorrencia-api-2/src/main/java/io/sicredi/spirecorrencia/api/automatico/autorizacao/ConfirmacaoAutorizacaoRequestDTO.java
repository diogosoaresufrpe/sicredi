package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.spi.entities.type.MotivoRejeicaoPain012;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.MotivoRejeicao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, ConfirmacaoAutorizacaoRequestDTO.class})
public final class ConfirmacaoAutorizacaoRequestDTO extends ProtocoloBaseRequest implements IdempotenteRequest {

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_SOLICITACAO_RECORRENCIA_NOTBLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_SOLICITACAO_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_SOLICITACAO_RECORRENCIA)
    private String idSolicitacaoRecorrencia;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_RECORRENCIA_NOTBLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_RECORRENCIA_AUTORIZACAO)
    private String idRecorrencia;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_INFORMACAO_STATUS_NOTBLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_INFORMACAO_STATUS, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_INFORMACAO_STATUS)
    private String idInformacaoStatus;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.VALOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.VALOR)
    @Digits(integer = 16, fraction = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.VALOR_DIGITS)
    private BigDecimal valorMaximo;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.APROVADA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.APROVADA)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.APROVADA_NOTNULL)
    private Boolean aprovada;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.MOTIVO_REJEICAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.MOTIVO_REJEICAO, implementation = MotivoRejeicao.class, enumAsRef = true)
    private MotivoRejeicaoPain012 motivoRejeicao;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.CPF_CNPJ, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_NOTBLANK)
    private String cpfCnpjPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_CANAL, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_CANAL)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_CANAL_NOTNULL)
    private TipoCanalEnum tipoCanalPagador;

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

    @Override
    public Boolean deveSinalizacaoSucessoProtocolo() {
        return Boolean.FALSE;
    }

    public static LocalDateTime criarDataHoraInicioCanal(String dataHoraInicioCanal) {
        return Optional.ofNullable(dataHoraInicioCanal)
                .map(header -> ZonedDateTime.parse(header, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);
    }
}