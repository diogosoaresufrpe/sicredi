package io.sicredi.spirecorrencia.api.recorrencia_tentativa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecorrenciaTransacaoTentativaRepository extends JpaRepository<RecorrenciaTransacaoTentativa, Long> {
}