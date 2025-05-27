package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.commons.PagadorResponse;
import io.sicredi.spirecorrencia.api.commons.RecebedorResponse;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Consulta de todas recorrências Pix")
class RecorrenciaResponse {

    @Schema(title = "Identificador da Recorrência", example = "2f335153-4d6a-4af1-92a0-c52c5c827af9")
    private String identificadorRecorrencia;

    @Schema(title = "Tipo de recorrência")
    private TipoRecorrencia tipoRecorrencia;

    @Schema(title = "Nome da recorrência", example = "Recorrência Academia")
    private String nome;

    @Schema(title = "Tipo de frequencia", example = "SEMANAL")
    private TipoFrequencia tipoFrequencia;

    @Schema(title = "Tipo do Canal", example = "MOBI")
    private TipoCanalEnum tipoCanal;

    @Schema(title = "Tipo da Marca", example = "SICREDI")
    private TipoMarcaEnum tipoMarca;

    @Schema(title = "Tipo de Iniciação", example = "PIX_PAYMENT_BY_KEY")
    private TipoPagamentoPixEnum tipoIniciacao;

    @Schema(title = "Status da Recorrência", example = "EXCLUIDO")
    private TipoStatusEnum tipoStatus;

    @Schema(title = "Valor do próximo pagamento", example = "100")
    private BigDecimal valorProximoPagamento;

    @Schema(title = "Número total de parcelas", example = "10")
    private Long numeroTotalParcelas;

    @Schema(title = "Data de criação", example = "2022-05-01 23:22:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataCriacao;

    @Schema(title = "Data de exclusão", example = "2022-06-02 23:22:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataExclusao;

    @Schema(title = "Data do último pagamento concluído", example = "2022-07-03")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataUltimoPagamentoConcluido;

    @Schema(title = "Data do próximo pagamento", example = "2022-08-03")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataProximoPagamento;

    @Schema(title = "Data da primeira parcela", example = "2022-08-03")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataPrimeiraParcela;

    @Schema(title = "Data da ultima parcela", example = "2022-08-03")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataUltimaParcela;

    @Schema(title = "Dados do recebedor")
    private RecebedorResponse recebedor;

    @Schema(title = "Dados do pagador")
    private PagadorResponse pagador;

    @Schema(title = "Parcelas da recorrência")
    private List<ParcelaRecorrenciaResponse> parcelas;


    static RecorrenciaResponse fromListagemRecorrencia(Recorrencia recorrencia) {
        return Optional.ofNullable(recorrencia)
                .map(recorrenciaFilter -> {
                    var recebedor = Optional.ofNullable(recorrenciaFilter.getRecebedor())
                            .map(x -> RecebedorResponse.fromNomeRecebedor(x.getNome()))
                            .orElse(null);

                    var parcelas = ParcelaRecorrenciaResponse.gerarParcelas(recorrenciaFilter.getRecorrencias());
                    return RecorrenciaResponse.builder()
                                .identificadorRecorrencia(recorrenciaFilter.getIdRecorrencia())
                                .tipoRecorrencia(recorrenciaFilter.getTipoRecorrencia())
                                .nome(recorrenciaFilter.getNome())
                                .tipoStatus(recorrenciaFilter.getTipoStatus())
                                .valorProximoPagamento(ParcelaRecorrenciaResponse.obterValorProximoPagamento(parcelas))
                                .dataCriacao(recorrenciaFilter.getDataCriacao())
                                .dataUltimoPagamentoConcluido(ParcelaRecorrenciaResponse.obterDataUltimoPagamentoConcluido(parcelas))
                                .dataProximoPagamento(ParcelaRecorrenciaResponse.obterDataProximoPagamento(parcelas))
                                .recebedor(recebedor)
                                .build();
                        }
                ).orElse(null);

    }

    static RecorrenciaResponse fromDetalhesRecorrencia(Recorrencia recorrencia) {
        return Optional.ofNullable(recorrencia)
                .map(recorrenciaFilter -> {
                        var parcelas = ParcelaRecorrenciaResponse.gerarParcelas(recorrenciaFilter.getRecorrencias());
                        var dataPrimeiraParcela = CollectionUtils.isEmpty(parcelas) ? null : parcelas.getFirst().getDataTransacao();
                        var dataUltimaParcela = CollectionUtils.isEmpty(parcelas) ? null : parcelas.getLast().getDataTransacao();
                        return RecorrenciaResponse.builder()
                                .identificadorRecorrencia(recorrenciaFilter.getIdRecorrencia())
                                .tipoFrequencia(recorrenciaFilter.getTipoFrequencia())
                                .tipoRecorrencia(recorrenciaFilter.getTipoRecorrencia())
                                .tipoIniciacao(recorrenciaFilter.getTipoIniciacao())
                                .nome(recorrenciaFilter.getNome())
                                .tipoStatus(recorrenciaFilter.getTipoStatus())
                                .valorProximoPagamento(ParcelaRecorrenciaResponse.obterValorProximoPagamento(parcelas))
                                .numeroTotalParcelas(ParcelaRecorrenciaResponse.obterNumeroTotalParcelas(parcelas))
                                .dataCriacao(recorrenciaFilter.getDataCriacao())
                                .dataExclusao(recorrenciaFilter.getDataExclusao())
                                .dataProximoPagamento(ParcelaRecorrenciaResponse.obterDataProximoPagamento(parcelas))
                                .dataPrimeiraParcela(dataPrimeiraParcela)
                                .dataUltimaParcela(dataUltimaParcela)
                                .recebedor(RecebedorResponse.fromDetalhesRecorrencia(recorrenciaFilter.getRecebedor()))
                                .pagador(PagadorResponse.fromDetalhesParcelas(recorrenciaFilter.getPagador()))
                                .parcelas(parcelas)
                                .build();
                        }
                ).orElse(null);
    }

}
