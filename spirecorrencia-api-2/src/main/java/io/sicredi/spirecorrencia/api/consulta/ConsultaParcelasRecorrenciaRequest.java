package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.ValidTamanhoPaginaListagem;
import io.sicredi.spirecorrencia.api.RecorrenciaConstantes;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.ConsultaRecorrencia.Schemas.Titles.TIPO_RECORRENCIA_DESCRICAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Pagador.Validations.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Exemplos.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Titles.NUMERO_PAGINA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Titles.TAMANHO_PAGINA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Paginacao.Schemas.Validations.*;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_RECORRENCIA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Recorrencia.Validations.TIPO_RECORRENCIA_PATTERN_MESSAGE;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Exemplos.TIPOS_STATUS_EXEMPLOS;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Exemplos.TIPO_STATUS;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Titles.STATUS_PARCELA;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Titles.STATUS_PARCELA_DESCRICAO;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.RecorrenciaTransacao.Schemas.Validations.STATUS_PATTERN_MESSAGE;
import static io.sicredi.spirecorrencia.api.RecorrenciaConstantes.Regex.*;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Getter
@Setter
@Validated
public class ConsultaParcelasRecorrenciaRequest {

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.CONTA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.CONTA, pattern = PATTERN_CONTA, requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = PATTERN_CONTA, message = CONTA_PATTERN_MESSAGE)
    @NotBlank(message = CONTA_NOTBLANK)
    private String contaPagador;

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.AGENCIA, description = RecorrenciaConstantes.Pagador.Schemas.Titles.AGENCIA, pattern = PATTERN_AGENCIA, requiredMode = Schema.RequiredMode.REQUIRED)
    @Pattern(regexp = PATTERN_AGENCIA, message = AGENCIA_PATTERN_MESSAGE)
    @NotBlank(message = AGENCIA_PATTERN)
    private String agenciaPagador;

    @Schema(example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ, description = RecorrenciaConstantes.Pagador.Schemas.Titles.CPF_CNPJ, pattern = PATTERN_CPF_CNPJ)
    @Pattern(regexp = PATTERN_CPF_CNPJ, message = CPF_CNPJ_PATTERN_MESSAGE)
    private String cpfCnpjPagador;

    @Schema(description = RecorrenciaConstantes.Pagador.Schemas.Titles.TIPO_PESSOA, example = RecorrenciaConstantes.Pagador.Schemas.Exemplos.TIPO_PESSOA, implementation = TipoPessoaEnum.class, enumAsRef = true)
    @Pattern(regexp = PATTERN_TIPO_PESSOA, message = TIPO_PESSOA_PATTERN_MESSAGE)
    private String tipoPessoaPagador;

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = RecorrenciaConstantes.Recorrencia.Schemas.Titles.TIPO_RECORRENCIA, example = RecorrenciaConstantes.Recorrencia.Schemas.Exemplos.TIPO_RECORRENCIA, implementation = TipoRecorrencia.class, enumAsRef = true),
            arraySchema = @Schema(example = TIPO_RECORRENCIA, description = TIPO_RECORRENCIA_DESCRICAO))
    private Set<@Pattern(regexp = PATTERN_TIPO_RECORRENCIA, message = TIPO_RECORRENCIA_PATTERN_MESSAGE) String> tipoRecorrencia;

    @ArraySchema(
            uniqueItems = true,
            schema = @Schema(description = STATUS_PARCELA, example = TIPO_STATUS, implementation = TipoStatusEnum.class, enumAsRef = true),
            arraySchema = @Schema(example = TIPOS_STATUS_EXEMPLOS, description = STATUS_PARCELA_DESCRICAO))
    private Set<@Pattern(regexp = PATTERN_TIPO_STATUS_PARCELA, message = STATUS_PATTERN_MESSAGE) String> status;

    @Schema(description = "Data de início da busca. A data utilizada para filtro é a data de transação da recorrência transação (DAT_TRANSACAO).", example = "2021-01-01")
    private LocalDate dataInicial;

    @Schema(description = "Data final da busca. A data utilizada para filtro é a data de transação da recorrência transação (DAT_TRANSACAO)", example = "2021-01-01")
    private LocalDate dataFinal;

    @Schema(description = TAMANHO_PAGINA, minimum = TAMANHO_PAGINA_EXEMPLO_MIN, maximum = TAMANHO_PAGINA_EXEMPLO_MAX, example = TAMANHO_PAGINA_EXEMPLO, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = TAMANHO_PAGINA_NOTNULL)
    @ValidTamanhoPaginaListagem
    private Integer tamanhoPagina;

    @Schema(description = NUMERO_PAGINA, minimum = NUMERO_PAGINA_EXEMPLO, example = NUMERO_PAGINA_EXEMPLO, requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(value = 0, message = NUMERO_PAGINA_MIN)
    @NotNull(message = NUMERO_PAGINA_NOTNULL)
    private Integer numeroPagina;


    /**
     * Obtém o tipo de pessoa do pagador.
     * <p>
     * Apesar de o IntelliJ IDEA ou outras ferramentas indicarem que este método não está sendo utilizado,
     * ele é utilizado indiretamente na classe {@code RecorrenciaTransacaoRepository}, no método {@code findParcelasByFiltros}.
     * Esse método é chamado dentro da query dinâmica para realizar o mapeamento correto dos parâmetros.
     *
     * @return o tipo de pessoa do pagador, ou {@code null} se não estiver definido.
     */
    public TipoPessoaEnum obterTipoPessoaPagador() {
        if (StringUtils.isNotBlank(tipoPessoaPagador)) {
            return TipoPessoaEnum.valueOf(tipoPessoaPagador);
        }
        return null;
    }

    /**
     * Obtém o primeiro elemento do conjunto de status.
     * <p>
     * Apesar de o IntelliJ IDEA ou outras ferramentas indicarem que este método não está sendo utilizado,
     * ele é utilizado indiretamente na classe {@code RecorrenciaTransacaoRepository}, no método {@code findParcelasByFiltros}.
     * Esse método é chamado dentro da query dinâmica para realizar o mapeamento correto dos parâmetros.
     *
     * @return o primeiro elemento do conjunto de status, ou {@code null} se o conjunto estiver vazio ou nulo.
     */
    public String obterPrimeiroElementoStatus() {
        return Optional.ofNullable(status)
                .flatMap(x -> x.stream().findFirst())
                .orElse(null);
    }

    /**
     * Obtém o primeiro elemento do conjunto de tipos de recorrência.
     * <p>
     * Apesar de o IntelliJ IDEA ou outras ferramentas indicarem que este método não está sendo utilizado,
     * ele é utilizado indiretamente na classe {@code RecorrenciaTransacaoRepository}, no método {@code findParcelasByFiltros}.
     * Esse método é chamado dentro da query dinâmica para realizar o mapeamento correto dos parâmetros.
     *
     * @return o primeiro elemento do conjunto de tipos de recorrência, ou {@code null} se o conjunto estiver vazio ou nulo.
     */
    public String obterPrimeiroElementoTipoRecorrencia() {
        return Optional.ofNullable(tipoRecorrencia)
                .flatMap(x -> x.stream().findFirst())
                .orElse(null);
    }

}
