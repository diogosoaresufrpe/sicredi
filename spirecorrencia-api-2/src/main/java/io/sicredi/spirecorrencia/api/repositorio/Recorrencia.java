package io.sicredi.spirecorrencia.api.repositorio;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "RECORRENCIA", schema = "SPI_OWNER")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class Recorrencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "OID_RECORRENCIA", nullable = false)
    private Long oidRecorrencia;

    @Column(name = "ID_RECORRENCIA", nullable = false, length = 36)
    private String idRecorrencia;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA_PAGADOR", referencedColumnName = "OID_RECORRENCIA_PAGADOR")
    private Pagador pagador;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA_RECEBEDOR", referencedColumnName = "OID_RECORRENCIA_RECEBEDOR")
    private Recebedor recebedor;

    @Column(name = "NOME", nullable = false, length = 30)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_CANAL", nullable = false, length = 25)
    private TipoCanalEnum tipoCanal;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_ORIGEM_SISTEMA", nullable = false, length = 8)
    private OrigemEnum tipoOrigemSistema;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_INICIACAO", nullable = false, length = 30)
    private TipoPagamentoPixEnum tipoIniciacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_STATUS", nullable = false, length = 25)
    private TipoStatusEnum tipoStatus;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "recorrencia", fetch = FetchType.LAZY)
    private List<RecorrenciaTransacao> recorrencias;

    @Column(name = "DAT_CRIACAO", nullable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "DAT_EXCLUSAO")
    private LocalDateTime dataExclusao;

    @Column(name = "DAT_CRIACAO_REGISTRO", updatable = false)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_INICIACAO_CANAL", nullable = false, length = 12)
    private TipoIniciacaoCanal tipoIniciacaoCanal;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_FREQUENCIA", length = 13)
    private TipoFrequencia tipoFrequencia;

    @Column(name = "NUM_INIC_CNPJ", length = 14)
    private String numInicCnpj;

    @Enumerated(EnumType.STRING)
    @Column(name = "TPO_RECORRENCIA")
    private TipoRecorrencia tipoRecorrencia;

    public Optional<TipoStatusEnum> criarStatusFinalizacao(List<RecorrenciaTransacao> listRecorrenciaTransacao) {
        var atualizarStatus = listRecorrenciaTransacao.stream().anyMatch(recorrenciaTransacao -> TipoStatusEnum.CRIADO == recorrenciaTransacao.getTpoStatus() || TipoStatusEnum.PENDENTE == recorrenciaTransacao.getTpoStatus());
        if (atualizarStatus) {
            return Optional.empty();
        }
        var statusConcluido = listRecorrenciaTransacao.stream().anyMatch(recorrenciaTransacao -> TipoStatusEnum.CONCLUIDO == recorrenciaTransacao.getTpoStatus());
        if (statusConcluido) {
            return Optional.of(TipoStatusEnum.CONCLUIDO);
        }
        return Optional.of(TipoStatusEnum.EXCLUIDO);
    }

    public String getAgenciaPagador() {
        return Optional.ofNullable(pagador)
                .map(Pagador::getAgencia)
                .orElse("");
    }

    public String getContaPagador() {
        return Optional.ofNullable(pagador)
                .map(Pagador::getConta)
                .orElse("");
    }

    public String getDocumentoPagador() {
        return Optional.ofNullable(pagador)
                .map(Pagador::getCpfCnpj)
                .orElse("");
    }

}
