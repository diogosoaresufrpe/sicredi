package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CadastroAutorizacaoRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Titles.*;
import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.Regex.*;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, CadastroAutorizacaoRequest.class})
@EqualsAndHashCode(callSuper = true)
public class CadastroAutorizacaoRequest extends ProtocoloBaseRequest implements IdempotenteRequest {

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_INFORMACAO_STATUS_NOTBLANK, groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_INFORMACAO_STATUS, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_INFORMACAO_STATUS)
    private String idInformacaoStatus;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_RECORRENCIA_NOTBLANK, groups = PrimaryOrder.class)
    @Pattern(regexp = PATTERN_ID_RECORRENCIA, groups = PrimaryOrder.class)
    @Schema(title = ID_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.ID_RECORRENCIA_AUTORIZACAO)
    private String idRecorrencia;

    @Schema(title = "Código identificador da transação (EndToEnd) do pagamento imediato. (Obrigatório para Jornada 3)", example = "E91586982202208151245099rD6AIAa7")
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_ID_FIM_A_FIM, message = "Código identificador da transação (EndToEnd) do pagamento imediato inválido.", groups = PrimaryOrder.class)
    private String idFimAFimPagamentoImediato;

    @Schema(title = DATA_RECEBIMENTO_PACS002)
    private LocalDateTime dataRecebimentoConfirmacaoPacs002PagamentoImediato;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.TIPO_JORNADA_NOTNULL, groups = PrimaryOrder.class)
    @Schema(title = TIPO_JORNADA, implementation = TipoJornada.class, enumAsRef = true)
    private TipoJornada tipoJornada;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.NUMERO_CONTRATO_NOTBLANK, groups = PrimaryOrder.class)
    @Size(max = 35)
    @Schema(title = NUMERO_CONTRATO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.CONTRATO)
    private String contrato;

    @Size(max = 35, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.OBJETO_PAGAMENTO_SIZE, groups = PrimaryOrder.class)
    @Schema(title = OBJETO_PAGAMENTO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.OBJETO_PAGAMENTO)
    private String objeto;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CODIGO_MUNICIPIO_IBGE_NOTBLANK, groups = PrimaryOrder.class)
    @Schema(title = CODIGO_MUNICIPIO_IBGE, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.CODIGO_MUNICIPIO_IBGE)
    private String codigoMunicipioIbge;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.NOME_DEVEDOR_NOTBLANK, groups = PrimaryOrder.class)
    @Size(max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.NOME_DEVEDOR_SIZE, groups = PrimaryOrder.class)
    @Schema(title = NOME_DEVEDOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.NOME)
    private String nomeDevedor;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CPF_CNPJ_DEVEDOR_NOTBLANK, groups = PrimaryOrder.class)
    @Size(max = 14, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CPF_CNPJ_DEVEDOR_SIZE, groups = PrimaryOrder.class)
    @Schema(title = CPF_CNPJ_DEVEDOR, example = SpiCanaisTransportContantes.Commons.Schemas.Exemplos.CPF_CNPJ)
    private String cpfCnpjDevedor;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.NOME_RECEBEDOR_NOTBLANK, groups = PrimaryOrder.class)
    @Size(max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.NOME_RECEBEDOR_SIZE, groups = PrimaryOrder.class)
    @Schema(title = NOME_RECEBEDOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.NOME_FANTASIA)
    private String nomeRecebedor;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CNPJ_RECEBEDOR_NOTBLANK, groups = PrimaryOrder.class)
    @Pattern(regexp = PATTERN_CNPJ, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CNPJ_RECEBEDOR_PATTERN, groups = PrimaryOrder.class)
    @Schema(title = CNPJ_RECEBEDOR, example = SpiCanaisTransportContantes.Commons.Schemas.Exemplos.CNPJ)
    private String cpfCnpjRecebedor;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.INSTITUICAO_RECEBEDOR_NOTBLANK, groups = PrimaryOrder.class)
    @Pattern(regexp = PATTERN_ISPB_INSTITUICAO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.INSTITUICAO_RECEBEDOR_PATTERN, groups = PrimaryOrder.class)
    @Schema(title = INSTITUICAO_RECEBEDOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.INSTITUICAO_RECEBEDOR)
    private String instituicaoRecebedor;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.TIPO_FREQUENCIA_NOTBLANK, groups = PrimaryOrder.class)
    @Schema(title = TIPO_FREQUENCIA, implementation = TipoFrequenciaPixAutomatico.class, enumAsRef = true)
    private TipoFrequenciaPixAutomatico tipoFrequencia;

    @Digits(integer = 16, fraction = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.VALOR_DIGITS, groups = PrimaryOrder.class)
    @Schema(title = VALOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.VALOR)
    private BigDecimal valor;

    @Digits(integer = 16, fraction = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.VALOR_DIGITS, groups = PrimaryOrder.class)
    @Schema(title = PISO_VALOR_MAXIMO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.VALOR)
    private BigDecimal pisoValorMaximo;

    @Digits(integer = 16, fraction = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.VALOR_DIGITS, groups = PrimaryOrder.class)
    @Schema(title = VALOR_MAXIMO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.VALOR)
    private BigDecimal valorMaximo;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.POLITICA_RETENTATIVAS_NOTBLANK, groups = PrimaryOrder.class)
    @Schema(title = POLITICA_RETENTATIVAS, implementation = PoliticaRetentativaRecorrenciaEnum.class, enumAsRef = true)
    private PoliticaRetentativaRecorrenciaEnum politicaRetentativa;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.DATA_INICIAL_RECORRENCIA_NOTBLANK, groups = PrimaryOrder.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @Schema(title = DATA_INICIAL_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.DATA_INICIAL_RECORRENCIA)
    private LocalDate dataInicialRecorrencia;

    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonSerialize(using = LocalDateSerializer.class)
    @Schema(title = DATA_FINAL_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.DATA_FINAL_RECORRENCIA)
    private LocalDate dataFinalRecorrencia;

    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(title = DATA_CRIACAO_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Exemplos.DATA_CRIACAO_RECORRENCIA)
    private LocalDateTime dataCriacaoRecorrencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_CANAL, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_CANAL, implementation = TipoCanalEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_CANAL_NOTNULL, groups = PrimaryOrder.class)
    private TipoCanalEnum tipoCanal;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.CPF_CNPJ, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CPF_CNPJ)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 11, max = 14, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CPF_CNPJ_PATTERN, groups = PrimaryOrder.class)
    private String cpfCnpjPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.NOME, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.NOME)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.NOME_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.NOME_SIZE, groups = PrimaryOrder.class)
    private String nomePagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.INSTITUICAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.INSTITUICAO)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 8, max = 8, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_INSTITUICAO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.INSTITUICAO_PATTERN, groups = PrimaryOrder.class)
    private String instituicaoPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.AGENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.AGENCIA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 4, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.AGENCIA_PATTERN, groups = PrimaryOrder.class)
    private String agenciaPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.CONTA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 1, max = 20, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.CONTA_PATTERN, groups = PrimaryOrder.class)
    private String contaPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.UA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.UA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_NOTBLANK, groups = PrimaryOrder.class)
    @Size(min = 2, max = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_UNIDADE_ATENDIMENTO, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.UA_PATTERN, groups = PrimaryOrder.class)
    private String postoPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.TIPO_CONTA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.TIPO_CONTA, implementation = TipoContaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.TIPO_CONTA_NOTNULL, groups = PrimaryOrder.class)
    private TipoContaEnum tipoContaPagador;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Titles.TIPO_PESSOA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Schemas.Exemplos.TIPO_PESSOA, implementation = TipoPessoaEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Pagador.Validations.TIPO_PESSOA_NOTNULL, groups = PrimaryOrder.class)
    private TipoPessoaEnum tipoPessoaPagador;

    @Schema(title = "Sistema de origem da conta", example = "LEGADO", implementation = OrigemEnum.class, enumAsRef = true)
    @NotNull(message = "Preenchimento do tipo de origem da conta é obrigatória.", groups = PrimaryOrder.class)
    private OrigemEnum tipoOrigemSistema;

    public static CadastroAutorizacaoRequest fromEmissaoProtocolo(ProtocoloDTO protocolo, CadastroAutorizacaoRecorrenciaTransacaoDTO transacao, String identificadorTransacao, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        final String ISPB_SICREDI = "01181521";

        return CadastroAutorizacaoRequest.builder()
                .idInformacaoStatus(transacao.getIdInformacaoStatus())
                .idRecorrencia(transacao.getIdRecorrencia())
                .tipoJornada(transacao.getTipoJornada())
                .contrato(transacao.getContrato())
                .objeto(transacao.getObjeto())
                .codigoMunicipioIbge(transacao.getCodigoMunicipioIbge())
                .nomeDevedor(transacao.getNomeDevedor())
                .cpfCnpjDevedor(transacao.getCpfCnpjDevedor())
                .nomeRecebedor(transacao.getNomeRecebedor())
                .cpfCnpjRecebedor(transacao.getCpfCnpjRecebedor())
                .instituicaoRecebedor(transacao.getInstituicaoRecebedor())
                .tipoFrequencia(transacao.getTipoFrequencia())
                .valor(transacao.getValor())
                .pisoValorMaximo(transacao.getPisoValorMaximo())
                .valorMaximo(transacao.getValorMaximo())
                .politicaRetentativa(transacao.getPoliticaRetentativa())
                .dataInicialRecorrencia(transacao.getDataInicialRecorrencia())
                .dataFinalRecorrencia(transacao.getDataFinalRecorrencia())
                .dataCriacaoRecorrencia(transacao.getDataCriacaoRecorrencia())
                .identificadorTransacao(identificadorTransacao)
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .protocoloDTO(protocolo)
                .dataHoraRecepcao(dataHoraRecebimentoMensagem)
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .tipoCanal(TipoCanalEnum.valueOf(transacao.getCanal()))
                .cpfCnpjPagador(transacao.getCpfCnpjConta())
                .nomePagador(transacao.getNomeSolicitante())
                .agenciaPagador(transacao.getCooperativa())
                .contaPagador(transacao.getConta())
                .postoPagador(transacao.getAgencia())
                .tipoOrigemSistema(transacao.getOrigemConta())
                .instituicaoPagador(ISPB_SICREDI)
                .tipoContaPagador(transacao.getTipoContaPagador())
                .idFimAFimPagamentoImediato(transacao.getIdFimAFimPagamentoImediato())
                .dataRecebimentoConfirmacaoPacs002PagamentoImediato(transacao.getDataRecebimentoConfirmacaoPacs002PagamentoImediato())
                .tipoPessoaPagador(TipoPessoaEnum.valueOf(transacao.getTipoPessoaConta().name()))
                .build();
    }

    @Override
    public String getIdentificadorTransacao() {
        return this.identificadorTransacao;
    }

    @Override
    public Boolean deveSinalizacaoSucessoProtocolo() {
        return Boolean.FALSE;
    }

    private static LocalDateTime criarDataHoraInicioCanal(String dataHoraInicioCanal) {
        return Optional.ofNullable(dataHoraInicioCanal)
                .map(header -> ZonedDateTime.parse(header, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);
    }


}
