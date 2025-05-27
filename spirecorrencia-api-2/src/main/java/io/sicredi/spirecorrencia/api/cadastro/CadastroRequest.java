package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.canaisdigitais.dto.seguranca.DispositivoAutenticacaoDTO;
import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.SecondaryOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuperBuilder
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.RECORRENCIA_REQUEST)
@ValidTipoFrequencia(groups = SecondaryOrder.class)
@GroupSequence({PrimaryOrder.class, SecondaryOrder.class, CadastroRequest.class})
public class CadastroRequest extends ProtocoloBaseRequest implements IdempotenteRequest {

    @Schema(title = "Identificador Único da recorrência")
    private String identificadorRecorrencia;

    @Schema(title = "Identificador Único da simulação de limites")
    private String identificadorSimulacaoLimites;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.PAGADOR_NOTNULL, groups = PrimaryOrder.class)
    @Valid
    private RecorrentePagadorDTO pagador;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.RECEBEDOR_NOTNULL, groups = PrimaryOrder.class)
    @Valid
    private RecorrenteRecebedorDTO recebedor;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.NOME, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.NOME)
    private String nome;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_CANAL, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_CANAL, implementation = TipoCanalEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_CANAL_NOTNULL, groups = PrimaryOrder.class)
    private TipoCanalEnum tipoCanal;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_INICIACAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_INICIACAO, implementation = TipoPagamentoPixEnum.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_INICIACAO_NOTNULL, groups = PrimaryOrder.class)
    private TipoPagamentoPixEnum tipoIniciacao;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_INICIACAO_CANAL, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_INICIACAO_CANAL, implementation = TipoIniciacaoCanal.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_INICIACAO_CANAL_NOTNULL, groups = PrimaryOrder.class)
    private TipoIniciacaoCanal tipoIniciacaoCanal;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_FREQUENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_FREQUENCIA, implementation = TipoFrequencia.class, enumAsRef = true)
    private TipoFrequencia tipoFrequencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.TIPO_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.TIPO_RECORRENCIA, implementation = TipoRecorrencia.class, enumAsRef = true)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.TIPO_RECORRENCIA_NOTNULL)
    private TipoRecorrencia tipoRecorrencia;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.NUM_INIC_CNPJ, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.NUM_INIC_CNPJ)
    @Size(min = 14, max = 14, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.NUM_INIC_CNPJ_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_APENAS_NUMEROS, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.NUM_INIC_CNPJ_PATTERN, groups = PrimaryOrder.class)
    private String numInicCnpj;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.RECORRENCIA_TRANSACAO_NOTNULL, groups = PrimaryOrder.class)
    @Size(min = 1, max = 730, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.RECORRENCIA_TRANSACAO_SIZE, groups = PrimaryOrder.class)
    @Valid
    private List<RecorrenteParcelaRequisicaoDTO> parcelas;

    private DispositivoAutenticacaoDTO dispositivoAutenticacao;

    @Schema(title = "Sistema de origem da conta", example = "LEGADO", implementation = OrigemEnum.class, enumAsRef = true)
    @NotNull(message = "Preenchimento do tipo de origem da conta é obrigatória.", groups = PrimaryOrder.class)
    private OrigemEnum tipoOrigemSistema;

    public static CadastroRequest criarRecorrencia(ProtocoloDTO protocolo,
                                                   List<CadastroRecorrenciaTransacaoDTO> listaParcelas,
                                                   String identificadorTransacao,
                                                   String dataHoraInicioCanal,
                                                   ZonedDateTime dataHoraRecepcao) {
        var primeiraParcela = listaParcelas.getFirst();
        var pagadorRequest = primeiraParcela.getPagador();
        var recebedorRequest = primeiraParcela.getRecebedor();
        var pagador = RecorrentePagadorDTO.from(pagadorRequest);
        var recebedor = RecorrenteRecebedorDTO.from(recebedorRequest);
        var dispositivoAutenticacao = listaParcelas.getFirst().getDispositivoAutenticacao();

        var identificadorRecorrencia = Optional.ofNullable(primeiraParcela.getIdentificadorRecorrencia())
                .orElseGet(() -> UUID.randomUUID().toString());

        var parcelas = listaParcelas.stream()
                .map(parcelaTransacao -> RecorrenteParcelaRequisicaoDTO.from(parcelaTransacao, identificadorRecorrencia))
                .toList();

        var nomeRecorrencia = Optional.ofNullable(primeiraParcela.getNome())
                .filter(StringUtils::isNotBlank)
                .orElseGet(recebedor::getNome);

        var tipoIniciacaoCanal = criarTipoIniciacaoCanal(primeiraParcela.getTipoCanal().name(), recebedor.getChave(), primeiraParcela.getTipoIniciacaoCanal());

        return CadastroRequest.builder()
                .pagador(pagador)
                .recebedor(recebedor)
                .nome(nomeRecorrencia)
                .tipoCanal(primeiraParcela.getTipoCanal())
                .tipoOrigemSistema(primeiraParcela.getOrigemConta())
                .tipoIniciacao(primeiraParcela.getTipoIniciacao())
                .tipoIniciacaoCanal(tipoIniciacaoCanal)
                .tipoFrequencia(primeiraParcela.getTipoFrequencia())
                .tipoRecorrencia(primeiraParcela.getTipoRecorrencia())
                .numInicCnpj(primeiraParcela.getNumInicCnpj())
                .parcelas(parcelas)
                .dispositivoAutenticacao(dispositivoAutenticacao)
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .dataHoraRecepcao(dataHoraRecepcao)
                .protocoloDTO(protocolo)
                .identificadorTransacao(identificadorTransacao)
                .identificadorRecorrencia(identificadorRecorrencia)
                .identificadorSimulacaoLimites(primeiraParcela.getIdentificadorSimulacaoLimite())
                .build();
    }

    public static CadastroRequest criarRecorrencia(ProtocoloDTO protocolo,
                                                   CadastroOrdemPagamentoTransacaoDTO ordemAgendamentoComRecorrencia,
                                                   String identificadorTransacao,
                                                   String dataHoraInicioCanal,
                                                   ZonedDateTime dataHoraRecepcao) {
        var tipoCanal = TipoCanalEnum.valueOf(ordemAgendamentoComRecorrencia.getCanal());
        var pagador = RecorrentePagadorDTO.from(ordemAgendamentoComRecorrencia);
        var recorrencia = ordemAgendamentoComRecorrencia.getRecorrencia();

        var identificadorRecorrencia = Optional.ofNullable(recorrencia.getIdentificadorRecorrencia())
                .orElseGet(() -> UUID.randomUUID().toString());

        var recebedor = RecorrenteRecebedorDTO.from(ordemAgendamentoComRecorrencia, recorrencia.getTipoPessoaRecebedor(), recorrencia.getTipoChaveRecebedor());
        var parcelas = recorrencia.getParcelas().stream()
                .map(parcela -> RecorrenteParcelaRequisicaoDTO.from(parcela, identificadorRecorrencia))
                .toList();

        var nomeRecorrencia = Optional.ofNullable(recorrencia.getNomeRecorrencia())
                .filter(StringUtils::isNotBlank)
                .orElseGet(recebedor::getNome);

        var tipoIniciacaoCanal = criarTipoIniciacaoCanal(ordemAgendamentoComRecorrencia.getCanal(), ordemAgendamentoComRecorrencia.getChaveDict(), recorrencia.getTipoIniciacaoCanal());

        return CadastroRequest.builder()
                .pagador(pagador)
                .recebedor(recebedor)
                .tipoCanal(tipoCanal)
                .tipoOrigemSistema(ordemAgendamentoComRecorrencia.getOrigemConta())
                .tipoIniciacao(ordemAgendamentoComRecorrencia.getTipoPagamento().getTipoPagamentoPix())
                .tipoIniciacaoCanal(tipoIniciacaoCanal)
                .numInicCnpj(ordemAgendamentoComRecorrencia.getNumInicCnpj())
                .tipoRecorrencia(TipoRecorrencia.AGENDADO_RECORRENTE)
                .dispositivoAutenticacao(ordemAgendamentoComRecorrencia.getDispositivoAutenticacao())
                .nome(nomeRecorrencia)
                .tipoFrequencia(recorrencia.getTipoFrequencia())
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .dataHoraRecepcao(dataHoraRecepcao)
                .protocoloDTO(protocolo)
                .identificadorTransacao(identificadorTransacao)
                .identificadorRecorrencia(identificadorRecorrencia)
                .parcelas(parcelas)
                .identificadorSimulacaoLimites(recorrencia.getIdentificadorSimulacaoLimite())
                .build();
    }

    public static CadastroRequest criarAgendamento(ProtocoloDTO protocolo,
                                                   CadastroOrdemPagamentoTransacaoDTO ordemAgendamento,
                                                   String identificadorTransacao,
                                                   String dataHoraInicioCanal,
                                                   ZonedDateTime dataHoraRecepcao) {
        var tipoCanal = TipoCanalEnum.valueOf(ordemAgendamento.getCanal());
        var pagador = RecorrentePagadorDTO.from(ordemAgendamento);
        var tipoPessoa = ordemAgendamento.getCpfCnpjUsuarioRecebedor().length() == 11 ? TipoPessoaEnum.PF : TipoPessoaEnum.PJ;
        var recebedor = RecorrenteRecebedorDTO.from(ordemAgendamento, tipoPessoa);
        var parcela = RecorrenteParcelaRequisicaoDTO.from(ordemAgendamento, identificadorTransacao);

        var tipoIniciacaoCanal = criarTipoIniciacaoCanal(ordemAgendamento.getCanal(), ordemAgendamento.getChaveDict(), null);

        return CadastroRequest.builder()
                .pagador(pagador)
                .recebedor(recebedor)
                .tipoCanal(tipoCanal)
                .tipoOrigemSistema(ordemAgendamento.getOrigemConta())
                .tipoIniciacao(ordemAgendamento.getTipoPagamento().getTipoPagamentoPix())
                .tipoIniciacaoCanal(tipoIniciacaoCanal)
                .numInicCnpj(ordemAgendamento.getNumInicCnpj())
                .tipoRecorrencia(TipoRecorrencia.AGENDADO)
                .dispositivoAutenticacao(ordemAgendamento.getDispositivoAutenticacao())
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .dataHoraRecepcao(dataHoraRecepcao)
                .protocoloDTO(protocolo)
                .identificadorTransacao(identificadorTransacao)
                .identificadorRecorrencia(identificadorTransacao)
                .nome(recebedor.getNome())
                .parcelas(List.of(parcela))
                .identificadorSimulacaoLimites(ordemAgendamento.getIdentificadorSimulacaoLimite())
                .build();
    }

    private static LocalDateTime criarDataHoraInicioCanal(String dataHoraInicioCanal) {
        return Optional.ofNullable(dataHoraInicioCanal)
                .map(header -> ZonedDateTime.parse(header, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);
    }

    private static TipoIniciacaoCanal criarTipoIniciacaoCanal(String canal, String chavePix, TipoIniciacaoCanal tipoIniciacaoCanal) {
        if (TipoCanalEnum.WEB_OPENBK.name().equals(canal)) {
            return TipoIniciacaoCanal.OPEN_FINANCE;
        }
        return Optional.ofNullable(tipoIniciacaoCanal)
                .orElseGet(() -> StringUtils.isEmpty(chavePix) ? TipoIniciacaoCanal.DADOS_BANCARIOS : TipoIniciacaoCanal.CHAVE);
    }

    @Override
    public String getIdentificadorTransacao() {
        return this.identificadorTransacao;
    }


}
