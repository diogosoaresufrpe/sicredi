package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString
public class CancelamentoAutorizacaoRequest extends ProtocoloBaseRequest implements IdempotenteRequest {
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.OID_RECORRENCIA_AUTORIZACAO_NOTNULL)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.OID_RECORRENCIA_AUTORIZACAO,
            example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.OID_RECORRENCIA_AUTORIZACAO)
    private Long oidRecorrenciaAutorizacao;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_INFORMACAO_CANCELAMENTO_NOTBLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_INFORMACAO_CANCELAMENTO,
            example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_INFORMACAO_CANCELAMENTO)
    private String idInformacaoCancelamento;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.CPF_CNPJ_SOLICITANTE,
            groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.Regex.PATTERN_CPF_CNPJ,
            message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.CPF_CNPJ_SOLICITANTE,
            groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Titles.CPF_CNPJ_SOLICITANTE)
    private String cpfCnpjSolicitanteCancelamento;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Validations.MOTIVO_CANCELAMENTO)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Titles.MOTIVO_CANCELAMENTO,
            example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.ExcluirRecorrenciaTransacao.Schemas.Exemplos.MOTIVO_CANCELAMENTO)
    private String motivoCancelamento;

    @Override
    public Boolean deveSinalizacaoSucessoProtocolo() {
        return Boolean.FALSE;
    }

}
