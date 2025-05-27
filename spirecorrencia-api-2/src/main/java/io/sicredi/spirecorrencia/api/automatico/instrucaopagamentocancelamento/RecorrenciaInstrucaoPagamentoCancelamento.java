package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "RECORRENCIA_INSTRUCAO_PAGAMENTO_CANCELAMENTO", schema = "SPI_OWNER")
@SuperBuilder
@Getter
@Setter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecorrenciaInstrucaoPagamentoCancelamento {
    @Id
    @Column(name = "ID_CANCELAMENTO_AGENDAMENTO", nullable = false, length = 32)
    private String idCancelamentoAgendamento;

    @Column(name = "COD_FIM_A_FIM", nullable = false, length = 36)
    private String codFimAFim;

    @Column(name = "TPO_PSP_SOLICITANTE_CANCELAMENTO", nullable = false, length = 30)
    private String tpoPspSolicitanteCancelamento;

    @Column(name = "TPO_STATUS", nullable = false, length = 9)
    private String tpoStatus;

    @Column(name = "NUM_CPF_CNPJ_SOLICITANTE_CANCELAMENTO", nullable = false, length = 14)
    private String numCpfCnpjSolicitanteCancelamento;

    @Column(name = "COD_MOTIVO_CANCELAMENTO", nullable = false)
    private String codMotivoCancelamento;

    @Column(name = "COD_MOTIVO_REJEICAO")
    private String codMotivoRejeicao;

    @Column(name = "DAT_CRIACAO_SOLICITACAO_CANCELAMENTO", nullable = false)
    private LocalDateTime datCriacaoSolicitacaoCancelamento;

    @Column(name = "DAT_ANALISADO_SOLICITACAO_CANCELAMENTO")
    private LocalDateTime datAnalisadoSolicitacaoCancelamento;

    @Column(name = "DAT_CRIACAO_REGISTRO", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime datCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime datAlteracaoRegistro;

    @ManyToOne
    @JoinColumn(name = "COD_FIM_A_FIM", referencedColumnName = "COD_FIM_A_FIM", insertable = false, updatable = false)
    private RecorrenciaInstrucaoPagamento recorrenciaInstrucaoPagamento;
}
