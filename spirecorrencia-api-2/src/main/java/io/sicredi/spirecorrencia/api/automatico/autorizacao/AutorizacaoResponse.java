package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.commons.DevedorResponse;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de todas as autorizações do Pix Automático.")
class AutorizacaoResponse {

    @Schema(title = "Identificador único da tabela de autorizações do Pix automático, valor é gerado de forma automática e sequencial por uma sequence.", example = "1")
    private Long oidRecorrenciaAutorizacao;

    @Schema(title = "Identificador da recorrência", example = "1")
    private String idRecorrencia;

    @Schema(title = "Razão social ou nome fantasia do recebedor, que está sendo autorizada a enviar as cobranças relacionadas à recorrência.", example = "Energia SA")
    private String nomeRecebedor;

    @Schema(title = "Data de criação da recorrência.", example = "2025-05-10 12:15:46")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataCriacao;

    @Schema(title = "Número, identificador, ou código que representa o objeto da autorização (contrato, pedido etc.).", example = "574554573-3")
    private String contrato;

    @Schema(title = "Informações referentes ao contrato que permitam ao pagador reconhecer o pagamento periódico por meio do Pix Automático.", example = "Plano Academia")
    private String descricao;

    @Schema(title = "Tipo de status que a autorização se encontra atualmente.", example = "CRIADA, APROVADA, REJEITADA, CANCELADA, EXPIRADA")
    private String tpoStatus;

    @Schema(title = "Tipo de sub status que a autorização se encontra atualmente.", example = "AGUARDANDO_ENVIO, AGUARDANDO_RETORNO, AGUARDANDO_CANCELAMENTO")
    private String tpoSubStatus;

    @Schema(title = "Status da Recorrência")
    private String tpoJornada;

    @Schema(title = "Tipo de frequencia", example = "SEMANAL")
    private String tpoFrequencia;

    @Schema(title = "Valor do pagamento", example = "100")
    private BigDecimal valor;

    @Schema(title = "Piso do valor máximo do pagamento", example = "100")
    private BigDecimal pisoValorMaximo;

    @Schema(title = "Valor máximo do pagamento", example = "100")
    private BigDecimal valorMaximo;

    @Schema(title = "Permite linha de crédito")
    private Boolean permiteLinhaCredito;

    @Schema(title = "Permite que angendamento seja notificado", example = "100")
    private Boolean permiteNotificacaoAgendamento;

    @Schema(title = "Permite uma nova tentativa", example = "100")
    private Boolean permiteRetentativa;

    @Schema(title = "Data prevista para o primeiro pagamento", example = "2025-05-13")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataInicialRecorrencia;

    @Schema(title = "Data prevista para o último pagamento", example = "2025-05-13")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataFinalRecorrencia;

    @Schema(title = "Data de autorização pelo usuário pagador", example = "2025-05-10 12:15:46")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataConfirmacao;

    @Schema(title = "Data de cancelamento da autorização", example = "2025-05-10 12:15:46")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataCancelamento;

    @Schema(title = "Codigo do motivo do cancelamento")
    private String codigoMotivoCancelamento;

    @Schema(title = "PSP Solicitante do cancelamento", example = "PAGADOR", implementation = TipoSolicitanteCancelamento.class, enumAsRef = true)
    private TipoSolicitanteCancelamento solicitanteCancelamento;

    @Schema(title = "Dados do recebedor da recorrência")
    private RecebedorResponse recebedor;

    @Schema(title = "Dados do devedor da recorrência")
    private DevedorResponse devedor;

    @Schema(title = "Dados do pagador da recorrência")
    private PagadorResponse pagador;

    static AutorizacaoResponse fromListagem(final RecorrenciaAutorizacao autorizacao) {
        return AutorizacaoResponse.builder()
                .oidRecorrenciaAutorizacao(autorizacao.getOidRecorrenciaAutorizacao())
                .nomeRecebedor(autorizacao.getNomeRecebedor())
                .dataCriacao(autorizacao.getDataCriacaoRecorrencia())
                .contrato(autorizacao.getNumeroContrato())
                .descricao(autorizacao.getDescricao())
                .tpoStatus(autorizacao.getTipoStatus().toString())
                .tpoSubStatus(autorizacao.getTipoSubStatus())
                .build();
    }

    static AutorizacaoResponse fromDetalhes(final RecorrenciaAutorizacao autorizacao, RecorrenciaAutorizacaoCancelamento cancelamento) {
        var pagadorResponse = PagadorResponse.from(
                autorizacao.getNomePagador(),
                autorizacao.getCpfCnpjPagador()
        );
        var recebedorResponse = RecebedorResponse.from(
                autorizacao.getNomeRecebedor(),
                autorizacao.getCpfCnpjRecebedor()
        );

        var devedorResponse = Optional.of(autorizacao)
                .filter(recorrenciaAutorizacao -> StringUtils.isNotBlank(recorrenciaAutorizacao.getNomeDevedor())
                                                  && StringUtils.isNotBlank(recorrenciaAutorizacao.getCpfCnpjDevedor()))
                .map(solicitacao -> DevedorResponse.from(
                        solicitacao.getNomeDevedor(),
                        solicitacao.getCpfCnpjDevedor()))
                .orElseGet(() -> DevedorResponse.from(
                        autorizacao.getNomePagador(),
                        autorizacao.getCpfCnpjPagador()));

        var builder = AutorizacaoResponse.builder()
                .oidRecorrenciaAutorizacao(autorizacao.getOidRecorrenciaAutorizacao())
                .idRecorrencia(autorizacao.getIdRecorrencia())
                .nomeRecebedor(autorizacao.getNomeRecebedor())
                .dataCriacao(autorizacao.getDataCriacaoRecorrencia())
                .contrato(autorizacao.getNumeroContrato())
                .descricao(autorizacao.getDescricao())
                .tpoStatus(autorizacao.getTipoStatus().toString())
                .tpoSubStatus(autorizacao.getTipoSubStatus())
                .tpoJornada(autorizacao.getTipoJornada())
                .tpoFrequencia(autorizacao.getTipoFrequencia())
                .valor(autorizacao.getValor())
                .pisoValorMaximo(autorizacao.getPisoValorMaximo())
                .valorMaximo(autorizacao.getValorMaximo())
                .permiteLinhaCredito(getBooleanFromString(autorizacao.getPermiteLinhaCredito()))
                .permiteNotificacaoAgendamento(getBooleanFromString(autorizacao.getPermiteNotificacaoAgendamento()))
                .permiteRetentativa(getBooleanFromString(autorizacao.getPermiteRetentativa()))
                .contrato(autorizacao.getNumeroContrato())
                .descricao(autorizacao.getDescricao())
                .recebedor(recebedorResponse)
                .devedor(devedorResponse)
                .pagador(pagadorResponse)
                .dataInicialRecorrencia(autorizacao.getDataInicialRecorrencia())
                .dataFinalRecorrencia(autorizacao.getDataFinalRecorrencia())
                .dataConfirmacao(autorizacao.getDataInicioConfirmacao());

        Optional.ofNullable(cancelamento).ifPresent(c -> builder
                .dataCancelamento(c.getDataCancelamento())
                .codigoMotivoCancelamento(c.getMotivoCancelamento())
                .solicitanteCancelamento(c.getTipoSolicitanteCancelamento()));

        return builder.build();
    }

    private static boolean getBooleanFromString(final String s) {
        return "S".equals(s);
    }
}