package io.sicredi.spirecorrencia.api.notificacao_dia_anterior_job;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.micrometer.tracing.Tracer;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.config.AppConfig.JobShedLock;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificacaoDiaAnteriorJobTest {

    JobShedLock jobShedLock = Mockito.mock(JobShedLock.class);
    @Mock
    private AppConfig appConfig;
    @Mock
    private RecorrenciaTransacaoRepository repository;
    @Mock
    private NotificacaoProducer producer;
    @Mock
    private Tracer tracer;
    private ObservabilidadeDecorator observabilidadeDecorator;
    private NotificacaoDiaAnteriorJob job;


    @Nested
    @DisplayName("Dado job desabilitado")
    class DadoJobDesabilitado {
        @BeforeEach
        void setUp() {
            observabilidadeDecorator = Mockito.spy(new ObservabilidadeDecorator(tracer));
            job = new NotificacaoDiaAnteriorJob(
                    appConfig,
                    observabilidadeDecorator,
                    repository,
                    producer
            );

            when(appConfig.getJobNotificacaoDiaAnterior()).thenReturn(jobShedLock);
            when(jobShedLock.isJobHabilitado()).thenReturn(false);
        }

        @Test
        @DisplayName("Quando executar, deve ignorar processamento")
        void dadoJobDesabilitado_quandoExecutar_deveIgnorarProcessamento() {
            when(appConfig.getJobNotificacaoDiaAnterior()).thenReturn(jobShedLock);
            when(jobShedLock.isJobHabilitado()).thenReturn(false);

            job.executar();

            verifyNoInteractions(observabilidadeDecorator, repository, producer);
        }
    }

    @Nested
    @DisplayName("Dado job habilitado")
    class DadoJobHabilitado {

        public static final String AGENCIA = "1234";
        public static final String CPF_CNPJ = "12345678901";
        public static final String CONTA = "12345";

        @BeforeEach
        void setUp() {
            observabilidadeDecorator = Mockito.spy(new ObservabilidadeDecorator(tracer));
            job = new NotificacaoDiaAnteriorJob(appConfig, observabilidadeDecorator, repository, producer);

            when(appConfig.getJobNotificacaoDiaAnterior()).thenReturn(jobShedLock);
            when(jobShedLock.isJobHabilitado()).thenReturn(true);
            when(jobShedLock.getNomeJob()).thenReturn("nomeJob");
            mockObservabilidadeDecorator();
        }

        @Test
        @DisplayName("Dado nenhuma transação para notificar, quando executar, deve ignorar processamento")
        void dadoNenhumaTransacaoParaNotificar_quandoExecutar_deveIgnorarProcessamento() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(10);
            when(repository.consultaTransacoesProximoDia(any(), any(), any())).thenReturn(Page.empty());

            job.executar();

            verify(repository).consultaTransacoesProximoDia(
                    any(),
                    eq(List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO)),
                    any()
            );
            verify(appConfig, times(3)).getJobNotificacaoDiaAnterior();
            verify(jobShedLock).isJobHabilitado();
            verifyNoInteractions(producer);
            verify(repository, never()).atualizaStatusNotificacaoRecorrenciaTransacao(any(), any(), any());
        }

        @Test
        void dadoRecorrenciaTransacaoEncontrada_quandoExecutar_deveProcessarSemErros() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(1);

            RecorrenciaTransacao recorrenciaTransacao = RecorrenciaTransacao.builder()
                    .oidRecorrenciaTransacao(1L)
                    .notificadoDiaAnterior(false)
                    .tpoStatus(TipoStatusEnum.CRIADO)
                    .recorrencia(Recorrencia.builder()
                            .idRecorrencia("333")
                            .pagador(Pagador.builder()
                                    .oidPagador(12L)
                                    .agencia(AGENCIA)
                                    .conta(CONTA)
                                    .cpfCnpj(CPF_CNPJ)
                                    .build())
                            .tipoCanal(TipoCanalEnum.MOBI)
                            .build()
                    )
                    .build();

            when(repository.consultaTransacoesProximoDia(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                    recorrenciaTransacao
            )));

            job.executar();

            verify(repository).consultaTransacoesProximoDia(
                    any(),
                    eq(List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO)),
                    any()
            );
            verify(producer).enviarNotificacao(any());
            verify(repository).atualizaStatusNotificacaoRecorrenciaTransacao(eq(Boolean.TRUE), any(), any());
        }

        @Test
        void dadoDuasRecorrenciaTransacaoEncontradaParaMesmoPagador_quandoExecutar_deveProcessarSemErrosENotificarApenasUmaVez() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(1);

            Pagador pagador = Pagador.builder()
                    .oidPagador(12L)
                    .agencia(AGENCIA)
                    .conta(CONTA)
                    .cpfCnpj(CPF_CNPJ)
                    .build();

            when(repository.consultaTransacoesProximoDia(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                    RecorrenciaTransacao.builder()
                            .notificadoDiaAnterior(false)
                            .tpoStatus(TipoStatusEnum.CRIADO)
                            .oidRecorrenciaTransacao(1L)
                            .recorrencia(Recorrencia.builder()
                                    .idRecorrencia("333")
                                    .pagador(pagador)
                                    .tipoCanal(TipoCanalEnum.MOBI)
                                    .build()
                            )
                            .build(),
                    RecorrenciaTransacao.builder()
                            .notificadoDiaAnterior(false)
                            .tpoStatus(TipoStatusEnum.CRIADO)
                            .oidRecorrenciaTransacao(2L)
                            .recorrencia(Recorrencia.builder()
                                    .idRecorrencia("444")
                                    .pagador(pagador)
                                    .tipoCanal(TipoCanalEnum.MOBI)
                                    .build()
                            )
                            .build()
            )));

            job.executar();

            verify(repository).consultaTransacoesProximoDia(
                    any(),
                    eq(List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO)),
                    any()
            );
            verify(producer).enviarNotificacao(any());
            verify(repository).atualizaStatusNotificacaoRecorrenciaTransacao(eq(Boolean.TRUE), any(), any());
        }

        @Test
        void dadoDuasRecorrenciaTransacaoEncontrada_quandoExecutar_deveProcessarSemErrosENotificarDuasVezes() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(1);
            when(repository.consultaTransacoesProximoDia(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                    RecorrenciaTransacao.builder()
                            .notificadoDiaAnterior(false)
                            .tpoStatus(TipoStatusEnum.CRIADO)
                            .oidRecorrenciaTransacao(1L)
                            .recorrencia(Recorrencia.builder()
                                    .idRecorrencia("333")
                                    .pagador(Pagador.builder()
                                            .oidPagador(12L)
                                            .agencia(AGENCIA)
                                            .conta(CONTA)
                                            .cpfCnpj(CPF_CNPJ)
                                            .build())
                                    .tipoCanal(TipoCanalEnum.MOBI)
                                    .build()
                            ).build(),
                    RecorrenciaTransacao.builder()
                            .notificadoDiaAnterior(false)
                            .tpoStatus(TipoStatusEnum.CRIADO)
                            .oidRecorrenciaTransacao(2L)
                            .recorrencia(Recorrencia.builder()
                                    .idRecorrencia("444")
                                    .pagador(Pagador.builder()
                                            .oidPagador(19L)
                                            .agencia(AGENCIA)
                                            .conta(CONTA)
                                            .cpfCnpj(CPF_CNPJ)
                                            .build())
                                    .tipoCanal(TipoCanalEnum.MOBI)
                                    .build()
                            ).build()
            )));

            job.executar();

            verify(repository).consultaTransacoesProximoDia(
                    any(),
                    eq(List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO)),
                    any()
            );
            verify(producer, times(2)).enviarNotificacao(any());
            verify(repository, times(2)).atualizaStatusNotificacaoRecorrenciaTransacao(eq(Boolean.TRUE), any(), any());
        }

        @Test
        void dadoTresRecorrenciaTransacaoEncontradas_quandoExecutar_deveProcessarSemErrosENotificarUmaVez() {
            when(jobShedLock.getTamanhoDaConsulta()).thenReturn(1);
            when(repository.consultaTransacoesProximoDia(any(), any(), any())).thenReturn(new PageImpl<>(List.of(
                RecorrenciaTransacao.builder()
                    .notificadoDiaAnterior(false)
                    .tpoStatus(TipoStatusEnum.CRIADO)
                    .oidRecorrenciaTransacao(1L)
                    .recorrencia(Recorrencia.builder()
                        .idRecorrencia("333")
                        .pagador(Pagador.builder()
                            .oidPagador(1L)
                            .agencia(AGENCIA)
                            .conta(CONTA)
                            .cpfCnpj(CPF_CNPJ)
                            .build())
                        .tipoCanal(TipoCanalEnum.MOBI)
                        .build()
                    ).build(),
                RecorrenciaTransacao.builder()
                    .notificadoDiaAnterior(true)
                    .tpoStatus(TipoStatusEnum.CRIADO)
                    .oidRecorrenciaTransacao(3L)
                    .recorrencia(Recorrencia.builder()
                        .idRecorrencia("444")
                        .pagador(Pagador.builder()
                            .oidPagador(3L)
                            .agencia(AGENCIA)
                            .conta(CONTA)
                            .cpfCnpj(CPF_CNPJ)
                            .build())
                        .tipoCanal(TipoCanalEnum.MOBI)
                        .build()
                    ).build()
            )));

            job.executar();

            verify(repository).consultaTransacoesProximoDia(
                any(),
                eq(List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO)),
                any()
            );
            verify(producer, times(1)).enviarNotificacao(any());
            verify(repository, times(1)).atualizaStatusNotificacaoRecorrenciaTransacao(eq(Boolean.TRUE), any(), any());
        }

        private void mockObservabilidadeDecorator() {
            doAnswer(invocation -> {
                invocation.getArgument(1, Runnable.class).run();
                return null;
            }).when(observabilidadeDecorator).executar(anyMap(), any(Runnable.class));
        }
    }

}