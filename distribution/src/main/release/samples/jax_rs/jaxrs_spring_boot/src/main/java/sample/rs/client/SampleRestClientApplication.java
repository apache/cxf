package sample.rs.client;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;

import sample.rs.service.HelloService;


public class SampleRestClientApplication {
    public static void main(String[] args) {
        HelloService service = JAXRSClientFactory.create("http://locahost:8080/helloservice/", 
                                  HelloService.class);
        System.out.println(service.sayHello("ApacheCxfUser"));
    }  
}