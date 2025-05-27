package io.sicredi.spirecorrencia.api.liquidacao;

import br.com.sicredi.canaisdigitais.dto.IdentificacaoAssociadoDTO;
import br.com.sicredi.framework.exception.TechnicalException;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.CadastroOrdemRequest;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.ParticipanteRequestDTO;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoLiquidacao;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoPixEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoStatusEnum;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.sicredi.spirecorrencia.api.exceptions.ErroLiquidacaoException;
import io.sicredi.spirecorrencia.api.participante.ParticipanteService;
import io.sicredi.spirecorrencia.api.protocolo.SpiCanaisProtocoloApiClient;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_AUTOMATICO;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_COM_CHAVE;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_INICIADOR;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_MANUAL;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_QRCODE_DINAMICO;
import static br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoPagamentoEnum.PIX_QRCODE_ESTATICO;

@Service
@RequiredArgsConstructor
class ProcessamentoLiquidacaoService {

    private final SpiCanaisProtocoloApiClient spiCanaisProtocoloApiClient;
    private final RecorrenciaTransacaoRepository recorrenciaTransacaoRepository;
    private final ParticipanteService participanteService;

    private static final String TIPO_PRODUTO = "AGENDADO_RECORRENTE";

    @RateLimiter(name = "processamentoLiquidacaoService")
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = TechnicalException.class)
    public void processarLiquidacao(TipoProcessamentoWrapperDTO tipoProcessamentoWrapperDTO) {
        var recorrenciaTransacao = tipoProcessamentoWrapperDTO.getRecorrenciaTransacao();
        var cadastroOrdemRequest = criarCadastroOrdemRequest(recorrenciaTransacao);
        try {
            recorrenciaTransacaoRepository.updateStateAndIdFimAFim(recorrenciaTransacao.getOidRecorrenciaTransacao(), TipoStatusEnum.PENDENTE, recorrenciaTransacao.getIdFimAFim(), LocalDateTime.now());
            spiCanaisProtocoloApiClient.emitirProtocoloLiquidacaoRecorrencia(recorrenciaTransacao.getRecorrencia().getTipoCanal(), TipoLiquidacao.AGENDADO_RECORRENTE, cadastroOrdemRequest);
        } catch (Exception ex) {
            throw new ErroLiquidacaoException("Falha durante emissão de protocolo de liquidação", ex);
        }
    }

    private CadastroOrdemRequest criarCadastroOrdemRequest(RecorrenciaTransacao recorrenciaTransacao) {
        var recorrencia = recorrenciaTransacao.getRecorrencia();
        var pagador = recorrencia.getPagador();
        var recebedor = recorrencia.getRecebedor();

        var request = new CadastroOrdemRequest();

        var associado = new IdentificacaoAssociadoDTO();
        associado.setTipoConta(pagador.getTipoConta().getTipoContaCanaisDigitais());
        associado.setCpfUsuario(pagador.getCpfCnpj());
        associado.setCpfCnpjConta(pagador.getCpfCnpj());
        associado.setConta(pagador.getConta());
        associado.setAgencia(pagador.getCodPosto());
        associado.setNomeAssociadoConta(pagador.getNome());
        associado.setNomeUsuario(pagador.getNome());
        associado.setCooperativa(pagador.getAgencia());
        associado.setOrigemConta(recorrencia.getTipoOrigemSistema());

        var nomeInstituicaoPagador = participanteService.buscarNomeInstituicao(pagador.getInstituicao());
        var participantePagador = new ParticipanteRequestDTO();
        participantePagador.setIspb(pagador.getInstituicao());
        participantePagador.setNome(nomeInstituicaoPagador);

        var nomeInstituicaoRecebedor = participanteService.buscarNomeInstituicao(recebedor.getInstituicao());
        var participanteRecebedor = new ParticipanteRequestDTO();
        participanteRecebedor.setIspb(recebedor.getInstituicao());
        participanteRecebedor.setNome(nomeInstituicaoRecebedor);

        request.setPrioridadePagamento("NORMAL");
        request.setChaveDict(recebedor.getChave());
        request.setCpfCnpjAssociado(pagador.getCpfCnpj());
        request.setNomeUsuarioPagador(pagador.getNome());
        request.setInformacoesEntreUsuarios(recorrenciaTransacao.getInformacoesEntreUsuarios());
        request.setParticipantePagador(participantePagador);
        request.setAgenciaUsuarioRecebedor(recebedor.getAgencia());
        request.setContaUsuarioRecebedor(recebedor.getConta());
        request.setCpfCnpjUsuarioRecebedor(recebedor.getCpfCnpj());
        request.setNomeUsuarioRecebedor(recebedor.getNome());
        request.setParticipanteRecebedor(participanteRecebedor);
        request.setTipoContaUsuarioRecebedor(recebedor.getTipoConta());
        request.setIdConciliacaoRecebedor(recorrenciaTransacao.getIdConciliacaoRecebedor());
        request.setInformacoesEntreUsuarios(recorrenciaTransacao.getInformacoesEntreUsuarios());
        request.setNumInicCnpj(recorrencia.getNumInicCnpj());
        request.setDataTransacao(recorrenciaTransacao.getDataTransacao().atTime(LocalTime.now()));
        request.setValor(recorrenciaTransacao.getValor());
        request.setTipoProduto(TIPO_PRODUTO);
        request.setIdentificacaoAssociado(associado);
        request.setIdentificadorTransacao(recorrenciaTransacao.getIdFimAFim());
        request.setIdFimAFim(recorrenciaTransacao.getIdFimAFim());
        request.setTipoPagamentoPix(fromTipoPagamentoPix(recorrencia.getTipoIniciacao()));

        return request;
    }


    private static TipoPagamentoEnum fromTipoPagamentoPix(TipoPagamentoPixEnum tipoPagamentoPix) {
        return switch (tipoPagamentoPix) {
            case PIX_PAYMENT_BY_KEY -> PIX_COM_CHAVE;
            case PIX_PAYMENT_MANUAL -> PIX_MANUAL;
            case PIX_PAYMENT_BY_STATIC_QR_CODE -> PIX_QRCODE_ESTATICO;
            case PIX_PAYMENT_BY_DYNAMIC_QR_CODE -> PIX_QRCODE_DINAMICO;
            case PIX_INIC -> PIX_INICIADOR;
            case PIX_AUTOMATICO -> PIX_AUTOMATICO;
        };
    }
}
