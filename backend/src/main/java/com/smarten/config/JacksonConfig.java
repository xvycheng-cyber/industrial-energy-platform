package com.smarten.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 配置：统一时间格式与命名策略。
 *
 * <p>时间格式 {@code yyyy-MM-dd HH:mm:ss}，时区 Asia/Shanghai；
 * 反序列化忽略未知字段，避免前端多传字段导致 400。</p>
 */
@Configuration
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIME_PATTERN = "HH:mm:ss";
    public static final String TIME_ZONE = "Asia/Shanghai";

    @Bean
    @Primary
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

        return builder -> builder
                .timeZone(TimeZone.getTimeZone(TIME_ZONE))
                .serializers(
                        new LocalDateTimeSerializer(dateTimeFormatter),
                        new LocalDateSerializer(dateFormatter),
                        new LocalTimeSerializer(timeFormatter))
                .deserializers(
                        new LocalDateTimeDeserializer(dateTimeFormatter),
                        new LocalDateDeserializer(dateFormatter),
                        new LocalTimeDeserializer(timeFormatter))
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
}
