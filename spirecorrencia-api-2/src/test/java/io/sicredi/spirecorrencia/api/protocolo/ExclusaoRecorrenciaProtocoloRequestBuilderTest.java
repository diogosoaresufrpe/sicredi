package io.sicredi.spirecorrencia.api.protocolo;

import br.com.sicredi.canaisdigitais.enums.OrigemEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoContaEnum;
import br.com.sicredi.spicanais.transacional.transport.lib.commons.enums.TipoMotivoExclusao;
import br.com.sicredi.spicanais.transacional.transport.lib.recorrencia.ExclusaoRecorrenciaProtocoloRequest;
import io.sicredi.spirecorrencia.api.repositorio.Pagador;
import io.sicredi.spirecorrencia.api.repositorio.Recorrencia;
import io.sicredi.spirecorrencia.api.repositorio.RecorrenciaTransacao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static io.sicredi.spirecorrencia.api.utils.ConstantesTest.IDENTIFICADOR_TRANSACAO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ExclusaoRecorrenciaProtocoloRequestBuilderTest {


    @Test
    void criarExclusaoRecorrenciaProtocoloRequest_deveCriarRequestComDadosCorretos() {
        Pagador pagador = Pagador.builder()
                .cpfCnpj("12345678901")
                .conta("567890")
                .codPosto("1234")
                .nome("Nome Pagador")
                .tipoConta(TipoContaEnum.CONTA_CORRENTE)
                .agencia("1234")
                .build();

        Recorrencia recorrencia = Recorrencia.builder()
                .idRecorrencia("recorrencia-id")
                .pagador(pagador)
                .tipoOrigemSistema(OrigemEnum.LEGADO)
                .build();

        RecorrenciaTransacao parcela = RecorrenciaTransacao.builder()
                .valor(BigDecimal.TEN)
                .dataTransacao(LocalDate.now())
                .idFimAFim("fim-a-fim-id")
                .idParcela("parcela-id")
                .build();

        ExclusaoRecorrenciaProtocoloRequest request = ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(
                IDENTIFICADOR_TRANSACAO, recorrencia, List.of(parcela));

        assertNotNull(request);
        assertEquals("recorrencia-id", request.getIdentificadorRecorrencia());
        assertEquals(TipoMotivoExclusao.SOLICITADO_SISTEMA, request.getTipoMotivoExclusao());
        assertEquals("12345678901", request.getIdentificacaoAssociado().getCpfUsuario());
        assertEquals("567890", request.getIdentificacaoAssociado().getConta());
        assertEquals("1234", request.getIdentificacaoAssociado().getAgencia());
        assertEquals("Nome Pagador", request.getIdentificacaoAssociado().getNomeAssociadoConta());
        assertEquals("Nome Pagador", request.getIdentificacaoAssociado().getNomeUsuario());
        assertEquals("CONTA_CORRENTE", request.getIdentificacaoAssociado().getTipoConta().name());
        assertEquals("1234", request.getIdentificacaoAssociado().getCooperativa());
        assertEquals(OrigemEnum.LEGADO, request.getIdentificacaoAssociado().getOrigemConta());
        assertEquals(1, request.getParcelas().size());
        assertEquals(BigDecimal.TEN, request.getParcelas().getFirst().getValor());
        assertEquals(LocalDate.now().atTime(LocalTime.now()).getHour(), request.getParcelas().getFirst().getDataTransacao().getHour());
        assertEquals("fim-a-fim-id", request.getParcelas().getFirst().getIdFimAFim());
        assertEquals("parcela-id", request.getParcelas().getFirst().getIdentificadorParcela());
    }

    @Test
    void criarExclusaoRecorrenciaProtocoloRequest_deveLancarExcecaoQuandoRecorrenciaNula() {
        assertThrows(NullPointerException.class, () -> ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(IDENTIFICADOR_TRANSACAO, null, List.of()));
    }

    @Test
    void criarExclusaoRecorrenciaProtocoloRequest_deveLancarExcecaoQuandoParcelasNulas() {
        Recorrencia recorrencia = mock(Recorrencia.class);
        assertThrows(NullPointerException.class, () -> ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(IDENTIFICADOR_TRANSACAO, recorrencia, null));
    }

    @Test
    void criarExclusaoRecorrenciaProtocoloRequest_deveLancarExcecaoQuandoOrigemContaNula() {
        Recorrencia recorrencia = mock(Recorrencia.class);
        RecorrenciaTransacao parcela = mock(RecorrenciaTransacao.class);
        assertThrows(NullPointerException.class, () -> ExclusaoRecorrenciaProtocoloRequestBuilder.criarExclusaoRecorrenciaProtocoloRequest(IDENTIFICADOR_TRANSACAO, recorrencia, List.of(parcela)));
    }


}