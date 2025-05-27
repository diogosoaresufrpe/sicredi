package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.canaisdigitais.dto.IdentificacaoAssociadoDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaParcelaRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ExclusaoRecorrenciaProtocoloRequestBuilder {

    public static ExclusaoRecorrenciaProtocoloRequest criarExclusaoRecorrenciaProtocoloRequest(
            String identificadorTransacao,
            Recorrencia recorrencia,
            Collection<RecorrenciaTransacao> parcelas
    ) {
        IdentificacaoAssociadoDTO associado = new IdentificacaoAssociadoDTO();
        var pagador = recorrencia.getPagador();

        associado.setCpfUsuario(pagador.getCpfCnpj());
        associado.setCpfCnpjConta(pagador.getCpfCnpj());
        associado.setConta(pagador.getConta());
        associado.setAgencia(pagador.getCodPosto());
        associado.setNomeAssociadoConta(pagador.getNome());
        associado.setNomeUsuario(pagador.getNome());
        associado.setTipoConta(pagador.getTipoConta().getTipoContaCanaisDigitais());
        associado.setCooperativa(pagador.getAgencia());
        associado.setOrigemConta(recorrencia.getTipoOrigemSistema());

        ExclusaoRecorrenciaProtocoloRequest request = new ExclusaoRecorrenciaProtocoloRequest();

        request.setIdentificadorTransacao(identificadorTransacao);
        request.setIdentificadorRecorrencia(recorrencia.getIdRecorrencia());
        request.setTipoMotivoExclusao(TipoMotivoExclusao.SOLICITADO_SISTEMA);
        request.setIdentificacaoAssociado(associado);

        List<ExclusaoRecorrenciaParcelaRequest> listaParcelas = parcelas.stream()
                .map(parcela -> {
                    ExclusaoRecorrenciaParcelaRequest parcelaRequest = new ExclusaoRecorrenciaParcelaRequest();
                    parcelaRequest.setValor(parcela.getValor());
                    parcelaRequest.setDataTransacao(parcela.getDataTransacao().atTime(LocalTime.now()));
                    parcelaRequest.setIdFimAFim(parcela.getIdFimAFim());
                    parcelaRequest.setIdentificadorParcela(parcela.getIdParcela());
                    return parcelaRequest;
                }).toList();

        request.setParcelas(listaParcelas);

        return request;
    }
}
