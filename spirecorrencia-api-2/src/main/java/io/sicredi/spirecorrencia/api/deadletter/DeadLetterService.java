package io.sicredi.spirecorrencia.api.deadletter;

import br.com.sicredi.canaisdigitais.enums.TipoRetornoTransacaoEnum;
import br.com.sicredi.framework.exception.TechnicalException;
import io.sicredi.spirecorrencia.api.idempotente.CriaResponseStrategyFactory;
import io.sicredi.spirecorrencia.api.idempotente.ErroDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.SPIRECORRENCIA_BU9001;
import static io.sicredi.spirecorrencia.api.exceptions.AppExceptionCode.SPIRECORRENCIA_BU9003;


@Slf4j
@Service
@RequiredArgsConstructor
public class DeadLetterService {

    private final CriaResponseStrategyFactory criaResponseStrategyFactory;

    public void processar(DeadLetterRequest reprocessamentoRequest) {
        var tipoExceptionDeadLetter = DeadLetterTipoErro.of(reprocessamentoRequest.causaException());

        if (DeadLetterTipoErro.IDEMPOTENT_TRANSACTION_DUPLICATED == tipoExceptionDeadLetter) {
            processarIdempotentTransactionDuplicatedException(reprocessamentoRequest);
            return;
        }
        if (DeadLetterTipoErro.OUTRAS == tipoExceptionDeadLetter) {
            processarOutrasExcecoes(reprocessamentoRequest);
        }
        throw new TechnicalException("Tipo de exceção não mapeada: " + tipoExceptionDeadLetter);
    }

    private void processarIdempotentTransactionDuplicatedException(DeadLetterRequest reprocessamentoRequest) {
        var erro = new ErroDTO(SPIRECORRENCIA_BU9003, TipoRetornoTransacaoEnum.ERRO_INFRA);
        criaResponseStrategyFactory.criar(reprocessamentoRequest.tipoResponse())
                .criarResponseReprocessamentoIdempotentTransactionDuplicated(reprocessamentoRequest, erro);
    }

    private void processarOutrasExcecoes(DeadLetterRequest reprocessamentoRequest) {
        var mensagemErro = reprocessamentoRequest.mensagemErro();

        var erro = new ErroDTO(SPIRECORRENCIA_BU9001, mensagemErro, TipoRetornoTransacaoEnum.ERRO_INFRA);

        criaResponseStrategyFactory.criar(reprocessamentoRequest.tipoResponse())
                .criarResponseReprocessamentoOutrasExceptions(reprocessamentoRequest, erro);
    }

}
