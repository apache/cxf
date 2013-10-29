/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package demo.hwDispatch.server;
import javax.xml.ws.Endpoint;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Server {

    protected Server() throws Exception {
        System.out.println("Starting Server");

        System.out.println("Starting NewWSDLFile service");
        Object implementor = new NewWSDLFileSoapMessageProvider();
        String address = "http://localhost:9000/SoapContext/NewWSDLFileSOAP";
        //EndpointImpl ep = (EndpointImpl)Endpoint.publish(address, implementor); 
	//ep.getFeatures().add(new WSAddressingFeature());
	EndpointImpl ep = (EndpointImpl)Endpoint.create(implementor);
	ep.getProperties().put("schema-validation-enabled","true");
	ep.publish(address);

	// also start a second server using Spring
	new ClassPathXmlApplicationContext("server-applicationContext.xml");
        System.in.read();
    }

    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }

}
