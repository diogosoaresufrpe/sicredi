package io.sicredi.spirecorrencia.api.automatico;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPessoaEnum;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode
@Entity
@Getter
@Builder
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RECORRENCIA_AUTORIZACAO_SOLICITACAO", schema = "SPI_OWNER")
public class SolicitacaoAutorizacaoRecorrencia {

    @Id
    @Column(name = "ID_SOLICITACAO_RECORRENCIA", nullable = false, length = 29)
    private String idSolicitacaoRecorrencia;

    @Column(name = "ID_RECORRENCIA", nullable = false, length = 29)
    private String idRecorrencia;

    @Column(name = "ID_INFORMACAO_STATUS", nullable = false, length = 29)
    private String idInformacaoStatus;

    @Column(name = "TPO_STATUS", nullable = false, length = 25)
    @Enumerated(EnumType.STRING)
    private TipoStatusSolicitacaoAutorizacao tipoStatus;

    @Column(name = "TPO_SUB_STATUS", length = 25)
    private String tipoSubStatus;

    @Column(name = "TPO_FREQUENCIA", nullable = false, length = 13)
    private String tipoFrequencia;

    @Column(name = "NUM_VALOR", precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(name = "NUM_PISO_VALOR_MAXIMO", precision = 18, scale = 2)
    private BigDecimal pisoValorMaximo;

    @Column(name = "NUM_CPF_CNPJ_PAGADOR", nullable = false, length = 14)
    private String cpfCnpjPagador;

    @Column(name = "TXT_NOME_PAGADOR", nullable = false, length = 140)
    private String nomePagador;

    @Column(name = "NUM_AGENCIA_PAGADOR", nullable = false, length = 4)
    private String agenciaPagador;

    @Column(name = "NUM_CONTA_PAGADOR", nullable = false, length = 20)
    private String contaPagador;

    @Column(name = "NUM_INSTITUICAO_PAGADOR", nullable = false, length = 8)
    private String instituicaoPagador;

    @Column(name = "COD_POSTO_PAGADOR", length = 4)
    private String postoPagador;

    @Column(name = "TPO_CONTA_PAGADOR", length = 25)
    private String tipoContaPagador;

    @Column(name = "TPO_PESSOA_PAGADOR", nullable = false, length = 2)
    @Enumerated(EnumType.STRING)
    private TipoPessoaEnum tipoPessoaPagador;

    @Column(name = "TPO_SISTEMA_PAGADOR", length = 8)
    @Enumerated(EnumType.STRING)
    private OrigemEnum tipoSistemaPagador;

    @Column(name = "TXT_NOME_RECEBEDOR", nullable = false, length = 140)
    private String nomeRecebedor;

    @Column(name = "NUM_CPF_CNPJ_RECEBEDOR", nullable = false, length = 14)
    private String cpfCnpjRecebedor;

    @Column(name = "NUM_INSTITUICAO_RECEBEDOR", nullable = false, length = 8)
    private String instituicaoRecebedor;

    @Column(name = "TXT_NOME_DEVEDOR", length = 140)
    private String nomeDevedor;

    @Column(name = "NUM_CPF_CNPJ_DEVEDOR", length = 14)
    private String cpfCnpjDevedor;

    @Column(name = "NUM_CONTRATO", length = 35, nullable = false)
    private String numeroContrato;

    @Column(name = "TXT_DESCRICAO", length = 35)
    private String descricao;

    @Column(name = "COD_MOTIVO_REJEICAO")
    private String motivoRejeicao;

    @Column(name = "COD_MUN_IBGE", length = 14)
    private String codigoMunicipioIBGE;

    @Column(name = "DAT_INICIAL_RECORRENCIA", nullable = false)
    @Temporal(TemporalType.DATE)
    private LocalDate dataInicialRecorrencia;

    @Column(name = "DAT_FINAL_RECORRENCIA")
    @Temporal(TemporalType.DATE)
    private LocalDate dataFinalRecorrencia;

    @Column(name = "DAT_CRIACAO_RECORRENCIA", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataCriacaoRecorrencia;

    @Column(name = "DAT_EXPIRACAO_CONFIRMACAO_SOLICITACAO", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataExpiracaoConfirmacaoSolicitacao;

    @Column(name = "DAT_INICIO_CONFIRMACAO")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataInicioConfirmacao;

    @Column(name = "DAT_CRIACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;
}
