package io.sicredi.spirecorrencia.api.exclusao;

import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoRecorrencia;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaRepository;
import io.sicredi.spirecorrencia.api.utils.ObjectMapperUtil;
import io.sicredi.spiutils.core.lib.commons.observabilidade.tracing.ObservabilidadeDecorator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.support.Acknowledgment;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.RecorrenciaConstantes.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
final class ExclusaoTitularidadeContaConsumerTest {

    private static final String ACAO_REMOVER = "REMOVE";
    private static final String TOPICO_HOLDERS_MAINTENANCE = "holders-maintenance-v1";
    private static final Set<TipoRecorrencia> TIPOS_RECORRENCIA = Set.of(TipoRecorrencia.AGENDADO_RECORRENTE, TipoRecorrencia.AGENDADO);
    private static final Set<TipoStatusEnum> STATUS_RECORRENCIAS_ATIVAS = Set.of(TipoStatusEnum.CRIADO, TipoStatusEnum.PENDENTE);

    @Mock
    private AppConfig appConfig;

    @Mock
    private Acknowledgment acknowledgment;

    @Mock
    private RecorrenciaRepository recorrenciaRepository;

    @Mock
    private ProcessamentoExclusaoService processamentoExclusaoService;

    private ExclusaoTitularidadeContaConsumer exclusaoTitularidadeContaConsumer;

    @BeforeEach
    void setUp() {
        ObservabilidadeDecorator observabilidadeDecorator = new ObservabilidadeDecorator(mock());
        exclusaoTitularidadeContaConsumer = new ExclusaoTitularidadeContaConsumer(appConfig, recorrenciaRepository, observabilidadeDecorator, processamentoExclusaoService);
    }

    @Test
    void dadoEventoAtualizacaoSemTitularesParaRemocao_quandoConsumir_deveIgnorarProcessamento() {
        var payload = ObjectMapperUtil.converterObjetoParaString(AtualizacaoTitularidadeDTO.builder()
                .idOperacao(UUID.randomUUID().toString())
                .titularesParaAtualizar(List.of(
                        AtualizacaoTitularidadeDTO.HolderToUpdateDTO.builder()
                                .acao("UPDATE")
                                .build()
                ))
                .build());
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(TOPICO_HOLDERS_MAINTENANCE, 0, 0, null, payload);

        exclusaoTitularidadeContaConsumer.consumir(consumerRecord, acknowledgment);

        verify(acknowledgment).acknowledge();
        verifyNoInteractions(recorrenciaRepository, processamentoExclusaoService, appConfig);
    }

    @Test
    void dadoTitularSemRecorrenciasCadastradas_quandoConsumir_deveIgnorarProcessamento() {
        var payload = ObjectMapperUtil.converterObjetoParaString(AtualizacaoTitularidadeDTO.builder()
                .idOperacao(UUID.randomUUID().toString())
                .dadosConta(AtualizacaoTitularidadeDTO.AccountDTO.builder()
                        .cooperativa(AGENCIA)
                        .conta(CONTA)
                        .build())
                .titularesParaAtualizar(List.of(
                        AtualizacaoTitularidadeDTO.HolderToUpdateDTO.builder()
                                .acao(ACAO_REMOVER)
                                .cpfCnpj(CPF_CNPJ)
                                .concluido(Boolean.TRUE)
                                .idAssociado(UUID.randomUUID().toString())
                                .build()
                ))
                .build());
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>(TOPICO_HOLDERS_MAINTENANCE, 0, 0, null, payload);

        var holdersMaintenanceConfig = new AppConfig.HoldersMaintenance();
        holdersMaintenanceConfig.setTamanhoDaConsulta(10);
        when(appConfig.getHoldersMaintenance()).thenReturn(holdersMaintenanceConfig);
        when(recorrenciaRepository.consultarPorDadosPagador(any(), any(), any(), any(), any(), any())).thenReturn(Page.empty());

        exclusaoTitularidadeContaConsumer.consumir(consumerRecord, acknowledgment);

        verify(acknowledgment).acknowledge();
        verify(appConfig).getHoldersMaintenance();
        verify(recorrenciaRepository).consultarPorDadosPagador(CPF_CNPJ, AGENCIA, CONTA, STATUS_RECORRENCIAS_ATIVAS, TIPOS_RECORRENCIA, PageRequest.of(0, 10));
        verifyNoInteractions(processamentoExclusaoService);
    }

}
