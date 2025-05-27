package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exclusao.AtualizacaoTitularidadeDTO.HoldersUpdateEventMdc;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spirecorrencia.api.utils.RecorrenciaMdc;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static io.sicredi.spirecorrencia.api.exclusao.AtualizacaoTitularidadeDTO.HoldersUpdateEventMdc.CUSTOMER_ID;

@Slf4j
@Component
@RequiredArgsConstructor
class ExclusaoTitularidadeContaConsumer {

    private static final String ACAO_REMOVER = "REMOVE";
    private static final Set<TipoRecorrencia> TIPOS_RECORRENCIA = Set.of(TipoRecorrencia.AGENDADO_RECORRENTE, TipoRecorrencia.AGENDADO);
    private static final Set<TipoStatusEnum> STATUS_RECORRENCIAS_ATIVAS = Set.of(TipoStatusEnum.CRIADO, TipoStatusEnum.PENDENTE);

    private final AppConfig appConfig;
    private final RecorrenciaRepository recorrenciaRepository;
    private final ObservabilidadeDecorator observabilidadeDecorator;
    private final ProcessamentoExclusaoService processamentoExclusaoService;

    @KafkaListener(
            topics = "#{@appConfig.kafka.consumer.holdersMaintenance.nome}",
            concurrency = "#{@appConfig.kafka.consumer.holdersMaintenance.concurrency}",
            groupId = "#{@appConfig.kafka.consumer.holdersMaintenance.groupId}",
            containerFactory = "holdersMaintenanceContainerFactory"
    )
    public void consumir(ConsumerRecord<String, String> consumerRecord, Acknowledgment acknowledgment) {
        var eventoAtualizacaoTitulares = ObjectMapperUtil.converterStringParaObjeto(consumerRecord.value(), new TypeReference<AtualizacaoTitularidadeDTO>() {
        });
        var atributos = Map.of(HoldersUpdateEventMdc.OPERATION_ID, eventoAtualizacaoTitulares.idOperacao());

        observabilidadeDecorator.executar(atributos, () -> {
            log.info("Realizando consumo de atualização de titulares de conta. operationId={}", eventoAtualizacaoTitulares.idOperacao());
            var titularesRemovidos = Optional.ofNullable(eventoAtualizacaoTitulares.titularesParaAtualizar())
                    .map(titulares -> titulares.stream()
                            .filter(titular -> ACAO_REMOVER.equals(titular.acao()) && titular.concluido())
                            .toList())
                    .orElse(Collections.emptyList());

            if (CollectionUtils.isEmpty(titularesRemovidos)) {
                log.info("Concluído consumo de atualização de titulares de conta. Nenhum titular foi marcado para exclusão. operationId={}", eventoAtualizacaoTitulares.idOperacao());
                acknowledgment.acknowledge();
                return;
            }

            titularesRemovidos.forEach(titular -> {
                var atributosTitular = Map.of(CUSTOMER_ID, titular.idAssociado());
                observabilidadeDecorator.executar(atributosTitular, () -> {
                    int paginaAtual = 0;
                    int tamanhoDaConsulta = appConfig.getHoldersMaintenance().getTamanhoDaConsulta();
                    Page<Recorrencia> recorrencias;

                    do {
                        recorrencias = recorrenciaRepository.consultarPorDadosPagador(titular.cpfCnpj(),
                                eventoAtualizacaoTitulares.dadosConta().cooperativa(),
                                eventoAtualizacaoTitulares.dadosConta().conta(),
                                STATUS_RECORRENCIAS_ATIVAS,
                                TIPOS_RECORRENCIA,
                                PageRequest.of(paginaAtual, tamanhoDaConsulta));

                        recorrencias.forEach(recorrencia -> {
                            var identificadorTransacao = UUID.randomUUID().toString();
                            var atributosRecorrencia = Map.of(
                                    RecorrenciaMdc.IDENTIFICADOR_RECORRENCIA, recorrencia.getIdRecorrencia(),
                                    RecorrenciaMdc.OID_RECORRENCIA_PAGADOR, recorrencia.getPagador().getOidPagador().toString(),
                                    RecorrenciaMdc.IDENTIFICADOR_TRANSACAO, identificadorTransacao
                            );
                            observabilidadeDecorator.executar(atributosRecorrencia, () -> {
                                log.info("Emitindo protocolo para exclusão da recorrência.");
                                processamentoExclusaoService.emitirProtocoloExclusaoTotal(identificadorTransacao, recorrencia);
                                log.info("Protocolo para exclusão de recorrência emitido.");
                            });
                        });

                        paginaAtual++;
                    } while (recorrencias.hasNext());
                });
            });

            acknowledgment.acknowledge();
            log.info("Concluído consumo de atualização de titulares de conta. operationId={}", eventoAtualizacaoTitulares.idOperacao());
        });
    }

}
