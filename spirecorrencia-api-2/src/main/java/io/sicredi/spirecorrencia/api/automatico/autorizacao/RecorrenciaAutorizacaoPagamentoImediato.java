package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RECORRENCIA_AUTORIZACAO_PAGAMENTO_IMEDIATO", schema = "SPI_OWNER")
class RecorrenciaAutorizacaoPagamentoImediato {

    @Id
    @Column(name = "COD_FIM_A_FIM", nullable = false, length = 32)
    private String idFimAFim;

    @Column(name = "ID_RECORRENCIA", nullable = false, length = 29)
    private String idRecorrencia;

    @Column(name = "DAT_RECEBIMENTO_CONFIRMACAO", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataRecebimentoConfirmacao;

    @Column(name = "DAT_CRIACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;
}

