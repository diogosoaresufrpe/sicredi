package io.sicredi.spirecorrencia.api.automatico.solicitacao;

import br.com.sicredi.spi.entities.type.TipoFrequencia;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrencia;
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
@Schema(title = "Consulta de todas as solicitações de autorização do Pix Automático.")
class SolicitacaoAutorizacaoRecorrenciaResponse {

    @Schema(title = "Identificador único da tabela de autorizações do Pix Automático.", example = "SC0118152120250425041bYqAj6ef")
    private String idSolicitacaoRecorrencia;

    @Schema(title = "Identificador único da tabela de recorrência do Pix Automático.", example = "SC0118152120250425041bYqAj6ef")
    private String idRecorrencia;

    @Schema(title = "Razão social ou nome fantasia do recebedor, que está sendo autorizada a enviar as cobranças relacionadas à recorrência.", example = "Energia SA")
    private String nomeRecebedor;

    @Schema(title = "Data de expiração da solicitação de recorrência.", example = "2025-05-10T12:15:46")
    private LocalDateTime dataExpiracao;

    @Schema(title = "Data de criação da recorrência.", example = "2025-05-10T12:15:46")
    private LocalDateTime dataCriacao;

    @Schema(title = "Número, identificador, ou código que representa o objeto da autorização (contrato, pedido etc.).", example = "574554573-3")
    private String contrato;

    @Schema(title = "Informações referentes ao contrato que permitam ao pagador reconhecer o pagamento periódico por meio do Pix Automático.", example = "Plano Academia")
    private String descricao;

    @Schema(title = "Tipo de status que a autorização se encontra atualmente.", example = "CRIADA, PENDENTE_CONFIRMACAO, CONFIRMADA, ACEITA, REJEITADA, CANCELADA, EXPIRADA")
    private String tpoStatus;

    @Schema(title = "Tipo de sub status que a autorização se encontra atualmente.", example = "AGUARDANDO_ENVIO, AGUARDANDO_RETORNO")
    private String tpoSubStatus;

    @Schema(title = "Tipo de frequencia da recorrência", example="SEMESTRAL", implementation = TipoFrequencia.class, enumAsRef = true)
    private TipoFrequencia tpoFrequencia;

    @Schema(title = "Valor autorizado para recorrência.", example = "00.00,100.00")
    private BigDecimal valor;

    @Schema(title="Piso do valor máximo autorizado para recorrência")
    private BigDecimal pisoValorMaximo;

    @Schema(title = "Data prevista para o primeiro pagamento", example = "07/05/2025")
    private LocalDate dataInicialRecorrencia;

    @Schema(title = "Data prevista para o último pagamento", example = "07/05/2025")
    private LocalDate dataFinalRecorrencia;

    @Schema(title="Recebedor da recorrência")
    private RecebedorResponse recebedor;

    @Schema(title="Devedor da recorrência")
    private DevedorResponse devedor;

    @Schema(title="Pagador da recorrência")
    private PagadorResponse pagador;

    static SolicitacaoAutorizacaoRecorrenciaResponse fromListagem(final SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacao) {
        return SolicitacaoAutorizacaoRecorrenciaResponse.builder()
                .idSolicitacaoRecorrencia(solicitacaoAutorizacao.getIdSolicitacaoRecorrencia())
                .nomeRecebedor(solicitacaoAutorizacao.getNomeRecebedor())
                .dataExpiracao(solicitacaoAutorizacao.getDataExpiracaoConfirmacaoSolicitacao())
                .dataCriacao(solicitacaoAutorizacao.getDataCriacaoRecorrencia())
                .contrato(solicitacaoAutorizacao.getNumeroContrato())
                .descricao(solicitacaoAutorizacao.getDescricao())
                .tpoStatus(solicitacaoAutorizacao.getTipoStatus().toString())
                .tpoSubStatus(solicitacaoAutorizacao.getTipoSubStatus())
                .build();
    }


    static SolicitacaoAutorizacaoRecorrenciaResponse fromDetalheSolicitacao(final SolicitacaoAutorizacaoRecorrencia solicitacaoAutorizacao) {
        var pagadorResponse = PagadorResponse.from(
                solicitacaoAutorizacao.getNomePagador(),
                solicitacaoAutorizacao.getCpfCnpjPagador()
        );
        var recebedorResponse = RecebedorResponse.from(
            solicitacaoAutorizacao.getNomeRecebedor(),
            solicitacaoAutorizacao.getCpfCnpjRecebedor()
        );

        var devedorResponse = Optional.of(solicitacaoAutorizacao)
                .filter(solicitacao -> StringUtils.isNotBlank(solicitacao.getNomeDevedor())
                                       && StringUtils.isNotBlank(solicitacao.getCpfCnpjDevedor()))
                .map(solicitacao -> DevedorResponse.from(
                        solicitacao.getNomeDevedor(),
                        solicitacao.getCpfCnpjDevedor()))
                .orElseGet(() -> DevedorResponse.from(
                        solicitacaoAutorizacao.getNomePagador(),
                        solicitacaoAutorizacao.getCpfCnpjPagador()));

        return  SolicitacaoAutorizacaoRecorrenciaResponse.builder()
                .idSolicitacaoRecorrencia(solicitacaoAutorizacao.getIdSolicitacaoRecorrencia())
                .idRecorrencia(solicitacaoAutorizacao.getIdRecorrencia())
                .tpoStatus(solicitacaoAutorizacao.getTipoStatus().toString())
                .tpoSubStatus(solicitacaoAutorizacao.getTipoSubStatus())
                .tpoFrequencia(TipoFrequencia.of(solicitacaoAutorizacao.getTipoFrequencia()))
                .valor(solicitacaoAutorizacao.getValor())
                .pisoValorMaximo(solicitacaoAutorizacao.getPisoValorMaximo())
                .dataInicialRecorrencia(solicitacaoAutorizacao.getDataInicialRecorrencia())
                .dataFinalRecorrencia(solicitacaoAutorizacao.getDataFinalRecorrencia())
                .contrato(solicitacaoAutorizacao.getNumeroContrato())
                .descricao(solicitacaoAutorizacao.getDescricao())
                .recebedor(recebedorResponse)
                .devedor(devedorResponse)
                .pagador(pagadorResponse)
                .build();
    }
}