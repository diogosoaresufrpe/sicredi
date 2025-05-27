package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.ValidTamanhoPaginaListagem;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Pagador.Validations.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Exemplos.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Titles.NUMERO_PAGINA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Titles.TAMANHO_PAGINA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Validations.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.*;

@SuperBuilder
@Getter
@Setter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ConsultaBaseRequest {

    @Schema(description = RecorrenciaConstantes.Pagador.Schemas.Titles.TIPO_PESSOA, example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.TIPO_PESSOA, implementation = TipoPessoaEnum.class, enumAsRef = true)
    @Pattern(regexp = PATTERN_TIPO_PESSOA, message = TIPO_PESSOA_PATTERN_MESSAGE)
    private String tipoPessoaPagador;

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ, description = RecorrenciaConstantes.Pagador.Schemas.Titles.CPF_CNPJ, pattern = PATTERN_CPF_CNPJ)
    @Pattern(regexp = PATTERN_CPF_CNPJ, message = CPF_CNPJ_PATTERN_MESSAGE)
    private String cpfCnpjPagador;

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.AGENCIA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.AGENCIA, pattern = PATTERN_AGENCIA, requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = PATTERN_AGENCIA, message = AGENCIA_PATTERN_MESSAGE)
    @NotBlank(message = AGENCIA_PATTERN)
    private String agenciaPagador;

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.CONTA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.CONTA, pattern = PATTERN_CONTA, requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = PATTERN_CONTA, message = CONTA_PATTERN_MESSAGE)
    @NotBlank(message = CONTA_NOTBLANK)
    private String contaPagador;

    @Schema(description = "Data de início da busca. A data utilizada para filtro é a data de criação da recorrência (DAT_CRIACAO/DAT_CRIACAO_RECORRENCIA/DAT_TRANSACAO).", example = "2021-01-01T00:00:00")
    private LocalDateTime dataInicial;

    @Schema(description = "Data final da busca. A data utilizada para filtro é a data de criação da recorrência (DAT_CRIACAO/DAT_CRIACAO_RECORRENCIA/DAT_TRANSACAO)", example = "2021-01-01T00:00:00")
    private LocalDateTime dataFinal;

    @Schema(description = TAMANHO_PAGINA, minimum = TAMANHO_PAGINA_EXEMPLO_MIN, maximum = TAMANHO_PAGINA_EXEMPLO_MAX, example = TAMANHO_PAGINA_EXEMPLO, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = TAMANHO_PAGINA_NOTNULL)
    @ValidTamanhoPaginaListagem
    private Integer tamanhoPagina;

    @Schema(description = NUMERO_PAGINA, minimum = NUMERO_PAGINA_EXEMPLO, example = NUMERO_PAGINA_EXEMPLO, requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 0, message = NUMERO_PAGINA_MIN)
    @NotNull(message = NUMERO_PAGINA_NOTNULL)
    private Integer numeroPagina;

    public TipoPessoaEnum obterTipoPessoaPagador() {
        if (StringUtils.isNotBlank(tipoPessoaPagador)) {
            return TipoPessoaEnum.valueOf(tipoPessoaPagador);
        }
        return null;
    }
}