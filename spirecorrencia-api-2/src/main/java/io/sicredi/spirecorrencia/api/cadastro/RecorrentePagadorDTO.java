package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.PagadorRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = "Request para cadastramento de recorrências Pix")
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, RecorrentePagadorDTO.class})
public class RecorrentePagadorDTO {

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.CPF_CNPJ, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 11, max = 14, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_PATTERN, groups = PrimaryOrder.class)
    private String cpfCnpj;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.NOME, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.NOME_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.NOME_SIZE, groups = PrimaryOrder.class)
    private String nome;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.INSTITUICAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.INSTITUICAO)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 8, max = 8, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_INSTITUICAO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_PATTERN, groups = PrimaryOrder.class)
    private String instituicao;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.AGENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.AGENCIA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 4, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_PATTERN, groups = PrimaryOrder.class)
    private String agencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CONTA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 20, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_PATTERN, groups = PrimaryOrder.class)
    private String conta;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.UA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.UA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 2, max = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_UNIDADE_ATENDIMENTO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_PATTERN, groups = PrimaryOrder.class)
    private String posto;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.TIPO_CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.TIPO_CONTA, implementation = TipoContaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.TIPO_CONTA_NOTNULL, groups = PrimaryOrder.class)
    private TipoContaEnum tipoConta;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.TIPO_PESSOA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.TIPO_PESSOA, implementation = TipoPessoaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.TIPO_PESSOA_NOTNULL, groups = PrimaryOrder.class)
    private TipoPessoaEnum tipoPessoa;

    public static RecorrentePagadorDTO from(PagadorRequestDTO pagadorRequest) {
        return RecorrentePagadorDTO.builder()
                .cpfCnpj(pagadorRequest.getCpfCnpj())
                .nome(pagadorRequest.getNome())
                .instituicao(pagadorRequest.getInstituicao())
                .agencia(pagadorRequest.getAgencia())
                .conta(pagadorRequest.getConta())
                .posto(pagadorRequest.getPosto())
                .tipoConta(pagadorRequest.getTipoConta())
                .tipoPessoa(pagadorRequest.getTipoPessoa())
                .build();
    }

    public static RecorrentePagadorDTO from(CadastroOrdemPagamentoTransacaoDTO ordemAgendamentoComRecorrencia) {
        return RecorrentePagadorDTO.builder()
                .cpfCnpj(ordemAgendamentoComRecorrencia.getCpfCnpjUsuarioPagador())
                .nome(ordemAgendamentoComRecorrencia.getNomeUsuarioPagador())
                .instituicao(ordemAgendamentoComRecorrencia.getParticipantePagador().getIspb())
                .agencia(ordemAgendamentoComRecorrencia.getCooperativa())
                .conta(ordemAgendamentoComRecorrencia.getConta())
                .posto(ordemAgendamentoComRecorrencia.getAgencia())
                .tipoConta(ordemAgendamentoComRecorrencia.getTipoContaUsuarioPagador())
                .tipoPessoa(TipoPessoaEnum.valueOf(ordemAgendamentoComRecorrencia.getTipoPessoaConta().name()))
                .build();
    }

}
