package io.sicredi.spirecorrencia.api.cadastro;

import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.pagamento.CadastroOrdemPagamentoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.CadastroRecorrenciaTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.RecorrenciaParcelaRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.RECORRENCIA_TRANSACAO_REQUEST)
@JsonInclude(JsonInclude.Include.NON_NULL)
@GroupSequence({PrimaryOrder.class, RecorrenteParcelaRequisicaoDTO.class})
public class RecorrenteParcelaRequisicaoDTO {

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.VALOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.VALOR)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.VALOR_NOTNULL, groups = PrimaryOrder.class)
    @Digits(integer = 16, fraction = 2, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.VALOR_DIGITS, groups = PrimaryOrder.class)
    private BigDecimal valor;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.DATA_TRANSACAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.DATA_TRANSACAO)
    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.DATA_TRANSACAO_NOTNULL, groups = PrimaryOrder.class)
    private LocalDateTime dataTransacao;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.ID_FIM_A_FIM, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.ID_FIM_A_FIM)
    @Size(min = 32, max = 32, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_FIM_A_FIM_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_ID_FIM_A_FIM, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_FIM_A_FIM_PATTERN, groups = PrimaryOrder.class)
    private String idFimAFim;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.DESCRICAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.DESCRICAO)
    @Size(min = 1, max = 140, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Validations.DESCRICAO_SIZE)
    private String informacoesEntreUsuarios;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.ID_CONCILIACAO_RECEBEDOR, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.ID_CONCILIACAO_RECEBEDOR)
    @Size(min = 1, max = 35, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_CONCILIACAO_RECEBEDOR_SIZE, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_ID_CONCILIACAO_RECEBEDOR, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_CONCILIACAO_RECEBEDOR_PATTERN, groups = PrimaryOrder.class)
    private String idConciliacaoRecebedor;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.ID_PARCELA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.ID_PARCELA)
    @Size(min = 1, message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Titles.ID_PARCELA, groups = PrimaryOrder.class)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_PARCELA_NOTBLANK, groups = PrimaryOrder.class)
    private String identificadorParcela;

    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Titles.IDENTIFICADOR_RECORRENCIA, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Recorrencia.Schemas.Exemplos.IDENTIFICADOR_RECORRENCIA)
    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Validations.ID_RECORRENCIA_NOTBLANK)
    private String identificadorRecorrencia;

    public static RecorrenteParcelaRequisicaoDTO from(RecorrenciaParcelaRequest parcela, String identificadorRecorrencia) {
        return RecorrenteParcelaRequisicaoDTO.builder()
                .identificadorParcela(parcela.getIdentificadorParcela())
                .identificadorRecorrencia(identificadorRecorrencia)
                .valor(parcela.getValor())
                .dataTransacao(parcela.getDataTransacao())
                .idConciliacaoRecebedor(parcela.getIdConciliacaoRecebedor())
                .idFimAFim(parcela.getIdFimAFim())
                .informacoesEntreUsuarios(parcela.getInformacoesEntreUsuarios())
                .build();
    }

    public static RecorrenteParcelaRequisicaoDTO from(CadastroOrdemPagamentoTransacaoDTO ordemAgendamento, String identificadorRecorrencia) {
        return RecorrenteParcelaRequisicaoDTO.builder()
                .identificadorParcela(identificadorRecorrencia)
                .identificadorRecorrencia(identificadorRecorrencia)
                .valor(ordemAgendamento.getValor())
                .dataTransacao(ordemAgendamento.getDataTransacao())
                .idConciliacaoRecebedor(ordemAgendamento.getIdConciliacaoRecebedor())
                .idFimAFim(ordemAgendamento.getIdFimAFim())
                .informacoesEntreUsuarios(ordemAgendamento.getInformacoesEntreUsuarios())
                .build();
    }

    public static RecorrenteParcelaRequisicaoDTO from(CadastroRecorrenciaTransacaoDTO parcela, String identificadorRecorrencia) {
        return RecorrenteParcelaRequisicaoDTO.builder()
                .identificadorParcela(parcela.getIdentificadorParcela())
                .identificadorRecorrencia(identificadorRecorrencia)
                .valor(parcela.getValor())
                .dataTransacao(parcela.getDataTransacao())
                .idFimAFim(parcela.getIdFimAFim())
                .informacoesEntreUsuarios(parcela.getInformacoesEntreUsuarios())
                .idConciliacaoRecebedor(parcela.getIdConciliacaoRecebedor())
                .build();
    }

}
