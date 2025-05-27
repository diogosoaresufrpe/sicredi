package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;

import br.com.sicredi.canaisdigitais.dto.protocolo.ProtocoloDTO;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CancelamentoAgendamentoDebitoTransacaoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.automatico.CancelamentoAutorizacaoRecorrenciaProtocoloRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes;
import br.com.sicredi.spicanais.transacional.transport.lib.validator.group.PrimaryOrder;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.sicredi.spirecorrencia.api.commons.ProtocoloBaseRequest;
import io.sicredi.spirecorrencia.api.idempotente.IdempotenteRequest;
import io.sicredi.spirecorrencia.api.idempotente.TipoResponseIdempotente;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.GroupSequence;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.CadastroAutorizacao.Titles.ID_FIM_A_FIM_PAGAMENTO_IMEDIATO;
import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.*;
import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaTransacao.Schemas.Exemplos.ID_FIM_A_FIM;
import static br.com.sicredi.spicanais.transacional.transport.lib.config.SpiCanaisTransportContantes.Regex.PATTERN_ID_CANCELAMENTO_AGENDAMENTO;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@EqualsAndHashCode(callSuper = false)
@GroupSequence({PrimaryOrder.class, CancelamentoAgendamentoDebitoRequest.class})
public class CancelamentoAgendamentoDebitoRequest extends ProtocoloBaseRequest implements IdempotenteRequest {

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.OID_RECORRENCIA_AUTORIZACAO_NOTNULL)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.OID_RECORRENCIA_AUTORIZACAO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.OID_RECORRENCIA_AUTORIZACAO)
    private Long oidRecorrenciaAutorizacao;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_FIM_A_FIM_NOT_BLANK)
    @Pattern(regexp = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.Regex.PATTERN_ID_FIM_A_FIM, message = ID_FIM_A_FIM_PATTERN, groups = PrimaryOrder.class)
    @Schema(title = ID_FIM_A_FIM_PAGAMENTO_IMEDIATO, example = ID_FIM_A_FIM)
    private String idFimAFim;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.ID_CANCELAMENTO_AGENDAMENTO_NOTBLANK)
    @Pattern(regexp = PATTERN_ID_CANCELAMENTO_AGENDAMENTO, message = ID_CANCELAMENTO_AGENDAMENTO_PATTERN, groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.ID_CANCELAMENTO_AGENDAMENTO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.ID_CANCELAMENTO_AGENDAMENTO)
    private String idCancelamentoAgendamento;

    @NotBlank(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.CPF_CNPJ_SOLICITANTE_NOTBLANK, groups = PrimaryOrder.class)
    @Pattern(regexp = SpiCanaisTransportContantes.Regex.PATTERN_CPF_CNPJ, message = CPF_CNPJ_SOLICITANTE_PATTERN, groups = PrimaryOrder.class)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.CPF_CNPJ_SOLICITANTE)
    private String cpfCnpjSolicitanteCancelamento;

    @NotNull(message = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Validations.MOTIVO_CANCELAMENTO_NOT_BLANK)
    @Schema(title = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Titles.MOTIVO_CANCELAMENTO, example = SpiCanaisTransportContantes.RecorrenciaGestaoConstantes.RecorrenciaAutorizacao.Schemas.Exemplos.MOTIVO_CANCELAMENTO)
    private MotivoCancelamentoCamt55 motivoCancelamento;

    @Override
    public Boolean deveSinalizacaoSucessoProtocolo() {
        return Boolean.FALSE;
    }

    public static CancelamentoAgendamentoDebitoRequest fromEmissaoProtocolo(ProtocoloDTO protocolo, CancelamentoAgendamentoDebitoTransacaoDTO transacao, String identificadorTransacao, String dataHoraInicioCanal, ZonedDateTime dataHoraRecebimentoMensagem) {
        return CancelamentoAgendamentoDebitoRequest.builder()
                .idCancelamentoAgendamento(transacao.getIdCancelamentoAgendamento())
                .idFimAFim(transacao.getIdFimAFim())
                .oidRecorrenciaAutorizacao(transacao.getOidRecorrenciaAutorizacao())
                .cpfCnpjSolicitanteCancelamento(transacao.getCpfCnpjSolicitanteCancelamento())
                .motivoCancelamento(MotivoCancelamentoCamt55.of(transacao.getMotivoCancelamento()))
                .identificadorTransacao(identificadorTransacao)
                .dataHoraInicioCanal(criarDataHoraInicioCanal(dataHoraInicioCanal))
                .protocoloDTO(protocolo)
                .dataHoraRecepcao(dataHoraRecebimentoMensagem)
                .tipoResponse(TipoResponseIdempotente.ORDEM_COM_PROTOCOLO)
                .build();

    }

    private static LocalDateTime criarDataHoraInicioCanal(String dataHoraInicioCanal) {
        return Optional.ofNullable(dataHoraInicioCanal)
                .map(header -> ZonedDateTime.parse(header, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSZ"))
                        .withZoneSameInstant(ZoneId.of("America/Sao_Paulo"))
                        .toLocalDateTime())
                .orElseGet(LocalDateTime::now);
    }
}
