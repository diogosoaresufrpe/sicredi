package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSolicitanteCancelamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoAutorizacao;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoRepository;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.Objects.*;

@Slf4j
@Service
@RequiredArgsConstructor
class ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoServiceImpl implements ConfirmacaoCancelamentoAposExpiracaoPixAutomaticoService {

    private static final int PAGE_SIZE = 1000; // TODO: Externalizar para application config
    private static final List<String> STATUS_CANCELAMENTO_VALIDOS = List.of(
            TipoStatusCancelamentoAutorizacao.CRIADA.name(),
            TipoStatusCancelamentoAutorizacao.ENVIADA.name()
    );

    private final RecorrenciaInstrucaoPagamentoCancelamentoRepository recorrenciaInstrucaoPagamentoCancelamentoRepository;
    private final RecorrenciaInstrucaoPagamentoRepository recorrenciaInstrucaoPagamentoRepository;
    private final RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;
    private final TransactionTemplate transactionTemplate;

    @Override
    public void atualizarConfirmacaoCancelamentoPixAutomaticoJobService() {
        var dataLimite = LocalDateTime.now().minusHours(12);

        IntStream.iterate(0, page -> page + 1)
                .mapToObj(pagina -> PageRequest.of(pagina, PAGE_SIZE))
                .map(page -> recorrenciaInstrucaoPagamentoCancelamentoRepository.getListaCodFimAFimByListStatusAndTipoPSPSolicitanteCancelamento(
                        page,
                        TipoSolicitanteCancelamento.PAGADOR.name(),
                        STATUS_CANCELAMENTO_VALIDOS,
                        dataLimite
                ))
                .takeWhile(Slice::hasContent)
                .forEach(page -> {
                    page.getContent().forEach(res -> log.info("Logando o id da transacao: {}", res));
                    atualizarStatusCancelamento(page.getContent());
                });
    }

    private void atualizarStatusCancelamento(List<String> listCodigosFimAFim) {
        transactionTemplate.execute(status -> {
            listCodigosFimAFim.stream().forEach(id -> log.info(id));

            recorrenciaInstrucaoPagamentoRepository.atualizaStatusByListaCodFimAFim(listCodigosFimAFim, TipoStatusAutorizacao.CANCELADA.name());
            recorrenciaTransacaoRepository.atualizaStatusByCodigoFimAFim(listCodigosFimAFim, TipoStatusEnum.EXCLUIDO, TipoMotivoExclusao.SOLICITADO_INICIADORA);
            recorrenciaInstrucaoPagamentoCancelamentoRepository.atualizaStatusByListaCodFimAFim(listCodigosFimAFim, TipoStatusCancelamentoAutorizacao.ACEITA.name());
            return null;
        });
    }
}

