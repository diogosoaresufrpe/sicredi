package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.RecebedorRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.utils.ChaveUtils;
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
import org.springframework.util.StringUtils;

import java.util.Optional;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.RECORRENCIA_REQUEST)
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, RecorrenteRecebedorDTO.class})
public class RecorrenteRecebedorDTO {

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.CPF_CNPJ, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CPF_CNPJ)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CPF_CNPJ_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 11, max = 14, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CPF_CNPJ_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CPF_CNPJ_PATTERN, groups = PrimaryOrder.class)
    private String cpfCnpj;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.NOME, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.NOME)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.NOME_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.NOME_SIZE, groups = PrimaryOrder.class)
    private String nome;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.AGENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.AGENCIA)
    @Size(min = 1, max = 4, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.AGENCIA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.AGENCIA_PATTERN, groups = PrimaryOrder.class)
    private String agencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CONTA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CONTA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 20, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CONTA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_CONTA_RECEBEDOR, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CONTA_PATTERN, groups = PrimaryOrder.class)
    private String conta;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.INSTITUICAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.INSTITUICAO)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.INSTITUICAO_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 8, max = 8, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.INSTITUICAO_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_INSTITUICAO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.INSTITUICAO_PATTERN, groups = PrimaryOrder.class)
    private String instituicao;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.TIPO_CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.TIPO_CONTA, implementation = TipoContaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.TIPO_CONTA_NOTNULL, groups = PrimaryOrder.class)
    private TipoContaEnum tipoConta;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.TIPO_PESSOA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.TIPO_PESSOA, implementation = TipoPessoaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.TIPO_PESSOA_NOTNULL, groups = PrimaryOrder.class)
    private TipoPessoaEnum tipoPessoa;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.TIPO_CHAVE, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.TIPO_CHAVE, implementation = TipoChaveEnum.class, enumAsRef = true)
    private TipoChaveEnum tipoChave;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Titles.CHAVE_PIX, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Schemas.Exemplos.CHAVE_PIX)
    @Size(min = 1, max = 77, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recebedor.Validations.CHAVE_SIZE, groups = PrimaryOrder.class)
    private String chave;

    public static RecorrenteRecebedorDTO from(RecebedorRequestDTO recebedorRequest) {
        var chave = recebedorRequest.getChave();
        var tipoChave = Optional.ofNullable(recebedorRequest.getTipoChave())
                .orElseGet(() -> {
                    if (StringUtils.hasText(chave)) {
                        return ChaveUtils.determinarTipoChave(chave);
                    }
                    return null;
                });
        return RecorrenteRecebedorDTO.builder()
                .cpfCnpj(recebedorRequest.getCpfCnpj())
                .nome(recebedorRequest.getNome())
                .agencia(recebedorRequest.getAgencia())
                .conta(recebedorRequest.getConta())
                .instituicao(recebedorRequest.getInstituicao())
                .tipoConta(recebedorRequest.getTipoConta())
                .tipoPessoa(recebedorRequest.getTipoPessoa())
                .tipoChave(tipoChave)
                .chave(chave)
                .build();
    }

    public static RecorrenteRecebedorDTO from(CadastroOrdemPagamentoTransacaoDTO ordemAgendamentoComRecorrencia, TipoPessoaEnum tipoPessoa) {
        var chave = ordemAgendamentoComRecorrencia.getChaveDict();
        var tipoChave = Optional.ofNullable(chave)
                .map(ChaveUtils::determinarTipoChave)
                .orElse(null);
        return RecorrenteRecebedorDTO.builder()
                .cpfCnpj(ordemAgendamentoComRecorrencia.getCpfCnpjUsuarioRecebedor())
                .nome(ordemAgendamentoComRecorrencia.getNomeUsuarioRecebedor())
                .agencia(ordemAgendamentoComRecorrencia.getAgenciaUsuarioRecebedor())
                .conta(ordemAgendamentoComRecorrencia.getContaUsuarioRecebedor())
                .instituicao(ordemAgendamentoComRecorrencia.getParticipanteRecebedor().getIspb())
                .tipoConta(ordemAgendamentoComRecorrencia.getTipoContaUsuarioRecebedor())
                .chave(chave)
                .tipoChave(tipoChave)
                .tipoPessoa(tipoPessoa)
                .build();
    }

    public static RecorrenteRecebedorDTO from(CadastroOrdemPagamentoTransacaoDTO ordemAgendamentoComRecorrencia, TipoPessoaEnum tipoPessoa, TipoChaveEnum tipoChaveEnum) {
        var chave = ordemAgendamentoComRecorrencia.getChaveDict();
        var tipoChave = Optional.ofNullable(tipoChaveEnum)
                .orElseGet(() -> {
                    if (StringUtils.hasText(chave)) {
                        return ChaveUtils.determinarTipoChave(chave);
                    }
                    return null;
                });
        return RecorrenteRecebedorDTO.builder()
                .cpfCnpj(ordemAgendamentoComRecorrencia.getCpfCnpjUsuarioRecebedor())
                .nome(ordemAgendamentoComRecorrencia.getNomeUsuarioRecebedor())
                .agencia(ordemAgendamentoComRecorrencia.getAgenciaUsuarioRecebedor())
                .conta(ordemAgendamentoComRecorrencia.getContaUsuarioRecebedor())
                .instituicao(ordemAgendamentoComRecorrencia.getParticipanteRecebedor().getIspb())
                .tipoConta(ordemAgendamentoComRecorrencia.getTipoContaUsuarioRecebedor())
                .chave(chave)
                .tipoChave(tipoChave)
                .tipoPessoa(tipoPessoa)
                .build();
    }

}
