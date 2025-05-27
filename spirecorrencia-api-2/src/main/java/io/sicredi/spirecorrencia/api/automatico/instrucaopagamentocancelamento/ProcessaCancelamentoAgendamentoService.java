package io.sicredi.spirecorrencia.api.automatico.instrucaopagamentocancelamento;


import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.spi.dto.Camt055Dto;
import br.com.sicredi.spi.entities.type.MotivoCancelamentoCamt55;
import br.com.sicredi.spi.entities.type.TipoSolicitacaoCamt55;
import io.sicredi.spirecorrencia.api.automatico.camt.Camt055ResponseFactory;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusCancelamentoInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoStatusInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.enums.TipoSubStatus;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamento;
import io.sicredi.spirecorrencia.api.automatico.instrucaopagamento.RecorrenciaInstrucaoPagamentoService;
import io.sicredi.spirecorrencia.api.config.AppConfig;
import io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode;
import io.sicredi.spirecorrencia.api.idempotente.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

import static br.com.sicredi.spi.entities.type.MotivoCancelamentoPain11.CategoriaCancelamento.PAGADOR;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.*;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessaCancelamentoAgendamentoService {

    private final AppConfig appConfig;
    private final Validator validator;
    private final RecorrenciaInstrucaoPagamentoService recorrenciaInstrucaoPagamentoService;
    private final RecorrenciaInstrucaoPagamentoCancelamentoRepository recorrenciaInstrucaoPagamentoCancelamentoRepository;

    public ErroWrapperDTO<Camt055Dto> processarCancelamento(CancelamentoAgendamentoDebitoRequest cancelamentoRequest) {
        var erroValidacao = validarRequest(cancelamentoRequest);
        if (erroValidacao.isPresent()) {
            log.error("Erro de validação de constraints no cancelamento. Erro: {}", erroValidacao.get().mensagemErro());
            return new ErroWrapperDTO<>(erroValidacao.get());
        }

        var resultadoConsulta = consultaInstrucaoPagamento(cancelamentoRequest);
        if (resultadoConsulta.getErro().isPresent()) {
            log.error("Erro na consulta da instrução de pagamento. Erro: {}", resultadoConsulta.getErro().get().mensagemErro());
            return new ErroWrapperDTO<>(resultadoConsulta.getErro().get());
        }

        var cancelamentoAgendamentoWrapperDTO = CancelamentoAgendamentoWrapperDTO.fromEmissaoProtocolo(
                resultadoConsulta.getObjeto(),
                cancelamentoRequest.getIdCancelamentoAgendamento(),
                cancelamentoRequest.getCpfCnpjSolicitanteCancelamento(),
                cancelamentoRequest.getMotivoCancelamento(),
                cancelamentoRequest.getDataHoraInicioCanal()
        );

        return processarCancelamentoComInstrucao(cancelamentoAgendamentoWrapperDTO);
    }

    public ErroWrapperDTO<Camt055Dto> processarCancelamentoComInstrucao(CancelamentoAgendamentoWrapperDTO wrapperDTO) {
        var erroValidacaoNegocio = validarRegraNegocio(wrapperDTO);
        if (erroValidacaoNegocio.isPresent()) {
            log.error("Erro de validação de negócio no cancelamento. Erro: {}", erroValidacaoNegocio.get().mensagemErro());
            return new ErroWrapperDTO<>(erroValidacaoNegocio.get());
        }

        var erroProcessamento = processarCancelamentoAgendamento(wrapperDTO);
        if (erroProcessamento.getErro().isPresent()) {
            log.error("Erro no processamento do cancelamento. Erro: {}", erroProcessamento.getErro().get().mensagemErro());
            return new ErroWrapperDTO<>(erroProcessamento.getErro().get());
        }

        var instrucaoCancelamentoSalva = erroProcessamento.getObjeto();

        var camt055 = Camt055ResponseFactory.fromInstrucaoPagamento(wrapperDTO.getInstrucaoPagamento(),
                instrucaoCancelamentoSalva.getIdCancelamentoAgendamento(),
                instrucaoCancelamentoSalva.getNumCpfCnpjSolicitanteCancelamento(),
                instrucaoCancelamentoSalva.getCodMotivoCancelamento(),
                instrucaoCancelamentoSalva.getDatCriacaoSolicitacaoCancelamento());

        return new ErroWrapperDTO<>(camt055);
    }

    private ErroWrapperDTO<RecorrenciaInstrucaoPagamento> consultaInstrucaoPagamento(CancelamentoAgendamentoDebitoRequest cancelamentoRequest) {
        var idFImAFim = cancelamentoRequest.getIdFimAFim();
        return recorrenciaInstrucaoPagamentoService.buscarPorCodFimAFimComAutorizacao(idFImAFim)
                .map(ErroWrapperDTO::new)
                .orElseGet(() -> new ErroWrapperDTO<>(
                        new ErroDTO(AppExceptionCode.INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA,
                                AppExceptionCode.INSTRUCAO_PAGAMENTO_NAO_ENCONTRADA.getMessage(),
                                TipoRetornoTransacaoEnum.ERRO_NEGOCIO)));
    }

    private ErroWrapperDTO<RecorrenciaInstrucaoPagamentoCancelamento> processarCancelamentoAgendamento(CancelamentoAgendamentoWrapperDTO wrapperDTO) {
        try {
            var instrucaoPagamento = wrapperDTO.getInstrucaoPagamento();
            var cpfCnpjSolicitanteCancelamento = wrapperDTO.getCpfCnpjSolicitante();
            var idCancelamentoAgendamento = wrapperDTO.getIdCancelamentoAgendamento();
            var motivoCancelamentoCamt55 = wrapperDTO.getMotivoCancelamentoCamt55();
            var dataHoraSolicitacaoCancelamento = wrapperDTO.getDataHoraSolicitacaoCancelamento();

            instrucaoPagamento.setTpoSubStatus(TipoSubStatus.AGUARDANDO_CANCELAMENTO.name());
            var instrucaoPagamentoSalva = recorrenciaInstrucaoPagamentoService.salvarInstrucaoPagamento(instrucaoPagamento);

            var instrucaoCancelamento = RecorrenciaInstrucaoPagamentoCancelamento.builder()
                    .idCancelamentoAgendamento(idCancelamentoAgendamento)
                    .codFimAFim(instrucaoPagamento.getCodFimAFim())
                    .tpoPspSolicitanteCancelamento(PAGADOR)
                    .tpoStatus(TipoStatusCancelamentoInstrucaoPagamento.CRIADA.name())
                    .numCpfCnpjSolicitanteCancelamento(cpfCnpjSolicitanteCancelamento)
                    .codMotivoCancelamento(motivoCancelamentoCamt55.name())
                    .datCriacaoSolicitacaoCancelamento(dataHoraSolicitacaoCancelamento)
                    .recorrenciaInstrucaoPagamento(instrucaoPagamentoSalva)
                    .build();

            var instrucaoPagamentoCancelamentoSalva = recorrenciaInstrucaoPagamentoCancelamentoRepository.save(instrucaoCancelamento);

            return new ErroWrapperDTO<>(instrucaoPagamentoCancelamentoSalva);
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            return new ErroWrapperDTO<>(
                    new ErroDTO(
                            AppExceptionCode.ERRO_PERSISTENCIA,
                            AppExceptionCode.ERRO_PERSISTENCIA.getMensagemFormatada(e.getMessage()),
                            TipoRetornoTransacaoEnum.ERRO_INFRA
                    )
            );
        }
    }


    private Optional<ErroDTO> validarRequest(CancelamentoAgendamentoDebitoRequest autorizacaoRequest) {
        return validator.validate(autorizacaoRequest).stream()
                .map(ConstraintViolation::getMessage)
                .filter(StringUtils::isNotBlank)
                .findFirst()
                .map(mensagemErro -> new ErroDTO(
                        AppExceptionCode.SPIRECORRENCIA_REC0001,
                        AppExceptionCode.SPIRECORRENCIA_REC0001.getMensagemFormatada(mensagemErro),
                        TipoRetornoTransacaoEnum.ERRO_VALIDACAO));
    }

    private Optional<ErroDTO> validarRegraNegocio(CancelamentoAgendamentoWrapperDTO wrapperDTO) {
        var instrucaoPagamento = wrapperDTO.getInstrucaoPagamento();
        var cpfCnpjSolicitante = wrapperDTO.getCpfCnpjSolicitante();

        return validarStatusInstrucaoPagamento(instrucaoPagamento)
                .or(() -> validarDadosSolicitanteValidos(instrucaoPagamento, cpfCnpjSolicitante)
                .or(() -> validarDataEHoraParaCancelarAgendamento(instrucaoPagamento)));
    }

    private Optional<ErroDTO> validarDadosSolicitanteValidos(RecorrenciaInstrucaoPagamento instrucaoPagamento, String cpfCnpjSolicitanteCancelamento) {
        String cpfCnpjPagador = instrucaoPagamento.getNumCpfCnpjPagador();
        String numInstituicaoPagador = instrucaoPagamento.getNumInstituicaoPagador();

        if (cpfCnpjSolicitanteCancelamento.equals(cpfCnpjPagador)) {
            return Optional.empty();
        }

        if (cpfCnpjSolicitanteCancelamento.length() == 14 && cpfCnpjSolicitanteCancelamento.substring(0, 8).equals(numInstituicaoPagador)) {
            return Optional.empty();
        }

        return Optional.of(new ErroDTO(
                DADOS_SOLICITANTE_DIFERENTE_DA_INSTRUCAO_PAGAMENTO,
                TipoRetornoTransacaoEnum.ERRO_NEGOCIO));
    }

    private Optional<ErroDTO> validarStatusInstrucaoPagamento(RecorrenciaInstrucaoPagamento instrucaoPagamento) {
        if (!TipoStatusInstrucaoPagamento.ATIVA.name().equals(instrucaoPagamento.getTpoStatus())) {
            return Optional.of(new ErroDTO(
                    INSTRUCAO_PAGAMENTO_DIFERENTE_ATIVA,
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            ));
        }

        return Optional.empty();
    }

    private Optional<ErroDTO> validarDataEHoraParaCancelarAgendamento(RecorrenciaInstrucaoPagamento instrucaoPagamento) {
        if (prazoCancelamentoExpirado(instrucaoPagamento.getDatVencimento())) {
            return Optional.of(new ErroDTO(
                    HORARIO_CANCELAMENTO_FORA_DO_PERMITIDO,
                    TipoRetornoTransacaoEnum.ERRO_NEGOCIO
            ));
        }

        return Optional.empty();
    }

    private boolean prazoCancelamentoExpirado(LocalDate dataVencimento) {
        var horarioLimiteCancelamento = appConfig.getRegras().getCancelamentoAgendamento().getHorarioLimiteCancelamento();
        var diasMinimosAntecedencia = appConfig.getRegras().getCancelamentoAgendamento().getDiasMinimosAntecedencia();

        LocalDateTime limite = dataVencimento.minusDays(diasMinimosAntecedencia).atTime(horarioLimiteCancelamento);
        return LocalDateTime.now().isAfter(limite);
    }
}
