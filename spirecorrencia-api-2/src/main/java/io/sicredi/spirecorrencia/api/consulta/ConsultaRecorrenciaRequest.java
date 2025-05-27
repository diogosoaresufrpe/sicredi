package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusFiltroEnum;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ConsultaRecorrencia.Schemas.Titles.TIPO_RECORRENCIA_DESCRICAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Validations.TIPO_RECORRENCIA_PATTERN_MESSAGE;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.PATTERN_TIPO_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.PATTERN_TIPO_STATUS;

@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
@Getter
@Setter
@Validated
public class ConsultaRecorrenciaRequest extends ConsultaBaseRequest {

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_RECORRENCIA, example = RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPO_RECORRENCIA, pattern = PATTERN_TIPO_RECORRENCIA, implementation = TipoRecorrencia.class, enumAsRef = true),
            arraySchema = @Schema(example = TIPO_RECORRENCIA, description = TIPO_RECORRENCIA_DESCRICAO))
    private Set<@Pattern(regexp = PATTERN_TIPO_RECORRENCIA, message = TIPO_RECORRENCIA_PATTERN_MESSAGE) String> tipoRecorrencia;

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = "Status da recorrencia", example = "CRIADO", pattern = "CRIADO|CONCLUIDO|EXCLUIDO", implementation = TipoStatusFiltroEnum.class, enumAsRef = true),
            arraySchema = @Schema(example = "[\"CRIADO\", \"CONCLUIDO\", \"EXCLUIDO\"]", description = "Conjunto de status para filtro na busca"))
    private Set<@Pattern(regexp = PATTERN_TIPO_STATUS, message = "Lista de Status da recorrencia inválida") String> status;
}
