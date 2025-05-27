package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import io.sicredi.spirecorrencia.api.repositorio.Recebedor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
interface RecorrenciaInstrucaoPagamentoCancelamentoRepository extends JpaRepository<RecorrenciaInstrucaoPagamentoCancelamento, String> {


    @Modifying
    @Query(value = """
                UPDATE RecorrenciaInstrucaoPagamentoCancelamento RIPC
                SET RIPC.tpoStatus = :novoStatus
                WHERE RIPC.codFimAFim IN (:listaCodigosFimAfim)
            """)
    void atualizaStatusByListaCodFimAFim(List<String> listaCodigosFimAfim, String novoStatus);

    @Query(value = """
                SELECT RIPC.codFimAFim
                FROM RecorrenciaInstrucaoPagamentoCancelamento RIPC
                WHERE RIPC.tpoPspSolicitanteCancelamento = :tpoPspSolicitanteCancelamento
                AND RIPC.tpoStatus IN (:listStatus)
                AND RIPC.datCriacaoSolicitacaoCancelamento < :dataHora12horas
                ORDER BY RIPC.codFimAFim
            """)
    Page<String> getListaCodFimAFimByListStatusAndTipoPSPSolicitanteCancelamento(Pageable pageable,
                                                                                 @Param("tpoPspSolicitanteCancelamento") String tpoPspSolicitanteCancelamento,
                                                                                 @Param("listStatus") List<String> listStatus,
                                                                                 @Param("dataHora12horas") LocalDateTime dataHora12horas);

}

