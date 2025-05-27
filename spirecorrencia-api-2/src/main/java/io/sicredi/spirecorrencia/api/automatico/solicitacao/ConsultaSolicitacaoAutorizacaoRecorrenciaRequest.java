package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.consulta.ConsultaBaseRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ConsultaRecorrencia.Schemas.Titles.TIPO_STATUS_SOLICITACAO_AUTORIZACAO_DESCRICAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPOS_STATUS_SOLICITACAO_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Validations.TIPO_STATUS_SOLICITACAO_AUTORIZACAO_PATTERN_MESSAGE;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.PATTERN_TIPO_STATUS_SOLICITACAO_AUTORIZACAO;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Validated
public class ConsultaSolicitacaoAutorizacaoRecorrenciaRequest extends ConsultaBaseRequest {

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_STATUS_SOLICITACAO_AUTORIZACAO, example = RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPO_STATUS_SOLICITACAO_AUTORIZACAO, pattern = PATTERN_TIPO_STATUS_SOLICITACAO_AUTORIZACAO, implementation = TipoStatusSolicitacaoAutorizacao.class, enumAsRef = true),
            arraySchema = @Schema(example = TIPOS_STATUS_SOLICITACAO_AUTORIZACAO, description = TIPO_STATUS_SOLICITACAO_AUTORIZACAO_DESCRICAO))
    private Set<@Pattern(regexp = PATTERN_TIPO_STATUS_SOLICITACAO_AUTORIZACAO, message = TIPO_STATUS_SOLICITACAO_AUTORIZACAO_PATTERN_MESSAGE) String> status;
}