package com.sonofnos.nibssgateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({EncryptionProperties.class, WebhookProperties.class, AmlProperties.class})
public class PropertiesConfig {
}
