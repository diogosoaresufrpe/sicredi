package io.sicredi.spirecorrencia.api.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import io.sicredi.spirecorrencia.api.notificacao.NotificacaoDTO;
import org.apache.kafka.clients.consumer.Consumer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;

public record TestConstants(
) {
    public static final String RECURSO_NAO_ENCONTRADO = "Recurso não encontrado para os dados informados";
    public static final String CPF_RECEBEDOR = "01234567890";
    public static final String CPF_PAGADOR = "12345678900";
    public static final String MSG_RECORRENCIA_NAO_LOCALIZADA = "Recorrencia não localizada";
    public static final String ISPB = "00000000";
    public static final String TOPICO_AGENDADO_RECORRENTE_TRANSACIONAL = "agendado-recorrente-transacional-cadastro-protocolo-v1";
    public static final String TOPICO_AGENDADO_RECORRENTE = "agendado-recorrente-cadastro-protocolo-v1";
    public static final String INFO_USUARIOS_PARCELA = "Informacoes Entre Usuarios Parcela";
    public static final String ID_CONCILIACAO = "idConciliacaoRecebedor";
    public static final String INFO_USUARIOS = "Informacoes entre usuarios";
    public static final String PATH_RECORRENCIA_CADASTRO = "/v1/recorrencias/cadastro";
    public static final String PATH_IDENTIFICADOR_TRANSACAO = "{identificadorTransacao}";
    public static final String PATH_CODIGO_TIPO_TRANSACAO = "{codigoTipoTransacao}";
    public static final String URL_CONSULTA_PROTOCOLO = "/v3/{codigoTipoTransacao}/{identificadorTransacao}";
    public static final String TOPICO_RETORNO_TRANSACAO = "spi-opag-recebimento-retorno-transacao-v1";
    public static final String PATH_EMITIR_CANCELAMENTO = "/v1/recorrencias/cancelamento";

    public static NotificacaoDTO getNotificacaoDTO(Consumer<String, String> consumer, String topicoNotificacao) {
        try {
            var response = KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(10), 1);

            for (org.apache.kafka.clients.consumer.ConsumerRecord<String, String> iteration : response.records(topicoNotificacao)) {
                if(iteration.value() != null) {
                    return ObjectMapperUtil.converterStringParaObjeto(iteration.value(), new TypeReference<>() {
                    });
                }
            }
            return NotificacaoDTO.builder().build();
        } catch (Exception ignore) {
            return NotificacaoDTO.builder().build();
        }
    }
}
