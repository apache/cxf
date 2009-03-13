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
package interop.server;

import javax.xml.ws.Endpoint;

import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.ws.security.SecurityConstants;

public class Server {

    
    protected Server() throws Exception {
        new SpringBusFactory().createBus("etc/server.xml");

        // Scenario 4.1
        /*
        addProperties(Endpoint.publish("http://localhost:9001/APingService", 
                             new APingService()));
        addProperties(Endpoint.publish("http://localhost:9001/A-NoTimestampPingService", 
                             new ANoTimestampPingService()));
        addProperties(Endpoint.publish("http://localhost:9001/ADPingService", 
                             new ADPingService()));
         */
        
        // Scenario 4.2
        addProperties(Endpoint.publish("http://localhost:9001/UXPingService", 
                                       new UXPingService()));
        
        //argv = new String[] {"A-ES"}; //NOT WORKING YET
        //argv = new String[] {"AD-ES"}; //NOT WORKING YET
        //argv = new String[] {"UX"}; //NOT WORKING YET
        /*
        argv = new String[] {"UX-NoTimestamp"}; //NOT WORKING YET
        argv = new String[] {"UXD"};
        argv = new String[] {"UXD_IPingService1"};
        argv = new String[] {"UX-SEES"};
        argv = new String[] {"UXD-SEES"};
        argv = new String[] {"X"};
        argv = new String[] {"X_IPingService1"};
        argv = new String[] {"X-NoTimestamp"};
        argv = new String[] {"X-AES128"};
        argv = new String[] {"X-AES192"};
        argv = new String[] {"X-TripleDES"};
        argv = new String[] {"XD"};
        argv = new String[] {"XD_IPingService1"};
        argv = new String[] {"XD-ES"};
        argv = new String[] {"XD-SEES"};
        */
    }
    private void addProperties(Endpoint ep) {
        ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ep.getProperties().put(SecurityConstants.ENCRYPT_USERNAME, "Bob");
        ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES, "etc/bob.properties");
    }
    
    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(60 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
    
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "A_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "wsdl/WsSecurity11.wsdl")        
    public static class APingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "A-NoTimestamp_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "wsdl/WsSecurity11.wsdl")        
    public static class ANoTimestampPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "AD_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "wsdl/WsSecurity11.wsdl")        
    public static class ADPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UX_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "wsdl/WsSecurity11.wsdl")        
    public static class UXPingService extends PingService {
    }
}
