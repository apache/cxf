package demo.jaxrs.openapi.server;

import java.util.Arrays;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.microprofile.openapi.OpenApiFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Bean
    OpenApiFeature openApiFeature() {
        final OpenApiFeature openApiFeature = new OpenApiFeature();
        openApiFeature.setTitle("Sample REST Application");
        openApiFeature.setScan(false);
        return openApiFeature;
    }
    
    @Bean
    Sample sampleResource() {
        return new Sample();
    }
    
    @Bean
    org.apache.cxf.endpoint.Server server() {
        final JAXRSServerFactoryBean factory = new JAXRSServerFactoryBean ();
        factory.setFeatures(Arrays.asList(openApiFeature()));
        factory.setServiceBean(sampleResource());
        factory.setAddress("http://localhost:9000/");
        return factory.create();
    }
}
