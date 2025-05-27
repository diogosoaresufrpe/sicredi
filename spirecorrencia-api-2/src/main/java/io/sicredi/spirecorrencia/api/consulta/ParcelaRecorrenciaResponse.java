package io.sicredi.spirecorrencia.api.consulta;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@Schema(title = "Response com detalhes da parcela da recorrência Pix")
class ParcelaRecorrenciaResponse {

    @Schema(title = "ID da Parcela da Recorrência")
    private String identificadorParcela;

    @Schema(title = "Identificador da Transação ( EndToEnd )", example = "E91586982202208151245099rD6AIAa7")
    private String idFimAFim;

    @Schema(title = "Valor da parcela", example = "89.90")
    private BigDecimal valor;

    @Schema(title = "Número da parcela", example = "1")
    @Setter
    private Long numeroParcela;

    @Schema(title = "Informações entre os usuários", example = "Academia")
    private String descricao;

    @Schema(title = "Status da parcela", example = "CRIADO")
    private TipoStatusEnum status;

    @Schema(title = "Data para efetivação da parcela", example = "2025-10-10")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataTransacao;

    @Schema(title = "Data de exclusão da parcela", example = "2025-09-10 12:22:21")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataExclusao;

    @Schema(title = "Campo livre para descrição disponibilizada para visualização entre os usuários referentes na parcela da recorrência.", example = "Informações complementares")
    private String informacoesEntreUsuarios;

    @Schema(title = "Identificador de conciliação para o recebedor da parcela da recorrência.", example = "Identificador")
    private String idConciliacaoRecebedor;

    @Schema(title = "Data da criação do registro da parcela.", example = "2025-10-10 12:22:21")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dataCriacaoRegistro;

    public static List<ParcelaRecorrenciaResponse> gerarParcelas(List<RecorrenciaTransacao> listaRecorrenciaTransacao) {
        AtomicLong numeroParcela = new AtomicLong(1);
        return Optional.ofNullable(listaRecorrenciaTransacao)
                .orElseGet(List::of).stream()
                .sorted(Comparator.comparing(RecorrenciaTransacao::getDataTransacao))
                .map(parcela -> ParcelaRecorrenciaResponse.builder()
                        .identificadorParcela(parcela.getIdParcela())
                        .idFimAFim(parcela.getIdFimAFim())
                        .status(parcela.getTpoStatus())
                        .valor(parcela.getValor())
                        .numeroParcela(numeroParcela.getAndIncrement())
                        .dataTransacao(parcela.getDataTransacao())
                        .dataExclusao(parcela.getDataExclusao())
                        .build()
                ).toList();
    }

    public static List<ParcelaRecorrenciaResponse> gerarDetalhesParcelas(List<RecorrenciaTransacao> listaRecorrenciaTransacao) {
        AtomicLong numeroParcela = new AtomicLong(1);
        return Optional.ofNullable(listaRecorrenciaTransacao)
                .orElseGet(List::of).stream()
                .sorted(Comparator.comparing(RecorrenciaTransacao::getDataTransacao))
                .map(parcela -> ParcelaRecorrenciaResponse.builder()
                        .identificadorParcela(parcela.getIdParcela())
                        .status(parcela.getTpoStatus())
                        .numeroParcela(numeroParcela.getAndIncrement())
                        .dataTransacao(parcela.getDataTransacao())
                        .valor(parcela.getValor())
                        .idFimAFim(parcela.getIdFimAFim())
                        .informacoesEntreUsuarios(parcela.getInformacoesEntreUsuarios())
                        .idConciliacaoRecebedor(parcela.getIdConciliacaoRecebedor())
                        .dataExclusao(parcela.getDataExclusao())
                        .dataCriacaoRegistro(parcela.getDataCriacaoRegistro())
                        .build()
                ).toList();
    }

    public static Long obterNumeroTotalParcelas(List<ParcelaRecorrenciaResponse> parcelas) {
        return (long) Optional.ofNullable(parcelas)
                .orElseGet(List::of).size();
    }

    public static LocalDate obterDataProximoPagamento(List<ParcelaRecorrenciaResponse> parcelas) {
        return Optional.ofNullable(parcelas)
                .orElseGet(List::of).stream()
                .filter(recorrenciaTransacao -> List.of(TipoStatusEnum.CRIADO, TipoStatusEnum.PENDENTE)
                        .contains(recorrenciaTransacao.getStatus()))
                .map(ParcelaRecorrenciaResponse::getDataTransacao)
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    public static LocalDate obterDataUltimoPagamentoConcluido(List<ParcelaRecorrenciaResponse> parcelas) {
        return Optional.ofNullable(parcelas)
                .orElseGet(List::of).stream()
                .filter(recorrenciaTransacao -> TipoStatusEnum.CONCLUIDO == recorrenciaTransacao.getStatus())
                .map(ParcelaRecorrenciaResponse::getDataTransacao)
                .max(LocalDate::compareTo)
                .orElse(null);
    }

    public static BigDecimal obterValorProximoPagamento(List<ParcelaRecorrenciaResponse> parcelas) {
        return Optional.ofNullable(parcelas)
                .orElseGet(List::of).stream()
                .findFirst()
                .map(ParcelaRecorrenciaResponse::getValor)
                .orElse(null);
    }

}
