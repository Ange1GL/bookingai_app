package com.github.angellariosacosta.bookingapp.infrastructure.config;

import com.openai.azure.AzureOpenAIServiceVersion;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
@ConfigurationPropertiesBinding
public class AzureOpenAIServiceVersionConverter implements Converter<String, AzureOpenAIServiceVersion> {

    @Override
    public AzureOpenAIServiceVersion convert(String source) {
        return AzureOpenAIServiceVersion.fromString(source);
    }
}
