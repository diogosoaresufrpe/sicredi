package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.consulta.ConsultaBaseRequest;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ConsultaRecorrencia.Schemas.Titles.TIPO_STATUS_AUTORIZACAO_DESCRICAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPOS_STATUS_AUTORIZACAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Validations.TIPO_STATUS_AUTORIZACAO_PATTERN_MESSAGE;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.PATTERN_TIPO_STATUS_AUTORIZACAO;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Validated
public class ConsultaAutorizacaoRequest extends ConsultaBaseRequest {

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_STATUS_AUTORIZACAO, example = RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPO_STATUS_AUTORIZACAO, pattern = PATTERN_TIPO_STATUS_AUTORIZACAO, implementation = TipoStatusAutorizacao.class, enumAsRef = true),
            arraySchema = @Schema(example = TIPOS_STATUS_AUTORIZACAO, description = TIPO_STATUS_AUTORIZACAO_DESCRICAO))
    private Set<@Pattern(regexp = PATTERN_TIPO_STATUS_AUTORIZACAO, message = TIPO_STATUS_AUTORIZACAO_PATTERN_MESSAGE) String> status;
}