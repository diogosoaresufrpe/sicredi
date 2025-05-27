package io.sicredi.spirecorrencia.api.utils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

@RequiredArgsConstructor
@Slf4j
@Component
public class NotificadorProcessadorPaginacaoUtil{

    public <N, T> void processaPaginacoesEnviaNotificacao(Page<T> paginas, Function<T,N> conversor, Consumer<N> produtor) {
        IntStream.iterate(0, pageNumber -> pageNumber + 1)
                .mapToObj(pageNumber -> paginas)
                .takeWhile(Page::hasContent)
                .forEach(pagina -> enviarNotificacoes(pagina, conversor, produtor));
    }

    protected  <T, N> void enviarNotificacoes(Page<T> solicitacoes, Function<T, N> conversor, Consumer<N> produtor) {
        log.debug("Enviando total de notificações: {}", solicitacoes.getTotalElements());

        solicitacoes.forEach(solicitacao -> {
            N notificacao = conversor.apply(solicitacao);
            produtor.accept(notificacao);
        });
    }
}
