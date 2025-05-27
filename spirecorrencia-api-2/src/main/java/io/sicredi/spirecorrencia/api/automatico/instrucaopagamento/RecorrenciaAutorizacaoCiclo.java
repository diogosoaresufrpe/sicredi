package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCicloEnum;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "RECORRENCIA_AUTORIZACAO_CICLO",
        schema = "SPI_OWNER",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_RECAUTCICLO_OIDRECAUTDATINIDATFIM",
                columnNames = {"OID_RECORRENCIA_AUTORIZACAO", "DAT_INICIAL", "DAT_FINAL"}
        )
)
public class RecorrenciaAutorizacaoCiclo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OID_RECORRENCIA_AUTORIZACAO_CICLO", nullable = false)
    private Long oidRecorrenciaAutorizacaoCiclo;

    @OneToMany(mappedBy = "ciclo", fetch = FetchType.LAZY)
    private List<RecorrenciaInstrucaoPagamento> instrucoesPagamento;

    @Column(name = "OID_RECORRENCIA_AUTORIZACAO", nullable = false)
    private Long oidRecorrenciaAutorizacao;

    @Column(name = "TPO_STATUS", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private TipoStatusCicloEnum tipoStatus;

    @Column(name = "DAT_INICIAL", nullable = false)
    private LocalDate dataInicial;

    @Column(name = "DAT_FINAL", nullable = false)
    private LocalDate dataFinal;

    @Column(name = "DAT_CRIACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;

    public List<RecorrenciaInstrucaoPagamento> getInstrucoesPagamento() {
        if(instrucoesPagamento == null) {
            return new ArrayList<>();
        }
        return instrucoesPagamento;
    }
}
