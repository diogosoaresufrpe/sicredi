package io.sicredi.spirecorrencia.api.automatico;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusSolicitacaoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.solicitacao.ConsultaSolicitacaoAutorizacaoRecorrenciaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitacaoAutorizacaoRecorrenciaRepository extends JpaRepository<SolicitacaoAutorizacaoRecorrencia, String> {

    @Query(value = """
            SELECT REC_AUT_SOLIC
            FROM SolicitacaoAutorizacaoRecorrencia REC_AUT_SOLIC
            WHERE (:#{#request.cpfCnpjPagador} IS NULL OR REC_AUT_SOLIC.cpfCnpjPagador = :#{#request.cpfCnpjPagador})
            AND (:#{#request.agenciaPagador} IS NULL OR REC_AUT_SOLIC.agenciaPagador = :#{#request.agenciaPagador})
            AND (:#{#request.contaPagador} IS NULL OR REC_AUT_SOLIC.contaPagador = :#{#request.contaPagador})
            AND (:#{#request.obterTipoPessoaPagador()} IS NULL OR REC_AUT_SOLIC.tipoPessoaPagador = :#{#request.obterTipoPessoaPagador()})
            AND (:#{#request.status} IS NULL OR REC_AUT_SOLIC.tipoStatus IN :#{#request.status})
            AND (:#{#request.dataInicial} IS NULL OR REC_AUT_SOLIC.dataCriacaoRecorrencia >= :#{#request.dataInicial})
            AND (:#{#request.dataFinal} IS NULL OR REC_AUT_SOLIC.dataCriacaoRecorrencia <= :#{#request.dataFinal})
            """)
    Page<SolicitacaoAutorizacaoRecorrencia> findAllByFiltros(@Param("request") ConsultaSolicitacaoAutorizacaoRecorrenciaRequest request,
                                                             Pageable pageable);

    Optional<SolicitacaoAutorizacaoRecorrencia> findFirstByIdRecorrenciaAndTipoStatusIn(String idRecorrencia, List<TipoStatusSolicitacaoAutorizacao> statusValidos);

    Optional<SolicitacaoAutorizacaoRecorrencia> findFirstByIdSolicitacaoRecorrenciaAndTipoStatusIn(String idSolicitacaoRecorrencia, List<TipoStatusSolicitacaoAutorizacao> statusValidos);

    Optional<SolicitacaoAutorizacaoRecorrencia> findByIdInformacaoStatusAndIdRecorrencia(String idInformacaoStatus, String idRecorrencia);

    @Modifying
    @Query("UPDATE SolicitacaoAutorizacaoRecorrencia s SET s.tipoStatus = :novoTipoStatus, s.tipoSubStatus = :novoSubStatus, s.dataAlteracaoRegistro = :dataAlteracaoRegistro WHERE s.idRecorrencia = :idRecorrencia AND s.tipoStatus = 'CONFIRMADA'")
    int atualizaStatusSeStatusAtualForConfirmadoPorIdRecorrencia(String idRecorrencia, TipoStatusSolicitacaoAutorizacao novoTipoStatus, String novoSubStatus, LocalDateTime dataAlteracaoRegistro);

    @Modifying
    @Query(value = """
            UPDATE SolicitacaoAutorizacaoRecorrencia solicitacao
            SET    solicitacao.tipoStatus = :novoStatus,
                   solicitacao.tipoSubStatus = :novoSubStatus,
                   solicitacao.dataAlteracaoRegistro = current_timestamp
            WHERE solicitacao.idRecorrencia = :idRecorrencia AND
                  solicitacao.tipoStatus = 'CONFIRMADA' AND
                  solicitacao.tipoSubStatus = 'AGUARDANDO_ENVIO'
        """)
    int atualizaSubStatusSeConfirmadaEAguardandoEnvioPorIdRecorrencia( @Param("idRecorrencia") String idRecorrencia,
                                                                       @Param("novoStatus") TipoStatusSolicitacaoAutorizacao novoStatus,
                                                                       @Param("novoSubStatus") String novoSubStatus);

    @Modifying
    @Query(value = """
            UPDATE SolicitacaoAutorizacaoRecorrencia REC_AUT_SOLIC
            SET REC_AUT_SOLIC.tipoStatus = :status,
                REC_AUT_SOLIC.tipoSubStatus = :subStatus,
                REC_AUT_SOLIC.motivoRejeicao = :codErro,
                REC_AUT_SOLIC.dataInicioConfirmacao = :dataHoraInicioCanal,
                REC_AUT_SOLIC.dataAlteracaoRegistro = :dataHoraAtual
            WHERE REC_AUT_SOLIC.idSolicitacaoRecorrencia = :id
            """)
    void atualizaRecorrenciaAutorizacaoSolicitacao(@Param("id") String id,
                                                   @Param("dataHoraInicioCanal") LocalDateTime dataHoraInicioCanal,
                                                   @Param("codErro") String codErro,
                                                   @Param("status") TipoStatusSolicitacaoAutorizacao status,
                                                   @Param("subStatus") String subStatus,
                                                   @Param("dataHoraAtual") LocalDateTime dataHoraAtual);

    @Query(value = """
            SELECT s FROM SolicitacaoAutorizacaoRecorrencia s
        WHERE s.dataExpiracaoConfirmacaoSolicitacao BETWEEN :dataInicio AND :dataFim
        AND s.tipoStatus = :tipoStatus
        """)
    Page<SolicitacaoAutorizacaoRecorrencia> buscaSolicitacaoDeAutorizacaoPixAutomaticoExpirada(Pageable pageable,
                                                                                               @Param("dataInicio") LocalDateTime dataInicio,
                                                                                               @Param("dataFim") LocalDateTime dataFim,
                                                                                               @Param("tipoStatus") TipoStatusSolicitacaoAutorizacao tipoStatus);

    @Modifying
    @Query(value = """
            UPDATE SolicitacaoAutorizacaoRecorrencia s
            SET s.tipoStatus = :tipoStatus, s.tipoSubStatus = :novoSubStatus, s.dataAlteracaoRegistro = :dataAlteracaoRegistro
            WHERE s.idRecorrencia = :idRecorrencia AND s.tipoStatus = :tipoStatusAtual
        """)
    int atualizarTipoStatusESubStatusPorIdRecorrenciaETipoStatusAtual(
            @Param("idRecorrencia") String idRecorrencia,
            @Param("tipoStatus") TipoStatusSolicitacaoAutorizacao tipoStatusSolicitacaoAutorizacao,
            @Param("novoSubStatus") String novoSubStatus,
            @Param("dataAlteracaoRegistro") LocalDateTime dataAlteracaoRegistro,
            @Param("tipoStatusAtual") TipoStatusSolicitacaoAutorizacao tipoStatusAtual);
}