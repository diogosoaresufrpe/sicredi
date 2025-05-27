package io.sicredi.spirecorrencia.api.recorrencia_tentativa;

import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "RECORRENCIA_TRANSACAO_TENT", schema = "SPI_OWNER")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecorrenciaTransacaoTentativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OID_RECORRENCIA_TRANSACAO_TENT", nullable = false)
    @EqualsAndHashCode.Include
    private Long oidRecorrenciaTransacaoTentativa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA_TRANSACAO", nullable = false)
    private RecorrenciaTransacao recorrenciaTransacao;

    @Column(name = "COD_FIM_A_FIM", nullable = false, length = 32)
    private String idFimAFim;

    @Column(name = "MOTIVO")
    private String motivo;

    @Column(name = "CODIGO")
    private String codigo;

    @Column(name = "DAT_CRIACAO_REGISTRO", updatable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;
}
