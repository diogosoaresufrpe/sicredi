package io.sicredi.spirecorrencia.api.automatico.autorizacao;

import br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11;
import br.com.sicredi.spi.util.SpiUtil;
import br.com.sicredi.spi.util.type.TipoId;
import io.sicredi.spirecorrencia.api.automatico.SolicitacaoAutorizacaoRecorrenciaRepository;
import io.sicredi.spirecorrencia.api.automatico.enums.*;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteService;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.utils.PaginadorUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.DOCUMENTO_PAGADOR;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.NOME_RECEBEDOR;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessarConfirmacaoAutorizacaoExpiradasService {

    private final AppConfig appConfig;
    private final IdempotenteService idempotenteService;
    private final RecorrenciaAutorizacaoRepository recorrenciaAutorizacaoRepository;
    private final RecorrenciaAutorizacaoCancelamentoRepository recorrenciaAutorizacaoCancelamentoRepository;
    private final SolicitacaoAutorizacaoRecorrenciaRepository solicitacaoAutorizacaoRecorrenciaRepository;
    private final NotificacaoProducer producer;
    private final TransactionTemplate transactionTemplate;

    public void processarAutorizacoesExpiradas(int tamanhoPagina, int minutosExpiracao) {
        var dataExpiracao = LocalDateTime.now().minusMinutes(minutosExpiracao);

        PaginadorUtil.paginar(tamanhoPagina, (pagina, tamanho) -> recorrenciaAutorizacaoRepository
                        .buscarRecorrenciaAutorizacaoPorStatusEDataCriacaoAntesDe(
                                TipoStatusAutorizacao.CRIADA, dataExpiracao, PageRequest.of(pagina, tamanho)),
                this::processarConfirmacaoAutorizacao
        );
    }

    private void processarConfirmacaoAutorizacao(RecorrenciaAutorizacao autorizacao) {
        log.info("Processando confirmacao de autorização: oidRecorrenciaAutorizacao: [{}]", autorizacao.getOidRecorrenciaAutorizacao());

        try {
            var dataAtual = LocalDateTime.now();
            var reenvioOperacaoHabilitado = appConfig.getRegras().getProcessamento().isReenvioOperacaoHabilitado();

            if (expirouConfirmacao(autorizacao, dataAtual)) {
                cancelar(autorizacao, dataAtual);
            } else if (reenvioOperacaoHabilitado) {
                reenviarOperacao(autorizacao);
            }
        } catch (Exception e) {
            log.error("Erro ao realizar o processamento da confirmacao de autorização: oidRecorrenciaAutorizacao: [{}]",
                    autorizacao.getOidRecorrenciaAutorizacao(), e);
        }
    }

    private void cancelar(RecorrenciaAutorizacao autorizacao, LocalDateTime dataAtual) {
        transactionTemplate.execute(status -> {
            log.info("Cancelando confirmação de autorização, oidRecorrenciaAutorizacao: [{}] por expiração de prazo.",
                    autorizacao.getOidRecorrenciaAutorizacao());

            atualizarRecorrenciaAutorizacao(autorizacao);
            salvarRecorrenciaAutorizacaoCancelamento(autorizacao, dataAtual);
            atualizarSolicitacaoAutorizacaoRecorrenciaSeJornada1(autorizacao, dataAtual);
            enviarNotificacaoPush(autorizacao);
            return Boolean.TRUE;
        });
    }

    private void reenviarOperacao(RecorrenciaAutorizacao autorizacao) {
        log.info("Realizando o reenvio da operacao oidRecorrenciaAutorizacao: [{}], jornada: [{}]",
                autorizacao.getOidRecorrenciaAutorizacao(), autorizacao.getTipoJornada());

        var topico = appConfig.getKafka().getProducer().getIcomPainEnvio().getTopico();
        var idInformacaoStatusEnvio = autorizacao.getIdInformacaoStatusEnvio();
        var chaveIdempotencia = Optional.of(autorizacao.getTipoJornada())
                .filter(TipoJornada.JORNADA_1.name()::equalsIgnoreCase)
                .map(s -> idInformacaoStatusEnvio.concat("_JORNADA1B"))
                .orElseGet(() -> criarChaveIdempotencia(idInformacaoStatusEnvio));

        idempotenteService.reenviarOperacao(topico, chaveIdempotencia);
    }

    private void atualizarRecorrenciaAutorizacao(RecorrenciaAutorizacao autorizacao) {
        log.debug("Atualizar status para cancelada da recorrencia autorização, oidRecorrenciaAutorizacao: [{}].",
                autorizacao.getOidRecorrenciaAutorizacao());
        recorrenciaAutorizacaoRepository.atualizarRecorrenciaAutorizacaoPorTipoStatusESubStatus(autorizacao.getOidRecorrenciaAutorizacao(),
                TipoStatusAutorizacao.CANCELADA, null);
    }

    private void salvarRecorrenciaAutorizacaoCancelamento(RecorrenciaAutorizacao autorizacao, LocalDateTime dataAtual) {
        log.debug("Salvando recorrencia autorização cancelamento do oidRecorrenciaAutorizacao: [{}], idRecorrencia: [{}].",
                autorizacao.getOidRecorrenciaAutorizacao(), autorizacao.getIdRecorrencia());

        var cancelamento = RecorrenciaAutorizacaoCancelamento.builder()
                .tipoSolicitanteCancelamento(TipoSolicitanteCancelamento.PAGADOR)
                .idInformacaoCancelamento(SpiUtil.gerarIdFimAFim(TipoId.MENSAGEM_CANCELAMENTO_AUTORIZACAO))
                .idRecorrencia(autorizacao.getIdRecorrencia())
                .tipoCancelamento(TipoCancelamento.RECORRENCIA_AUTORIZACAO)
                .motivoCancelamento(MotivoCancelamentoPain11.AUSENCIA_RESPOSTA_PSP_RECEBEDOR.getXmlValue())
                .dataCancelamento(dataAtual)
                .tipoStatus(TipoStatusCancelamentoAutorizacao.ACEITA)
                .cpfCnpjSolicitanteCancelamento(autorizacao.getCpfCnpjPagador())
                .build();

        recorrenciaAutorizacaoCancelamentoRepository.save(cancelamento);
    }

    private void atualizarSolicitacaoAutorizacaoRecorrenciaSeJornada1(RecorrenciaAutorizacao autorizacao, LocalDateTime dataAtual) {

        if (TipoJornada.JORNADA_1.name().equalsIgnoreCase(autorizacao.getTipoJornada())) {
            log.debug("Atualizando solicitação para autorização oidRecorrenciaAutorizacao: [{}], recorrência [{}].",
                    autorizacao.getOidRecorrenciaAutorizacao(), autorizacao.getIdRecorrencia());

            solicitacaoAutorizacaoRecorrenciaRepository.atualizarTipoStatusESubStatusPorIdRecorrenciaETipoStatusAtual(
                    autorizacao.getIdRecorrencia(),
                    TipoStatusSolicitacaoAutorizacao.PENDENTE_CONFIRMACAO,
                    null,
                    dataAtual,
                    TipoStatusSolicitacaoAutorizacao.CONFIRMADA);
        }
    }

    private void enviarNotificacaoPush(RecorrenciaAutorizacao autorizacao) {
        log.debug("Enviando notificação push para autorização, oidRecorrenciaAutorizacao: [{}], recorrência [{}].",
                autorizacao.getOidRecorrenciaAutorizacao(), autorizacao.getIdRecorrencia());

        var informacoesAdicionais = List.of(
                NotificacaoDTO.InformacaoAdicional.of(DOCUMENTO_PAGADOR, autorizacao.getCpfCnpjPagador()),
                NotificacaoDTO.InformacaoAdicional.of(NOME_RECEBEDOR, autorizacao.getNomeRecebedor())
        );

        var notificacao = NotificacaoDTO.builder()
                .agencia(autorizacao.getAgenciaPagador())
                .conta(autorizacao.getContaPagador())
                .canal(NotificacaoUtils.converterCanalParaNotificacao(
                        autorizacao.getTipoCanalPagador(), autorizacao.getTipoSistemaPagador()))
                .operacao(NotificacaoDTO.TipoTemplate.AUTOMATICO_AUTORIZACAO_CONFIRMADA_PAGADOR_FALHA_NAO_RESPONDIDA_OU_CANCELADA_RECEBEDOR)
                .informacoesAdicionais(informacoesAdicionais)
                .build();

        producer.enviarNotificacao(notificacao);
    }

    private String criarChaveIdempotencia(String identificadorTransacao) {
        String tipoOperacao = "CADASTRO";
        String tipoJornada = "AUTN";

        return identificadorTransacao
                .concat("_")
                .concat(tipoOperacao)
                .concat("_")
                .concat(tipoJornada);
    }

    private boolean expirouConfirmacao(RecorrenciaAutorizacao autorizacao, LocalDateTime dataAtual) {
        var periodoConfirmacao = Duration.between(autorizacao.getDataInicioConfirmacao(), dataAtual);
        return periodoConfirmacao.toHours() >= appConfig.getRegras().getProcessamento().getLimiteExpiracaoHoras();
    }
}
