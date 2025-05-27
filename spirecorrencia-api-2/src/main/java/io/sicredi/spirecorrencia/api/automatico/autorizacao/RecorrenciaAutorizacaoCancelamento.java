package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "RECORRENCIA_AUTORIZACAO_CANCELAMENTO", schema = "SPI_OWNER")
public class RecorrenciaAutorizacaoCancelamento {

    @Id
    @Column(name = "ID_INFORMACAO_CANCELAMENTO", nullable = false, length = 29)
    private String idInformacaoCancelamento;

    @Column(name = "ID_RECORRENCIA", nullable = false, length = 29)
    private String idRecorrencia;

    @Column(name = "ID_INFORMACAO_STATUS", length = 29)
    private String idInformacaoStatus;

    @Column(name = "TPO_CANCELAMENTO", nullable = false, length = 23)
    @Enumerated(EnumType.STRING)
    private TipoCancelamento tipoCancelamento;

    @Column(name = "TPO_PSP_SOLICITANTE_CANCELAMENTO", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private TipoSolicitanteCancelamento tipoSolicitanteCancelamento;

    @Column(name = "TPO_STATUS", nullable = false, length = 9)
    @Enumerated(EnumType.STRING)
    private TipoStatusCancelamentoAutorizacao tipoStatus;

    @Column(name = "NUM_CPF_CNPJ_SOLICITANTE_CANCELAMENTO", nullable = false, length = 14)
    private String cpfCnpjSolicitanteCancelamento;

    @Column(name = "COD_MOTIVO_CANCELAMENTO", nullable = false)
    private String motivoCancelamento;

    @Column(name = "COD_MOTIVO_REJEICAO")
    private String motivoRejeicao;

    @Column(name = "DAT_CANCELAMENTO", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime dataCancelamento;

    @Column(name = "DAT_CRIACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @CreationTimestamp
    private LocalDateTime dataCriacaoRegistro;

    @Column(name = "DAT_ALTERACAO_REGISTRO")
    @Temporal(TemporalType.TIMESTAMP)
    @UpdateTimestamp
    private LocalDateTime dataAlteracaoRegistro;
}
