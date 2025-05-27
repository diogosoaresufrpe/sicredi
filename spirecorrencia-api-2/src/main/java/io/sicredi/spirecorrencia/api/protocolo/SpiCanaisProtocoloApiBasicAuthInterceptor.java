package io.sicredi.spirecorrencia.api.protocolo;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;

import static feign.Util.ISO_8859_1;

@Configuration
class SpiCanaisProtocoloApiBasicAuthInterceptor implements RequestInterceptor {

    private static final String CLIENTID_SERVICE_PROPERTY = "services.%s.auth.client-id";
    private static final String CLIENTSECRET_SERVICE_PROPERTY = "services.%s.auth.client-secret";

    @Autowired
    private Environment environment;

    @Override
    public void apply(RequestTemplate template) {
        var clientId = environment.getProperty(String.format(CLIENTID_SERVICE_PROPERTY, template.feignTarget().name()));
        var clientSecret = environment.getProperty(String.format(CLIENTSECRET_SERVICE_PROPERTY, template.feignTarget().name()));
        if (clientId != null && clientSecret != null) {
            template.header(HttpHeaders.AUTHORIZATION, "Basic " + HttpHeaders.encodeBasicAuth(clientId, clientSecret, ISO_8859_1));
        }
    }
}
