package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoChaveEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import io.sicredi.spirecorrencia.api.liquidacao.TipoExclusaoRecorrencia;
import io.sicredi.spirecorrencia.api.liquidacao.TipoProcessamentoWrapperDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoProducer;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoUtils;
import io.sicredi.spirecorrencia.api.protocolo.ExclusaoRecorrenciaProtocoloRequestBuilder;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.recorrencia_tentativa.TentativaRecorrenciaTransacaoService;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Optional;

import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO.InformacaoAdicional.of;
import static io.sicredi.spirecorrencia.api.notificacao.NotificacaoInformacaoAdicional.*;

@Service
@RequiredArgsConstructor
public class ProcessamentoExclusaoService {

    private final NotificacaoProducer producer;
    private final TentativaRecorrenciaTransacaoService tentativaService;
    private final SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    private final RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;

    void emitirProtocoloExclusaoTotal(String identificadorTransacao, Recorrencia recorrencia) {
        if (recorrencia == null || CollectionUtils.isEmpty(recorrencia.getRecorrencias())) {
            return;
        }
        var parcelasParaExcluir = recorrencia.getRecorrencias().stream()
                .filter(transacao -> TipoStatusEnum.CRIADO == transacao.getTpoStatus())
                .toList();

        var request = ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(identificadorTransacao, recorrencia, parcelasParaExcluir);
        spiCanaisProtocoloApiClient.emitirProtocoloCancelamentoRecorrencia(recorrencia.getTipoCanal(), TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA, request);
    }

    @Transactional
    public void processarExclusaoTotal(TipoProcessamentoWrapperDTO tipoProcessamentoWrapperDTO) {
        try {
            adicionarTentativa(tipoProcessamentoWrapperDTO);

            var recorrenciaTransacao = tipoProcessamentoWrapperDTO.getRecorrenciaTransacao();
            var recorrencia = recorrenciaTransacao.getRecorrencia();
            var parcelasEmAberto = recorrenciaTransacaoRepository.findByRecorrenciaAndStatus(recorrencia.getIdRecorrencia(), List.of(TipoStatusEnum.CRIADO, TipoStatusEnum.PENDENTE));
            var request = ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(
                    tipoProcessamentoWrapperDTO.getIdentificadorTransacao(),
                    recorrencia,
                    parcelasEmAberto
            );
            emitirCancelamento(request, recorrenciaTransacao);
            notificar(recorrenciaTransacao, tipoProcessamentoWrapperDTO.getTemplateNotificacao());
        } catch (Exception ex) {
            throw new TechnicalException(ex);
        }
    }

    @Transactional
    public void processarExclusaoParcial(TipoProcessamentoWrapperDTO tipoProcessamentoWrapperDTO) {
        try {
            adicionarTentativa(tipoProcessamentoWrapperDTO);

            var recorrenciaTransacao = tipoProcessamentoWrapperDTO.getRecorrenciaTransacao();
            var request = ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(
                    tipoProcessamentoWrapperDTO.getIdentificadorTransacao(),
                    recorrenciaTransacao.getRecorrencia(),
                    List.of(recorrenciaTransacao)
            );
            emitirCancelamento(request, recorrenciaTransacao);
            notificar(recorrenciaTransacao, tipoProcessamentoWrapperDTO.getTemplateNotificacao());
        } catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    private void emitirCancelamento(ExclusaoRecorrenciaProtocoloRequest request, RecorrenciaTransacao recorrenciaTransacao) {
        spiCanaisProtocoloApiClient.emitirProtocoloCancelamentoRecorrencia(
                recorrenciaTransacao.getRecorrencia().getTipoCanal(),
                TipoExclusaoRecorrencia.EXCLUSAO_INTEGRADA,
                request
        );
    }

    private void adicionarTentativa(TipoProcessamentoWrapperDTO tipoProcessamentoWrapperDTO) {
        var recorrenciaTransacao = tipoProcessamentoWrapperDTO.getRecorrenciaTransacao();

        tentativaService.registrarRecorrenciaTransacaoTentativa(
                tipoProcessamentoWrapperDTO.getTipoProcessamentoErro().getMensagemErro(),
                tipoProcessamentoWrapperDTO.getTipoProcessamentoErro().getCodigoErro(),
                recorrenciaTransacao
        );
    }

    private void notificar(RecorrenciaTransacao recorrenciaTransacao, NotificacaoDTO.TipoTemplate tipoOperacao) {
        var recorrencia = recorrenciaTransacao.getRecorrencia();
        var recebedor = recorrencia.getRecebedor();
        var pagador = recorrencia.getPagador();

        var informacoesAdicionais = List.of(
                of(NOME_RECEBEDOR, recebedor.getNome()),
                of(DOCUMENTO_RECEBEDOR, recebedor.getCpfCnpj()),
                of(VALOR, String.valueOf(recorrenciaTransacao.getValor())),
                of(DOCUMENTO_PAGADOR, recorrenciaTransacao.getDocumentoPagador())
        );

        var tipoChave = Optional.ofNullable(recebedor.getTipoChave()).map(TipoChaveEnum::name).orElse(null);
        var canal = NotificacaoUtils.converterCanalParaNotificacao(recorrencia.getTipoCanal(), recorrencia.getTipoOrigemSistema());
        var notificacao = NotificacaoDTO.builder()
                .agencia(pagador.getAgencia())
                .conta(pagador.getConta())
                .chave(recebedor.getChave())
                .tipoChave(tipoChave)
                .operacao(tipoOperacao)
                .canal(canal)
                .informacoesAdicionais(informacoesAdicionais)
                .build();

        producer.enviarNotificacao(notificacao);

    }

}
