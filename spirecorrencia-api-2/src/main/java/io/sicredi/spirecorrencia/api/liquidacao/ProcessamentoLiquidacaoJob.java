package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoCanalEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.micrometer.observation.annotation.Observed;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.ErroLiquidacaoException;
import io.sicredi.spirecorrencia.api.exclusao.ProcessamentoExclusaoService;
import io.sicredi.spirecorrencia.api.metrica.MetricaCounter;
import io.sicredi.spirecorrencia.api.metrica.RegistraMetricaService;
import io.sicredi.spirecorrencia.api.metrica.TipoRetornoTransacaoEnum;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.LockProviderToUse;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.MDC;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.SPIRECORRENCIA_BU0027;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.TipoTemplate.RECORRENCIA_FALHA_OPERACIONAL;

@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
class ProcessamentoLiquidacaoJob {

    private static final Map<TipoProcessamentoEnum, TipoRetornoTransacaoEnum> TIPO_RESULTADO_PROCESSAMENTO = Map.of(
            TipoProcessamentoEnum.EXCLUSAO_PARCIAL, TipoRetornoTransacaoEnum.SUCESSO_EXCLUSAO,
            TipoProcessamentoEnum.EXCLUSAO_TOTAL, TipoRetornoTransacaoEnum.SUCESSO_EXCLUSAO,
            TipoProcessamentoEnum.LIQUIDACAO, TipoRetornoTransacaoEnum.SUCESSO_LIQUIDACAO
    );

    private final AppConfig appConfig;
    private final LiquidacaoService liquidacaoService;
    private final RecorrenciaTransacaoRepository repository;
    private final RegistraMetricaService registraMetricaService;
    private final ProcessamentoExclusaoService processamentoExclusaoService;
    private final ProcessamentoLiquidacaoService processamentoLiquidacaoService;
    private final TentativaRecorrenciaTransacaoService tentativaRecorrenciaTransacaoService;

    @LockProviderToUse("configLockProvider")
    @SchedulerLock(
            name = "#{@appConfig.jobProcessamentoLiquidacao.nomeJob}",
            lockAtMostFor = "#{@appConfig.jobProcessamentoLiquidacao.lockAtMostFor}",
            lockAtLeastFor = "#{@appConfig.jobProcessamentoLiquidacao.lockAtLeastFor}"
    )
    @Observed(name = "#{@appConfig.jobProcessamentoLiquidacao.nomeJob}")
    public void executar() {
        if (!appConfig.getJobProcessamentoLiquidacao().isJobHabilitado()) {
            log.info("Job que executa liquidação de parcelas não habilitado.");
            return;
        }

        Slice<RecorrenciaTransacao> recorrenciaTransacaoSlice;
        log.info("Executando job que processa as recorrências do dia.");

        int paginaAtual = 0;
        int tamanhoDaConsulta = appConfig.getJobProcessamentoLiquidacao().getTamanhoDaConsulta();
        var sort = Sort.by(Sort.Direction.ASC, "dataCriacaoRegistro");

        do {
            recorrenciaTransacaoSlice = repository.buscarParcelasParaProcessamento(LocalDate.now(), PageRequest.of(paginaAtual, tamanhoDaConsulta, sort));

            log.info("Processando página {} com {} recorrencias", paginaAtual, recorrenciaTransacaoSlice.getNumberOfElements());

            recorrenciaTransacaoSlice
                    .getContent()
                        .forEach(this::processarRecorrencia);

            paginaAtual++;
        } while (recorrenciaTransacaoSlice.hasNext());

        log.info("Job que processa as recorrências do dia processado com sucesso.");
    }

    private void processarRecorrencia(RecorrenciaTransacao recorrenciaTransacao) {
        var identificadorTransacao = UUID.randomUUID().toString();

        try {
            var pagador = recorrenciaTransacao.getRecorrencia().getPagador();
            MDC.put(RecorrenciaMdc.IDENTIFICADOR_PARCELA.getChave(), recorrenciaTransacao.getIdParcela());
            MDC.put(RecorrenciaMdc.CONTA_PAGADOR.getChave(), pagador.getConta());
            MDC.put(RecorrenciaMdc.AGENCIA_PAGADOR.getChave(), pagador.getAgencia());
            MDC.put(RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA.getChave(), recorrenciaTransacao.getRecorrencia().getIdRecorrencia());
            MDC.put(RecorrenciaMdc.IDENTIFICADOR_TRANSACAO.getChave(), identificadorTransacao);

            if(TipoStatusEnum.CRIADO != recorrenciaTransacao.getTpoStatus()){
                log.debug("Ignorando recorrência com status diferente de CRIADO = {}", recorrenciaTransacao.getOidRecorrenciaTransacao());
                return;
            }

            log.debug("Processando recorrência com oidRecorrenciaTransacao = {}", recorrenciaTransacao.getOidRecorrenciaTransacao());

            var tipoProcessamentoDTO = liquidacaoService.consultarTipoProcessamento(identificadorTransacao, recorrenciaTransacao);
            log.debug("Iniciando processamento do tipo {} para recorrência com oidRecorrenciaTransacao = {}, idFimAFim = {}", tipoProcessamentoDTO.getTipoProcessamentoEnum().name(), recorrenciaTransacao.getOidRecorrenciaTransacao(), recorrenciaTransacao.getIdFimAFim());

            if (tipoProcessamentoDTO.getTipoProcessamentoEnum() == TipoProcessamentoEnum.IGNORADA) {
                registrarTentativa(tipoProcessamentoDTO);
                log.info("Liquidação da parcela ignorada. A mesma ficará disponível para retry. Motivo: {}. {}",
                        tipoProcessamentoDTO.getTipoProcessamentoErro().getCodigoErro(),
                        tipoProcessamentoDTO.getTipoProcessamentoErro().getMensagemErro());
                return;
            }

            switch (tipoProcessamentoDTO.getTipoProcessamentoEnum()) {
                case LIQUIDACAO -> processamentoLiquidacaoService.processarLiquidacao(tipoProcessamentoDTO);
                case EXCLUSAO_TOTAL -> processamentoExclusaoService.processarExclusaoTotal(tipoProcessamentoDTO);
                case EXCLUSAO_PARCIAL -> processamentoExclusaoService.processarExclusaoParcial(tipoProcessamentoDTO);
            }

            log.debug("Processamento da recorrência com oidRecorrenciaTransacao = {}, idFimAFim = {} realizada com sucesso.", recorrenciaTransacao.getOidRecorrenciaTransacao(), recorrenciaTransacao.getIdFimAFim());

            registrarMetricaSucesso(tipoProcessamentoDTO);

        } catch (ErroLiquidacaoException ex) {
            var deveProcessarExclusaoParcial = deveProcessarExclusaoParcial(recorrenciaTransacao.getRecorrencia().getTipoCanal());

            log.error("Erro durante o processamento da liquidação. deveProcessarExclusaoParcial = {} oidRecorrenciaTransacao = {} idFimAFim = {}", deveProcessarExclusaoParcial, recorrenciaTransacao.getOidRecorrenciaTransacao(), recorrenciaTransacao.getIdFimAFim(), ex);

            if(deveProcessarExclusaoParcial){
                var tipoProcessamentoErro = TipoProcessamentoWrapperDTO.criarTipoExclusaoParcial(
                        identificadorTransacao,
                        recorrenciaTransacao,
                        SPIRECORRENCIA_BU0027.getCode(),
                        SPIRECORRENCIA_BU0027.getMessage(),
                        TipoMotivoExclusao.SOLICITADO_SISTEMA,
                        RECORRENCIA_FALHA_OPERACIONAL
                );

                try {
                    processamentoExclusaoService.processarExclusaoParcial(tipoProcessamentoErro);

                    registrarMetrica(recorrenciaTransacao.getRecorrencia(), TipoRetornoTransacaoEnum.ERRO, ex.getClass().getSimpleName());
                } catch (TechnicalException exception) {
                    log.error("Erro durante o processamento da liquidação. oidRecorrenciaTransacao = {} idFimAFim = {}", recorrenciaTransacao.getOidRecorrenciaTransacao(), recorrenciaTransacao.getIdFimAFim(), exception);

                    registrarMetrica(recorrenciaTransacao.getRecorrencia(), TipoRetornoTransacaoEnum.ERRO, exception.getClass().getSimpleName());
                }
            } else {
                registrarMetrica(recorrenciaTransacao.getRecorrencia(), TipoRetornoTransacaoEnum.ERRO, ex.getClass().getSimpleName());
            }
        } catch (TechnicalException ex) {
            log.error("Erro durante o processamento da liquidação. oidRecorrenciaTransacao = {} idFimAFim = {}", recorrenciaTransacao.getOidRecorrenciaTransacao(), recorrenciaTransacao.getIdFimAFim(), ex);

            registrarMetrica(recorrenciaTransacao.getRecorrencia(), TipoRetornoTransacaoEnum.ERRO, ex.getClass().getSimpleName());
        }  finally {
            MDC.clear();
        }
    }

    private void registrarTentativa(TipoProcessamentoWrapperDTO tipoProcessamentoWrapperDTO) {
        var recorrenciaTransacao = tipoProcessamentoWrapperDTO.getRecorrenciaTransacao();

        tentativaRecorrenciaTransacaoService.registrarRecorrenciaTransacaoTentativa(
                tipoProcessamentoWrapperDTO.getTipoProcessamentoErro().getMensagemErro(),
                tipoProcessamentoWrapperDTO.getTipoProcessamentoErro().getCodigoErro(),
                recorrenciaTransacao
        );
    }

    private boolean deveProcessarExclusaoParcial(TipoCanalEnum tipoCanal) {
        var dataHoraLimiteTentativa = appConfig.getRegras().getProcessamento().getHorarioLimiteLiquidacao();
        return LocalTime.now().isAfter(dataHoraLimiteTentativa) || tipoCanal == TipoCanalEnum.WEB_OPENBK;
    }

    private void registrarMetricaSucesso(TipoProcessamentoWrapperDTO tipoProcessamentoDTO) {
        var recorrencia = tipoProcessamentoDTO.getRecorrenciaTransacao().getRecorrencia();
        var tipoRetorno = TIPO_RESULTADO_PROCESSAMENTO.getOrDefault(tipoProcessamentoDTO.getTipoProcessamentoEnum(), TipoRetornoTransacaoEnum.SUCESSO_LIQUIDACAO);

        var isProcessamentoExclusao = TipoProcessamentoEnum.EXCLUSAO_TOTAL == tipoProcessamentoDTO.getTipoProcessamentoEnum() || TipoProcessamentoEnum.EXCLUSAO_PARCIAL == tipoProcessamentoDTO.getTipoProcessamentoEnum();
        var mensagem = isProcessamentoExclusao ? tipoProcessamentoDTO.getTipoProcessamentoErro().getCodigoErro() : null;

        registrarMetrica(recorrencia, tipoRetorno, mensagem);
    }

    private void registrarMetrica(Recorrencia recorrencia, TipoRetornoTransacaoEnum tipoRetornoTransacaoEnum, String mensagem) {
        var metrica = new MetricaCounter(
                "pix_emissao_liquidacao_parcela_recorrencia",
                "Resultado do processamento da emissão de uma liquidação de parcela recorrente do Pix."
        ).adicionarTag("tipo_retorno", tipoRetornoTransacaoEnum.name())
                .adicionarTag("mensagem", mensagem)
                .adicionarTag("tipo_canal", Optional.of(recorrencia.getTipoCanal().getTipoCanalPix().name()).orElse(null));

        registraMetricaService.registrar(metrica);
    }
}
