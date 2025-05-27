package io.sicredi.spirecorrencia.api.utils;

import br.com.sicredi.framework.exception.TechnicalException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.sicredi.spirecorrencia.api.exceptions.MensagemInvalidaException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Optional;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ObjectMapperUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final DateTimeFormatter CANAIS_DIGITAIS_LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter CANAIS_DIGITAIS_LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter CANAIS_DIGITAIS_LOCAL_DATE_TIME_FORMATTER_NOVO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");
    public static final DateTimeFormatter CANAIS_DIGITAIS_LOCAL_TIME_FORMATTER_NOVO = DateTimeFormatter.ofPattern("HH:mm:ss");


    static {
        var canaisDigitaisLocalDateTimeFormatter = CANAIS_DIGITAIS_LOCAL_DATE_TIME_FORMATTER;
        var canaisDigitaisLocalTimeFormatter = CANAIS_DIGITAIS_LOCAL_TIME_FORMATTER;
        var canaisDigitaisLocalDateTimeFormatterNovo = CANAIS_DIGITAIS_LOCAL_DATE_TIME_FORMATTER_NOVO;
        var canaisDigitaisLocalTimeFormatterNovo = CANAIS_DIGITAIS_LOCAL_TIME_FORMATTER_NOVO;

        var javaTimeModuleDefault = new JavaTimeModule();
        javaTimeModuleDefault.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(DateTimeFormatter.ISO_DATE_TIME));
        javaTimeModuleDefault.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(canaisDigitaisLocalDateTimeFormatter));
        javaTimeModuleDefault.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(canaisDigitaisLocalDateTimeFormatterNovo));
        javaTimeModuleDefault.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        javaTimeModuleDefault.addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ISO_DATE));
        javaTimeModuleDefault.addDeserializer(LocalTime.class, new LocalTimeDeserializer(canaisDigitaisLocalTimeFormatter));
        javaTimeModuleDefault.addDeserializer(LocalTime.class, new LocalTimeDeserializer(canaisDigitaisLocalTimeFormatterNovo));
        javaTimeModuleDefault.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DateTimeFormatter.ISO_DATE_TIME));
        javaTimeModuleDefault.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(canaisDigitaisLocalDateTimeFormatter));
        javaTimeModuleDefault.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(canaisDigitaisLocalDateTimeFormatterNovo));
        javaTimeModuleDefault.addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ISO_DATE));
        javaTimeModuleDefault.addSerializer(LocalTime.class, new LocalTimeSerializer(canaisDigitaisLocalTimeFormatter));
        javaTimeModuleDefault.addSerializer(LocalTime.class, new LocalTimeSerializer(canaisDigitaisLocalTimeFormatterNovo));


        objectMapper.registerModule(javaTimeModuleDefault);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
    }

    public static <T> String converterObjetoParaString(T object, boolean deveLancarException) {
        try {
            return Optional.of(escreverValor(object)).orElse(StringUtils.EMPTY);
        } catch (MensagemInvalidaException mensagemInvalidaException) {
            if (deveLancarException) {
                throw mensagemInvalidaException;
            }
            return StringUtils.EMPTY;
        }
    }

    public static <T> T converterStringParaObjeto(String message, Class<T> clazz) {
        try {
            return Optional.ofNullable(objectMapper.readValue(message, clazz))
                    .orElseThrow(() -> new RuntimeException("Leitura do valor retornou vazio"));
        } catch (Exception e) {
            var mensagemErro = String.format("Erro ao deserializar objeto como %s", clazz.getName());
            throw new MensagemInvalidaException(mensagemErro, e);
        }
    }
    private static <T> String escreverValor(T object) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(object))
                    .orElseThrow(() -> new RuntimeException("Escrita do objeto retornou vazio"));
        } catch (Exception e) {
            log.error("Erro ao tentar serializar objeto. Objeto: {}", object, e);
            throw new MensagemInvalidaException("Erro ao tentar serializar objeto", e);
        }
    }

    public static <T> T converterStringParaObjeto(String message, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(message, typeReference);
        } catch (JsonProcessingException e) {
            var mensagemErro = String.format("Erro ao deserializar objeto como %s", typeReference.getType());
            throw new TechnicalException(mensagemErro, e);
        }
    }

    public static String converterObjetoParaString(Object object) {
        try {
            return Optional.ofNullable(objectMapper.writeValueAsString(object))
                    .orElseThrow(() -> new TechnicalException("Leitura do valor retornou vazio"));
        } catch (IOException e) {
            var mensagemErro = String.format("Erro ao serializar objeto. %s", object);
            throw new TechnicalException(mensagemErro, e);
        }
    }

    public static class CustomLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

        private static final DateTimeFormatter FORMATTER_1 = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSS]");
        private static final DateTimeFormatter FORMATTER_2 = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH:mm:ss").optionalStart().appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true).optionalEnd().toFormatter();

        @Override
        public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText();

            try {
                return LocalDateTime.parse(dateStr, FORMATTER_1);
            } catch (Exception e1) {
                try {
                    return LocalDateTime.parse(dateStr, FORMATTER_2);
                } catch (Exception e2) {
                    throw new IOException("Failed to parse date: " + dateStr, e2);
                }
            }
        }
    }
}
