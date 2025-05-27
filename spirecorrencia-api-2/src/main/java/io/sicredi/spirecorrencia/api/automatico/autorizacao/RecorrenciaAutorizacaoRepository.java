package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
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
public interface RecorrenciaAutorizacaoRepository extends JpaRepository<RecorrenciaAutorizacao, Long> {

    @Query(value = """
            SELECT REC_AUT
            FROM RecorrenciaAutorizacao REC_AUT
            WHERE (:#{#request.cpfCnpjPagador} IS NULL OR REC_AUT.cpfCnpjPagador = :#{#request.cpfCnpjPagador})
            AND (:#{#request.agenciaPagador} IS NULL OR REC_AUT.agenciaPagador = :#{#request.agenciaPagador})
            AND (:#{#request.contaPagador} IS NULL OR REC_AUT.contaPagador = :#{#request.contaPagador})
            AND (:#{#request.obterTipoPessoaPagador()} IS NULL OR REC_AUT.tipoPessoaPagador = :#{#request.obterTipoPessoaPagador()})
            AND (:#{#request.status} IS NULL OR REC_AUT.tipoStatus IN :#{#request.status})
            AND (:#{#request.dataInicial} IS NULL OR REC_AUT.dataCriacaoRecorrencia >= :#{#request.dataInicial})
            AND (:#{#request.dataFinal} IS NULL OR REC_AUT.dataCriacaoRecorrencia <= :#{#request.dataFinal})
            """)
    Page<RecorrenciaAutorizacao> findAllByFiltros(@Param("request") ConsultaAutorizacaoRequest request,
                                                  Pageable pageable);
    @Query(value = """
            SELECT REC_AUT
            FROM RecorrenciaAutorizacao REC_AUT
            WHERE REC_AUT.oidRecorrenciaAutorizacao = :oidRecorrenciaAutorizacao
            """)
    Optional<RecorrenciaAutorizacao> consultarPorIdentificadorRecorrencia(Long oidRecorrenciaAutorizacao);

    List<RecorrenciaAutorizacao> findAllByIdRecorrenciaOrderByDataAlteracaoRegistroDesc(String idRecorrencia);

    Optional<RecorrenciaAutorizacao> findByIdInformacaoStatusEnvio(String idInformacaoStatusEnvio);

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaAutorizacao a
        SET a.tipoSubStatus = :novoSubStatus, a.dataAlteracaoRegistro = :dataAlteracaoRegistro
        WHERE a.oidRecorrenciaAutorizacao = :oidRecorrenciaAutorizacao
          AND a.tipoStatus = 'CRIADA'
          AND a.tipoSubStatus = 'AGUARDANDO_ENVIO'
    """)
    void atualizaSubStatusSeCriadaEAguardandoEnvio(@Param("oidRecorrenciaAutorizacao") Long oidRecorrenciaAutorizacao,
                                                  @Param("novoSubStatus") String novoSubStatus,
                                                  @Param("dataAlteracaoRegistro") LocalDateTime dataAlteracaoRegistro);

    Optional<RecorrenciaAutorizacao> findFirstByIdRecorrenciaAndTipoStatus(String idRecorrencia, TipoStatusAutorizacao status);

    Optional<RecorrenciaAutorizacao> findByIdRecorrenciaAndTipoStatusIn(String idRecorrencia, List<TipoStatusAutorizacao> statusValidos);

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaAutorizacao a
        SET a.tipoSubStatus = :novoSubStatus, a.dataAlteracaoRegistro = current_timestamp
        WHERE a.idRecorrencia = :idRecorrencia
    """)
    void atualizaSubStatusPorIdRecorrencia(@Param("idRecorrencia") String idRecorrencia,
                                           @Param("novoSubStatus") String novoSubStatus);

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaAutorizacao r
        SET r.tipoStatus = :tipoStatusAutorizacao, r.tipoSubStatus = :tipoSubStatus, r.dataAlteracaoRegistro = current_timestamp
        WHERE r.oidRecorrenciaAutorizacao = :oidRecorrenciaAutorizacao
    """)
    void atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(@Param("oidRecorrenciaAutorizacao") Long oidRecorrenciaAutorizacao,
                                                                @Param("tipoStatusAutorizacao") TipoStatusAutorizacao tipoStatusAutorizacao,
                                                                @Param("tipoSubStatus") String novoSubStatus);
    @Query("""
            SELECT r
            FROM RecorrenciaAutorizacao r
            WHERE r.tipoStatus = :tipoStatusAutorizacao
              AND r.dataInicioConfirmacao <= :dataCriacaoAntesDe
            """)
    Page<RecorrenciaAutorizacao> buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(
            @Param("tipoStatusAutorizacao") TipoStatusAutorizacao tipoStatusAutorizacao,
            @Param("dataCriacaoAntesDe") LocalDateTime dataCriacaoAntesDe, Pageable pageable);

    @Query(value = """
            SELECT r
            FROM RecorrenciaAutorizacao r
            LEFT JOIN FETCH r.ciclos c
            WHERE r.idRecorrencia = :idRecorrencia
           """)
    List<RecorrenciaAutorizacao> findWithCiclosByIdRecorrencia(String idRecorrencia);
}