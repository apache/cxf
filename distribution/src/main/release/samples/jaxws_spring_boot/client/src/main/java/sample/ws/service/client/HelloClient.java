package sample.ws.service.client;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import sample.ws.service.Hello;

public class HelloClient {

	public static void main(String[] args) {
		try {
		    URL wsdlURL = new URL("http://localhost:8080/Service/Hello?wsdl");
		    QName SERVICE_NAME = new QName("http://service.ws.sample/","HelloService");
		    Service service = Service.create(wsdlURL, SERVICE_NAME);
		    Hello client = service.getPort(Hello.class);
		    System.out.println(client.sayHello("Elan"));
		} catch (Exception e) {
		    e.printStackTrace();
		}
	}

}
