package io.sicredi.spirecorrencia.api.idempotente;

public interface IdempotenteService {

    void reenviarOperacao(final String topico, final String chaveIdempotencia);
}
