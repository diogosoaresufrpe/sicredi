package io.sicredi.spirecorrencia.api.automatico.autorizacao_cancelamento;

import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
@Setter
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@EqualsAndHashCode
public class AutorizacaoCancelamentoRequest {

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.OID_RECORRENCIA_AUTORIZACAO_NOTNULL)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.OID_RECORRENCIA_AUTORIZACAO,
            example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.OID_RECORRENCIA_AUTORIZACAO)
    private Long oidRecorrenciaAutorizacao;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_INFORMACAO_CANCELAMENTO_NOTBLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_INFORMACAO_CANCELAMENTO,
            example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_INFORMACAO_CANCELAMENTO)
    private String idInformacaoCancelamento;


}
