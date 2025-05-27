package io.sicredi.spirecorrencia.api;

import io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento.ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationTeste implements ApplicationRunner {
    private final ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoService service;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        service.atualizarConfirmacaoCancelamentoPixAutomaticoJobService();
    }
}