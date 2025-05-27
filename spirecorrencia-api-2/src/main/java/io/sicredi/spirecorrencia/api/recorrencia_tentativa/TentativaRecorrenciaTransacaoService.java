package io.sicredi.spirecorrencia.api.recorrencia_tentativa;

import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TentativaRecorrenciaTransacaoService {

    private final RecorrenciaTransacaoTentativaRepository tentativaRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarRecorrenciaTransacaoTentativa(
            String motivo,
            String codigo,
            RecorrenciaTransacao recorrenciaTransacao
    ) {
        log.debug("Gerando registro de tentativa de liquidação para parcela com id: {}, motivo: {}, codigo: {}", recorrenciaTransacao.getIdParcela(), motivo, codigo);
        RecorrenciaTransacaoTentativa tentativa = RecorrenciaTransacaoTentativa.builder()
                .recorrenciaTransacao(recorrenciaTransacao)
                .idFimAFim(recorrenciaTransacao.getIdFimAFim())
                .codigo(codigo)
                .motivo(motivo)
                .build();

        tentativaRepository.save(tentativa);
    }
}
