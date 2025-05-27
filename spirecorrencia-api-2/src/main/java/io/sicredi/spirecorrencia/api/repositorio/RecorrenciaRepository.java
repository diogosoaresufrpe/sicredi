package io.sicredi.spirecorrencia.api.repositorio;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.consulta.ConsultaRecorrenciaRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface RecorrenciaRepository extends JpaRepository<Recorrencia, Long> {

    @Query("""
            SELECT REC
            FROM Recorrencia REC
            JOIN FETCH REC.recebedor RECEB
            JOIN FETCH REC.pagador PAGADOR
            JOIN FETCH REC.recorrencias PARCELAS
            WHERE REC.idRecorrencia = :identificadorRecorrencia
            """)
    Optional<Recorrencia> consultarPorIdentificadorRecorrencia(String identificadorRecorrencia);

    @Query(value = """
            SELECT REC
            FROM Recorrencia REC
            JOIN FETCH REC.recebedor RECEB
            JOIN REC.pagador PAG
            WHERE (:#{#request.cpfCnpjPagador} IS NULL OR PAG.cpfCnpj = :#{#request.cpfCnpjPagador})
            AND (:#{#request.agenciaPagador} IS NULL OR PAG.agencia = :#{#request.agenciaPagador})
            AND (:#{#request.contaPagador} IS NULL OR PAG.conta = :#{#request.contaPagador})
            AND (:#{#request.obterTipoPessoaPagador()} IS NULL OR PAG.tipoPessoa = :#{#request.obterTipoPessoaPagador()})
            AND (:#{#request.tipoRecorrencia} IS NULL OR REC.tipoRecorrencia IN :#{#request.tipoRecorrencia})
            AND (:#{#request.status} IS NULL OR REC.tipoStatus IN :#{#request.status})
            AND (:#{#request.dataInicial} IS NULL OR REC.dataCriacao >= :#{#request.dataInicial})
            AND (:#{#request.dataFinal} IS NULL OR REC.dataCriacao <= :#{#request.dataFinal})
            """)
    Page<Recorrencia> findAllByFiltros(@Param("request") ConsultaRecorrenciaRequest request,
                                       Pageable pageable);

    @Query(value = """ 
                SELECT r
                FROM Recorrencia r
                JOIN FETCH r.recebedor recebedor
                LEFT JOIN FETCH r.recorrencias
                JOIN FETCH r.pagador pagador
                WHERE r.idRecorrencia = :idRecorrencia
            """)
    Optional<Recorrencia> findByIdRecorrencia(@Param("idRecorrencia") String idRecorrencia);

    @Query(value = """
            SELECT recorrencia
            FROM Recorrencia recorrencia
            JOIN FETCH recorrencia.recebedor recebedor
            LEFT JOIN FETCH recorrencia.recorrencias parcelas
            JOIN FETCH recorrencia.pagador pagador
            WHERE recorrencia.oidRecorrencia = (
                 SELECT parcela.recorrencia.oidRecorrencia
                 FROM RecorrenciaTransacao parcela
                 WHERE parcela.idParcela = :#{#idParcela}
            )
            AND (:#{#agenciaPagador} IS NULL OR pagador.agencia = :#{#agenciaPagador})
            AND (:#{#contaPagador} IS NULL OR pagador.conta = :#{#contaPagador})
            """)
    Optional<Recorrencia> findRecorrenciaByIdParcelaAndAgenciaAndConta(@Param("idParcela") String idParcela, @Param("agenciaPagador") String agenciaPagador, @Param("contaPagador") String contaPagador);

    @Query("""
            SELECT recorrencia
            FROM Recorrencia recorrencia
            LEFT JOIN FETCH recorrencia.recorrencias
            JOIN FETCH recorrencia.pagador pagador
            WHERE recorrencia.pagador.cpfCnpj = :cpfCnpjPagador
            AND recorrencia.pagador.agencia = :agenciaPagador
            AND recorrencia.pagador.conta = :contaPagador
            AND recorrencia.tipoStatus IN (:status)
            AND recorrencia.tipoRecorrencia IN (:tiposRecorrencia)
            """)
    Page<Recorrencia> consultarPorDadosPagador(String cpfCnpjPagador, String agenciaPagador, String contaPagador, Set<TipoStatusEnum> status, Set<TipoRecorrencia> tiposRecorrencia, Pageable pageable);

}
