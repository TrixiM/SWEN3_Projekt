package fhtw.wien.documentaccessbatchservice.config;

import fhtw.wien.documentaccessbatchservice.xmlModel.AccessStatisticsDateListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class BatchListenerConfig {

    @Bean
    public AccessStatisticsDateListener accessStatisticsDateListener() {
        return new AccessStatisticsDateListener(
                new ClassPathResource("accessLog/accessLog.xml")
        );
    }
}
