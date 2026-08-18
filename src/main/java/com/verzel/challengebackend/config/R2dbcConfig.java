package com.verzel.challengebackend.config;

import com.verzel.challengebackend.domain.TipoAcesso;
import io.r2dbc.spi.ConnectionFactory;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        return R2dbcCustomConversions.of(dialect, List.of(
                new TipoAcessoToStringConverter(),
                new StringToTipoAcessoConverter()));
    }

    @WritingConverter
    static class TipoAcessoToStringConverter implements Converter<TipoAcesso, String> {
        @Override
        public String convert(TipoAcesso source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class StringToTipoAcessoConverter implements Converter<String, TipoAcesso> {
        @Override
        public TipoAcesso convert(String source) {
            return TipoAcesso.valueOf(source);
        }
    }
}
