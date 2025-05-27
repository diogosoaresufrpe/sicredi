package io.sicredi.spirecorrencia.api.automatico.instrucaopagamento;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RecorrenciaInstrucaoPagamentoRepository extends JpaRepository<RecorrenciaInstrucaoPagamento, String> {

    @Query(value = """
            SELECT INSTRUCAO
            FROM RecorrenciaInstrucaoPagamento INSTRUCAO
            LEFT JOIN FETCH INSTRUCAO.autorizacao AUTORIZACAO
            WHERE INSTRUCAO.codFimAFim = :idFimAFim
            """)
    Optional<RecorrenciaInstrucaoPagamento> buscarPorCodFimAFimComAutorizacao(@Param("idFimAFim") String idFimAFim);

    @Modifying
    @Query(value = """
            UPDATE RecorrenciaInstrucaoPagamento REC_INST_PAG
            SET REC_INST_PAG.tpoStatus = :tpoStatus,
                REC_INST_PAG.tpoSubStatus = :tpoSubStatus,
                REC_INST_PAG.datAlteracaoRegistro = :dataHoraAtual
            WHERE REC_INST_PAG.codFimAFim = :codFimAFim
            """)
    void atualizaTpoStatusETipoSubStatus(@Param("codFimAFim") String codFimAFim,
                                         @Param("tpoStatus") String tpoStatus,
                                         @Param("tpoSubStatus") String tpoSubStatus,
                                         @Param("dataHoraAtual") LocalDateTime dataHoraAtual);


    @Modifying
    @Query(value = """
        UPDATE RecorrenciaInstrucaoPagamento RIP
        SET RIP.tpoStatus = :novoStatus
        WHERE RIP.codFimAFim in (:listaCodFimAFim)
    """)
    void atualizaStatusByListaCodFimAFim(@Param("listaCodFimAFim") List<String> listaCodFimAFim,
                                        @Param("novoStatus") String novoStatus);



}