package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJobTest {

    private static final String JOB_TESTE = "job-teste";
    private static final int TAMANHO_PAGINA = 1000;
    private static final int MINUTOS_EXPIRACAO = 30;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    AppConfig appConfig;

    @Mock
    private ObservabilidadeDecorator observabilidadeDecorator;

    @Mock
    private ProcessarConfirmacaoAutorizacaoExpiradasService service;

    private ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJob job;

    @BeforeEach
    void setup() {
        job = new ConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJob(appConfig, observabilidadeDecorator, service);
    }

    @Test
    void dadoConfirmacaoUsuarioPagadorAutorizacaoPixAutomaticoJob_quandoExecutar_deveExecutarComSucesso() {

        // Arrange
        when(appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().isJobHabilitado()).thenReturn(true);
        when(appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().getNomeJob()).thenReturn(JOB_TESTE);
        when(appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().getTamanhoDaConsulta()).thenReturn(TAMANHO_PAGINA);
        when(appConfig.getRegras().getProcessamento().getMinutosExpiracao()).thenReturn(MINUTOS_EXPIRACAO);

        // Captura o Runnable e executa manualmente
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run(); // executa o conteúdo do lambda
            return null;
        }).when(observabilidadeDecorator).executar(anyMap(), any(Runnable.class));


        // Act
        job.executar();

        // Assert
        verify(service).processarAutorizacoesExpiradas(TAMANHO_PAGINA, MINUTOS_EXPIRACAO);

        // Assert
        verify(observabilidadeDecorator).executar(
                argThat(map -> JOB_TESTE.equals(map.get(RecorrenciaMdc.NOME_JOB))),
                any(Runnable.class)
        );
    }

    @Test
    @DisplayName("Com job desabilitado, não deve ser executado o processamento do mesmo.")
    void dadoJobNaoHabilitado_quandoExecutarJob_deveNaoProcessarConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico() {
        // Arrange
        when(appConfig.getJobConfirmacaoUsuarioPagadorAutorizacaoPixAutomatico().isJobHabilitado()).thenReturn(false);

        // Act
        job.executar();

        // Assert
        verifyNoInteractions(observabilidadeDecorator);
        verifyNoInteractions(service);
    }
}

