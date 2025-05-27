package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
interface RecorrenciaAutorizacaoPagamentoImediatoRepository extends JpaRepository<RecorrenciaAutorizacaoPagamentoImediato, String> {

}