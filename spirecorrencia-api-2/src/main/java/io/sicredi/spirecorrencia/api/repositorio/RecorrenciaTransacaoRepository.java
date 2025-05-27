package io.sicredi.spirecorrencia.api.repositorio;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.consulta.ConsultaParcelasRecorrenciaRequest;
import io.sicredi.spirecorrencia.api.consulta.ListagemParcelaRecorrenciaProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecorrenciaTransacaoRepository extends JpaRepository<RecorrenciaTransacao, Long>, JpaSpecificationExecutor<RecorrenciaTransacao> {

    @Query("""
            SELECT PARCELA
            FROM RecorrenciaTransacao PARCELA
            WHERE PARCELA.recorrencia.oidRecorrencia IN (:listaOidRecorrencia)
            """)
    List<RecorrenciaTransacao> consultarPorRecorrenciaIn(List<Long> listaOidRecorrencia);

    @Query(value = """
            SELECT PARCELA
            FROM RecorrenciaTransacao PARCELA
            JOIN FETCH PARCELA.recorrencia RECORRENCIA
            JOIN FETCH RECORRENCIA.pagador PAGADOR
            WHERE PARCELA.dataTransacao = :dataProximoDia
            AND PARCELA.tpoStatus IN :status
            """
    )
    Page<RecorrenciaTransacao> consultaTransacoesProximoDia(
        @Param("dataProximoDia") LocalDate dataProximoDia,
        @Param("status") List<TipoStatusEnum> statusList,
        Pageable pageable
    );

    @Modifying
    @Query(value = """
            UPDATE RecorrenciaTransacao PARCELA
            SET PARCELA.notificadoDiaAnterior = :notificadoDiaAnterior,
            PARCELA.dataAlteracaoRegistro = :dataAlteracao
            WHERE PARCELA.oidRecorrenciaTransacao IN :listaOidRecorrenciaTransacao
            """
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void atualizaStatusNotificacaoRecorrenciaTransacao(
            Boolean notificadoDiaAnterior,
            LocalDateTime dataAlteracao,
            List<Long> listaOidRecorrenciaTransacao
    );

    @Query(value = """
        SELECT *
        FROM (
           SELECT PARCELA.ID_PARCELA AS IDENTIFICADOR_PARCELA,
                  REC.ID_RECORRENCIA AS IDENTIFICADOR_RECORRENCIA,
                  PARCELA.NUM_VALOR AS VALOR,
                  PARCELA.DAT_TRANSACAO AS DATA_TRANSACAO,
                  PARCELA.DAT_EXCLUSAO AS DATA_EXCLUSAO,
                  PARCELA.TPO_STATUS AS STATUS,
                  REC.TPO_RECORRENCIA AS TIPO_RECORRENCIA,
                  RECEBEDOR.NOME AS NOME_RECEBEDOR
            FROM SPI_OWNER.RECORRENCIA_TRANSACAO PARCELA
            JOIN SPI_OWNER.RECORRENCIA REC ON PARCELA.OID_RECORRENCIA = REC.OID_RECORRENCIA
            JOIN SPI_OWNER.RECORRENCIA_PAGADOR PAG ON REC.OID_RECORRENCIA_PAGADOR = PAG.OID_RECORRENCIA_PAGADOR
            JOIN SPI_OWNER.RECORRENCIA_RECEBEDOR RECEBEDOR ON REC.OID_RECORRENCIA_RECEBEDOR = RECEBEDOR.OID_RECORRENCIA_RECEBEDOR
            WHERE (:#{#request.cpfCnpjPagador} IS NULL OR PAG.NUM_CPF_CNPJ = :#{#request.cpfCnpjPagador})
              AND (:#{#request.agenciaPagador} IS NULL OR PAG.NUM_AGENCIA = :#{#request.agenciaPagador})
              AND (:#{#request.contaPagador} IS NULL OR PAG.NUM_CONTA = :#{#request.contaPagador})
              AND (:#{#request.obterTipoPessoaPagador()} IS NULL OR PAG.TPO_PESSOA = :#{#request.tipoPessoaPagador})
              AND (:#{#request.obterPrimeiroElementoTipoRecorrencia()} IS NULL OR REC.TPO_RECORRENCIA IN (:#{#request.tipoRecorrencia}))
              AND (:#{#request.obterPrimeiroElementoStatus()} IS NULL OR PARCELA.TPO_STATUS IN (:#{#request.status}))
              AND (:#{#request.dataInicial} IS NULL OR PARCELA.DAT_TRANSACAO >= :#{#request.dataInicial})
              AND (:#{#request.dataFinal} IS NULL OR PARCELA.DAT_TRANSACAO <= :#{#request.dataFinal})
              ORDER BY PARCELA.DAT_TRANSACAO
        )
        """,
            countQuery = """
        SELECT COUNT(1)
            FROM SPI_OWNER.RECORRENCIA_TRANSACAO PARCELA
            JOIN SPI_OWNER.RECORRENCIA REC ON PARCELA.OID_RECORRENCIA = REC.OID_RECORRENCIA
            JOIN SPI_OWNER.RECORRENCIA_PAGADOR PAG ON REC.OID_RECORRENCIA_PAGADOR = PAG.OID_RECORRENCIA_PAGADOR
            WHERE (:#{#request.cpfCnpjPagador} IS NULL OR PAG.NUM_CPF_CNPJ = :#{#request.cpfCnpjPagador})
              AND (:#{#request.agenciaPagador} IS NULL OR PAG.NUM_AGENCIA = :#{#request.agenciaPagador})
              AND (:#{#request.contaPagador} IS NULL OR PAG.NUM_CONTA = :#{#request.contaPagador})
              AND (:#{#request.obterTipoPessoaPagador()} IS NULL OR PAG.TPO_PESSOA = :#{#request.tipoPessoaPagador})
              AND (:#{#request.obterPrimeiroElementoTipoRecorrencia()} IS NULL OR REC.TPO_RECORRENCIA IN (:#{#request.tipoRecorrencia}))
              AND (:#{#request.obterPrimeiroElementoStatus()} IS NULL OR PARCELA.TPO_STATUS IN (:#{#request.status}))
              AND (:#{#request.dataInicial} IS NULL OR PARCELA.DAT_TRANSACAO >= :#{#request.dataInicial})
              AND (:#{#request.dataFinal} IS NULL OR PARCELA.DAT_TRANSACAO <= :#{#request.dataFinal})
        """,
            nativeQuery = true)
    Page<ListagemParcelaRecorrenciaProjection> findParcelasByFiltros(@Param("request") ConsultaParcelasRecorrenciaRequest request, Pageable pageable);


    Optional<RecorrenciaTransacao> findByIdFimAFim(String idFimAFim);

    @Query(value = """
            SELECT PARCELA
            FROM RecorrenciaTransacao PARCELA
            JOIN FETCH PARCELA.recorrencia RECORRENCIA
            JOIN FETCH RECORRENCIA.pagador PAGADOR
            JOIN FETCH RECORRENCIA.recebedor RECEBEDOR
            WHERE PARCELA.dataTransacao = :dataTransacao
            """)
    Slice<RecorrenciaTransacao> buscarParcelasParaProcessamento(@Param("dataTransacao") LocalDate dataTransacao, Pageable pageable);

    @Modifying
    @Query("UPDATE RecorrenciaTransacao RT SET RT.tpoStatus = :status, RT.idFimAFim = :idFimAFim, RT.dataAlteracaoRegistro = :dataAlteracao WHERE RT.oidRecorrenciaTransacao = :oidRecorrenciaTransacao")
    void updateStateAndIdFimAFim(@Param("oidRecorrenciaTransacao") Long oidRecorrenciaTransacao, @Param("status")  TipoStatusEnum status, @Param("idFimAFim")  String idFimAFim, @Param("dataAlteracao")  LocalDateTime dataAlteracao);

    @Query("""
        SELECT PARCELA
        FROM RecorrenciaTransacao PARCELA
        WHERE PARCELA.recorrencia.idRecorrencia = :idRecorrencia
        AND PARCELA.tpoStatus IN (:status)
    """)
    List<RecorrenciaTransacao> findByRecorrenciaAndStatus(@Param("idRecorrencia") String idRecorrencia, @Param("status") List<TipoStatusEnum> status);

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaTransacao RT
        SET RT.tpoStatus = :novoStatus,
        RT.tipoMotivoExclusao = :tipoMotivoExclusao
        WHERE RT.idFimAFim in (:listaCodigosFimAfim)
    """)
    void atualizaStatusByCodigoFimAFim(@Param("listaCodigosFimAfim") List<String> listaCodigosFimAfim,
                                       @Param("novoStatus") TipoStatusEnum novoStatus,
                                       @Param("tipoMotivoExclusao") TipoMotivoExclusao tipoMotivoExclusao);

}
