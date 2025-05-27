package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RecorrenciaAutorizacaoCancelamentoRepository extends JpaRepository<RecorrenciaAutorizacaoCancelamento, String> {

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaAutorizacaoCancelamento RAC
        SET RAC.tipoStatus = :novoStatus, RAC.idInformacaoStatus = :idInformacaoStatus,
        RAC.dataAlteracaoRegistro = :dataAlteracaoRegistro
        WHERE RAC.idRecorrencia = :idRecorrencia
    """)
    void atualizaStatusEIdInformacaoStatus(String idRecorrencia, TipoStatusCancelamentoAutorizacao novoStatus, String idInformacaoStatus, LocalDateTime dataAlteracaoRegistro);

    @Modifying
    @Query(value = """
        UPDATE RecorrenciaAutorizacaoCancelamento RAC
        SET RAC.tipoStatus = :novoStatus, RAC.idInformacaoStatus = :idInformacaoStatus,
        RAC.dataAlteracaoRegistro = :dataAlteracaoRegistro, RAC.motivoRejeicao = :motivoRejeicao
        WHERE RAC.idRecorrencia = :idRecorrencia
    """)
    void atualizaStatusIdInformacaoStatusEMotivoRejeicao(String idRecorrencia, TipoStatusCancelamentoAutorizacao novoStatus, String idInformacaoStatus, String motivoRejeicao, LocalDateTime dataAlteracaoRegistro);

    Optional<RecorrenciaAutorizacaoCancelamento> findFirstByIdRecorrenciaAndTipoStatusOrderByDataAlteracaoRegistroDesc(String idRecorrencia, TipoStatusCancelamentoAutorizacao tipoStatusCancelamentoAutorizacao);

    @Modifying
    @Query(value = """
    UPDATE RecorrenciaAutorizacaoCancelamento RAC
    SET RAC.tipoStatus = :novoStatus, RAC.dataAlteracaoRegistro = current_timestamp
    WHERE RAC.idRecorrencia = :idRecorrencia
       AND RAC.idInformacaoCancelamento = :idInformacaoCancelamento
       AND RAC.tipoStatus = io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao.CRIADA
   """)
    void atualizaRecorrenciaCancelamentoSeIdInformacaoCancelamentoEIdRecorrenciaEStatusCriada(String idInformacaoCancelamento, String idRecorrencia, TipoStatusCancelamentoAutorizacao novoStatus);
}