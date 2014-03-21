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

package demo.wssec.server;

import java.net.URL;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JStaxOutInterceptor;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;

/**
 * A StAX-based server
 */
public class StaxServer {

    protected StaxServer() throws Exception {
        System.out.println("Starting StaxServer");

        Object implementor = new GreeterImpl();
        String address = "http://localhost:9000/SoapContext/GreeterPort";
        EndpointImpl impl = (EndpointImpl)Endpoint.publish(address, implementor);
        
        WSSSecurityProperties properties = new WSSSecurityProperties();
        properties.addAction(WSSConstants.USERNAMETOKEN);
        properties.addAction(WSSConstants.TIMESTAMP);

        properties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_TEXT);
        properties.setTokenUser("Alice");
        
        properties.setCallbackHandler(new UTPasswordCallback());
        
        impl.getOutInterceptors().add(new WSS4JStaxOutInterceptor(properties));

        WSSSecurityProperties inProperties = new WSSSecurityProperties();
        inProperties.addAction(WSSConstants.USERNAMETOKEN);
        inProperties.addAction(WSSConstants.TIMESTAMP);
        inProperties.setUsernameTokenPasswordType(WSSConstants.UsernameTokenPasswordType.PASSWORD_DIGEST);
        inProperties.setCallbackHandler(new UTPasswordCallback());
        
        impl.getInInterceptors().add(new WSS4JStaxInInterceptor(inProperties));
    }

    public static void main(String args[]) throws Exception {

        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = StaxServer.class.getResource("/wssec.xml");
        Bus bus = bf.createBus(busFile.toString());

        BusFactory.setDefaultBus(bus);

        new StaxServer();
        System.out.println("StaxServer ready...");

        Thread.sleep(5 * 60 * 1000);

        bus.shutdown(true);
        System.out.println("StaxServer exiting");
        System.exit(0);
    }
}
