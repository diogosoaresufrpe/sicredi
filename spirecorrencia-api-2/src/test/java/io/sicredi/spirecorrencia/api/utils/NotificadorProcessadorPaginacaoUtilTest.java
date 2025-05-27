package io.sicredi.spirecorrencia.api.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificadorProcessadorPaginacaoUtilTest {

    @InjectMocks
    private NotificadorProcessadorPaginacaoUtil util;

    @BeforeEach
    void setUp() {
        util = Mockito.spy(NotificadorProcessadorPaginacaoUtil.class);
    }

    @Test
    void dadoNotificadorProcessadorPaginacaoUtil_quandoNaoHouverRegistro_deveIgnorar() {

        Page<Object> emptyPage = Page.empty();
        Function<Object, Object> conversor = mock(Function.class);
        Consumer<Object> produtor = mock(Consumer.class);

        util.processaPaginacoesEnviaNotificacao(emptyPage, conversor, produtor);

        verifyNoInteractions(conversor);
        verifyNoInteractions(produtor);
    }

    @Test
    void dadoNotificadorProcessadorPaginacaoUtil_quandoHouverRegistrosValidos_deveEnviarNotificacao() {

        List<String> elementosPagina1 = List.of("item1", "item2", "item3");
        Page<String> pagina1 = new PageImpl<>(elementosPagina1, PageRequest.of(0, 3), 6);

        List<String> elementosPagina2 = List.of("item4", "item5", "item6");
        Page<String> pagina2 = new PageImpl<>(elementosPagina2, PageRequest.of(1, 3), 6);

        Function<String, String> conversorMock = mock(Function.class);
        Consumer<String> consumidorMock = mock(Consumer.class);

        when(conversorMock.apply(any(String.class))).thenAnswer(invocation -> "Notificação: " + invocation.getArgument(0));

        doAnswer(invocation -> {
            util.enviarNotificacoes(pagina1, conversorMock, consumidorMock);
            util.enviarNotificacoes(pagina2, conversorMock, consumidorMock);
            return null;
        }).when(util).processaPaginacoesEnviaNotificacao(any(Page.class), any(Function.class), any(Consumer.class));

        util.processaPaginacoesEnviaNotificacao(pagina1, conversorMock, consumidorMock);

        verify(util, times(1)).enviarNotificacoes(pagina1, conversorMock, consumidorMock);
        verify(util, times(1)).enviarNotificacoes(pagina2, conversorMock, consumidorMock);
    }
}