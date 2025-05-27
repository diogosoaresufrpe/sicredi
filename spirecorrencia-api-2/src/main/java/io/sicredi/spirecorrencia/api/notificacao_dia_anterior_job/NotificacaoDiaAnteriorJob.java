package io.sicredi.spirecorrencia.api.notificacao_dia_anterior_job;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.micrometer.observation.annotation.Observed;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spirecorrencia.api.utils.SystemDateUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DATA_PARCELA;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
class NotificacaoDiaAnteriorJob {

    private final AppConfig appConfig;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final RecorrenciaTransacaoRepository repository;
    private final NotificacaoProducer producer;

    @LockProviderToUse("configLockProvider")
    @SchedulerLock(
            name = "#{@appConfig.jobNotificacaoDiaAnterior.nomeJob}",
            lockAtMostFor = "#{@appConfig.jobNotificacaoDiaAnterior.lockAtMostFor}",
            lockAtLeastFor = "#{@appConfig.jobNotificacaoDiaAnterior.lockAtLeastFor}"
    )
    @Observed(name = "#{@appConfig.jobNotificacaoDiaAnterior.nomeJob}")
    public void executar() {
        if (!appConfig.getJobNotificacaoDiaAnterior().isJobHabilitado()) {
            return;
        }

        var atributos = Map.of(
                RecorrenciaMdc.NOME_JOB, appConfig.getJobNotificacaoDiaAnterior().getNomeJob()
        );

        observabilidadeDecorator.executar(atributos, () -> {
            Page<RecorrenciaTransacao> parcelasRecorrenciaPage;
            Set<Pagador> pagadoresNotificados = new HashSet<>();

            LocalDate dataProximoDia = LocalDate.now().plusDays(1);

            int paginaAtual = 0;
            int tamanhoDaConsulta = appConfig.getJobNotificacaoDiaAnterior().getTamanhoDaConsulta();

            log.info(
                    "Executando Job responsável por disparar o envio das notificações de recorrencias no dia anterior, referente a recorrencias do dia: {}",
                    dataProximoDia
            );

            do {
                parcelasRecorrenciaPage = repository.consultaTransacoesProximoDia(
                    dataProximoDia,
                    List.of(TipoStatusEnum.PENDENTE, TipoStatusEnum.CRIADO),
                    PageRequest.of(paginaAtual, tamanhoDaConsulta)
                );

                if (parcelasRecorrenciaPage.isEmpty()) {
                    break;
                }

                log.debug("Processando {} parcelas na página {}. Total de parcelas encontradas = {}",
                        parcelasRecorrenciaPage.getNumberOfElements(), paginaAtual, parcelasRecorrenciaPage.getTotalElements()
                );

                parcelasRecorrenciaPage.getContent().stream()
                    .filter(recorrenciaTransacao -> !recorrenciaTransacao.getNotificadoDiaAnterior())
                    .collect(Collectors.groupingBy(
                        parcelaRecorrencia -> parcelaRecorrencia.getRecorrencia().getPagador()
                )).forEach((pagador, parcelasRecorrencia) -> {
                    try {
                        Recorrencia recorrencia = parcelasRecorrencia.getFirst().getRecorrencia();
                        log.debug("Processando notificação para pagador, agencia pagador: {}, conta pagador: {}, oidRecorrenciaPagador: {}, recorrencia: {}",
                                pagador.getAgencia(), pagador.getConta(), pagador.getOidPagador(), recorrencia.getIdRecorrencia()
                        );

                        MDC.put(RecorrenciaMdc.CONTA_PAGADOR.getChave(), pagador.getConta());
                        MDC.put(RecorrenciaMdc.AGENCIA_PAGADOR.getChave(), pagador.getAgencia());
                        MDC.put(RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA.getChave(), recorrencia.getIdRecorrencia());
                        MDC.put(RecorrenciaMdc.OID_RECORRENCIA_PAGADOR.getChave(), pagador.getOidPagador().toString());

                        boolean pagadorNaoNotificado = pagadoresNotificados.isEmpty() || !pagadoresNotificados.contains(pagador);
                        if (pagadorNaoNotificado) {
                            var informacoesAdicionais = List.of(
                                    NotificacaoDTO.InformacaoAdicional.of(DATA_PARCELA, SystemDateUtil.formatarData(dataProximoDia)),
                                    NotificacaoDTO.InformacaoAdicional.of(DOCUMENTO_PAGADOR, pagador.getCpfCnpj())
                            );

                            var notificacao = NotificacaoDTO.builder()
                                    .agencia(pagador.getAgencia())
                                    .conta(pagador.getConta())
                                    .canal(NotificacaoUtils.converterCanalParaNotificacao(recorrencia.getTipoCanal(), recorrencia.getTipoOrigemSistema()))
                                    .operacao(NotificacaoDTO.TipoTemplate.RECORRENCIA_VENCIMENTO_PROXIMO_DIA)
                                    .informacoesAdicionais(informacoesAdicionais)
                                    .build();

                            producer.enviarNotificacao(notificacao);
                        }

                        List<Long> listaOidRecorrenciaTransacao = parcelasRecorrencia.stream()
                                .map(RecorrenciaTransacao::getOidRecorrenciaTransacao)
                                .toList();

                        repository.atualizaStatusNotificacaoRecorrenciaTransacao(
                            Boolean.TRUE,
                            LocalDateTime.now(),
                            listaOidRecorrenciaTransacao
                        );

                        pagadoresNotificados.add(pagador);
                    } catch (Exception e) {
                        // Abafando qualquer exceção para evitar encerramento do processo
                        log.error(
                                "Erro ao enviar push de notificação para pagador, agencia pagador: {}, conta pagador: {}, oidRecorrenciaPagador: {} ",
                                pagador.getAgencia(),
                                pagador.getConta(),
                                pagador.getOidPagador(),
                                e
                        );
                    } finally {
                        MDC.remove(RecorrenciaMdc.CONTA_PAGADOR.getChave());
                        MDC.remove(RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA.getChave());
                        MDC.remove(RecorrenciaMdc.AGENCIA_PAGADOR.getChave());
                        MDC.remove(RecorrenciaMdc.OID_RECORRENCIA_PAGADOR.getChave());
                    }
                });
                paginaAtual++;
            } while (parcelasRecorrenciaPage.hasNext());
            log.info("Job responsável por disparar o envio das notificações de recorrencias no dia anterior foi concluído com sucesso");
        });
    }


}
