package com.pantry.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.FixedLocaleResolver;

import java.util.Locale;

@Configuration
public class WebConfig {

    /** Date e numeri sempre in italiano (es. "lun 12 ott", "3,50"). */
    @Bean
    public LocaleResolver localeResolver() {
        return new FixedLocaleResolver(Locale.ITALY);
    }
}
