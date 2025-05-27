package io.sicredi.spirecorrencia.api.repositorio;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.dict.ChannelDataDTO;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "RECORRENCIA_TRANSACAO", schema = "SPI_OWNER")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RecorrenciaTransacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OID_RECORRENCIA_TRANSACAO", nullable = false)
    @EqualsAndHashCode.Include
    private Long oidRecorrenciaTransacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA", nullable = false)
    private Recorrencia recorrencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_MOTIVO_EXCLUSAO")
    private TipoMotivoExclusao tipoMotivoExclusao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_STATUS", nullable = false, length = 25)
    private TipoStatusEnum tpoStatus;

    @Column(name = "ID_PARCELA")
    private String idParcela;

    @Column(name = "COD_FIM_A_FIM", length = 32)
    private String idFimAFim;

    @Column(name = "NUM_VALOR", nullable = false)
    private BigDecimal valor;

    @Column(name = "FLG_NOTIFICACAO_DIA_ANTERIOR")
    @Convert(converter = NotificacaoDiaAnteriorConverter.class)
    private Boolean notificadoDiaAnterior;

    @Column(name = "DAT_TRANSACAO", nullable = false)
    private LocalDate dataTransacao;

    @Column(name = "INFORMACOES_ENTRE_USUARIOS", length = 140)
    private String informacoesEntreUsuarios;

    @Column(name = "ID_CONCILIACAO_RECEBEDOR", length = 35)
    private String idConciliacaoRecebedor;

    @Column(name = "DAT_EXCLUSAO")
    private LocalDateTime dataExclusao;

    @Column(name = "DAT_CRIACAO_REGISTRO", updatable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;

    public ChannelDataDTO criarChannelData() {
        return ChannelDataDTO.builder()
                .oidRecorrencia(recorrencia.getOidRecorrencia())
                .oidRecorrenciaTransacao(oidRecorrenciaTransacao)
                .oidPagador(recorrencia.getPagador().getOidPagador())
                .oidRecebedor(recorrencia.getRecebedor().getOidRecebedor())
                .build();
    }

    public RecorrenciaTransacao excluir(TipoMotivoExclusao tipoMotivoExclusao) {
        this.tipoMotivoExclusao = tipoMotivoExclusao;
        this.tpoStatus = TipoStatusEnum.EXCLUIDO;
        this.dataExclusao = LocalDateTime.now();
        return this;
    }

    public String getNomeRecebedor() {
        return Optional.ofNullable(recorrencia)
                .map(Recorrencia::getRecebedor)
                .map(Recebedor::getNome)
                .orElse("");
    }

    public String getAgenciaPagador() {
        return Optional.ofNullable(recorrencia)
                .map(Recorrencia::getAgenciaPagador)
                .orElse("");
    }

    public String getContaPagador() {
        return Optional.ofNullable(recorrencia)
                .map(Recorrencia::getContaPagador)
                .orElse("");
    }

    public String getDocumentoPagador() {
        return Optional.ofNullable(recorrencia)
                .map(Recorrencia::getDocumentoPagador)
                .orElse("");
    }

    public TipoCanalEnum getTipoCanal() {
        return Optional.ofNullable(recorrencia)
                .map(Recorrencia::getTipoCanal)
                .orElse(null);
    }

}
