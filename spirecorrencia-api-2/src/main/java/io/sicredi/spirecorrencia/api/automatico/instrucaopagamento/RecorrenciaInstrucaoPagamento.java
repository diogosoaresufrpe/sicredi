package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;

import io.sicredi.spirecorrencia.api.automatico.autorizacao.RecorrenciaAutorizacao;
import jakarta.persistence.*;
import jakarta.validation.constraints.Digits;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "RECORRENCIA_INSTRUCAO_PAGAMENTO", schema = "SPI_OWNER")
@Builder
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class RecorrenciaInstrucaoPagamento {

    @Id
    @Column(name = "COD_FIM_A_FIM", nullable = false, length = 32)
    private String codFimAFim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA_AUTORIZACAO", referencedColumnName = "OID_RECORRENCIA_AUTORIZACAO")
    private RecorrenciaAutorizacao autorizacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OID_RECORRENCIA_AUTORIZACAO_CICLO", referencedColumnName = "OID_RECORRENCIA_AUTORIZACAO_CICLO")
    private RecorrenciaAutorizacaoCiclo ciclo;

    @Column(name = "ID_RECORRENCIA", nullable = false, length = 29)
    private String idRecorrencia;

    @Column(name = "NUM_CPF_CNPJ_PAGADOR", nullable = false, length = 14)
    private String numCpfCnpjPagador;

    @Column(name = "NUM_INSTITUICAO_PAGADOR", nullable = false, length = 8)
    private String numInstituicaoPagador;

    @Column(name = "TXT_NOME_DEVEDOR", length = 140)
    private String txtNomeDevedor;

    @Column(name = "NUM_CPF_CNPJ_DEVEDOR", length = 14)
    private String numCpfCnpjDevedor;

    @Column(name = "NUM_VALOR", nullable = false)
    @Digits(integer = 18, fraction = 2)
    private BigDecimal numValor;

    @Column(name = "NUM_CPF_CNPJ_RECEBEDOR", nullable = false, length = 14)
    private String numCpfCnpjRecebedor;

    @Column(name = "NUM_AGENCIA_RECEBEDOR", length = 4)
    private String numAgenciaRecebedor;

    @Column(name = "NUM_CONTA_RECEBEDOR", nullable = false, length = 20)
    private String numContaRecebedor;

    @Column(name = "NUM_INSTITUICAO_RECEBEDOR", nullable = false, length = 8)
    private String numInstituicaoRecebedor;

    @Column(name = "TPO_CONTA_RECEBEDOR", nullable = false, length = 25)
    private String tpoContaRecebedor;

    @Column(name = "ID_CONCILIACAO_RECEBEDOR", nullable = false, length = 35)
    private String idConciliacaoRecebedor;

    @Column(name = "TPO_STATUS", nullable = false, length = 25)
    private String tpoStatus;

    @Column(name = "TPO_SUB_STATUS", length = 25)
    private String tpoSubStatus;

    @Column(name = "TPO_FINALIDADE_AGENDAMENTO", nullable = false, length = 25)
    private String tpoFinalidadeAgendamento;

    @Column(name = "TXT_INFORMACOES_ENTRE_USUARIOS", length = 140)
    private String txtInformacoesEntreUsuarios;

    @Column(name = "COD_MOTIVO_REJEICAO", length = 255)
    private String codMotivoRejeicao;

    @Column(name = "DAT_VENCIMENTO", nullable = false)
    private LocalDate datVencimento;

    @Column(name = "DAT_CONFIRMACAO")
    private LocalDateTime datConfirmacao;

    @Column(name = "DAT_EMISSAO")
    private LocalDateTime datEmissao;

    @Column(name = "DAT_CONCLUIDO")
    private LocalDateTime datConcluido;

    @Column(name = "DAT_CRIACAO_REGISTRO", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @CreationTimestamp
    private LocalDateTime datCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @UpdateTimestamp
    private LocalDateTime datAlteracaoRegistro;
}
