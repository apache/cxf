package sample.rs.service;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;

import org.apache.cxf.endpoint.Server;
import org.apache.cxf.bus.spring.SpringBus;
import sample.rs.service.HelloService;
import org.apache.cxf.jaxrs.JAXRSBindingFactory;

@Configuration
@EnableAutoConfiguration
@ImportResource({ "classpath:META-INF/cxf/cxf.xml" })
public class SampleRestWSApplication extends SpringBootServletInitializer {
 
    @Autowired
    private ApplicationContext applicationContext;
 
    public static void main(String[] args) {
        SpringApplication.run(SampleRestWSApplication.class, args);
    }
 
    @Bean
    public ServletRegistrationBean servletRegistrationBean(ApplicationContext context) {
        return new ServletRegistrationBean(new CXFServlet(), "/services/*");
    }
 
    
    @Bean
    public Server rsServer() {
        Bus bus = (Bus) applicationContext.getBean(Bus.DEFAULT_BUS_ID);
        JAXRSServerFactoryBean endpoint = new JAXRSServerFactoryBean();
        endpoint.setServiceBean(new HelloService());
        endpoint.setAddress("/helloservice");
        endpoint.setBus(bus);
        return endpoint.create();
    }
 
    @Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SampleRestWSApplication.class);
	}
 
}