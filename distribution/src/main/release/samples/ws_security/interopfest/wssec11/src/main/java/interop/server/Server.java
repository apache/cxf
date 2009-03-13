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

    
    protected Server(String baseUrl) throws Exception {
        
        new SpringBusFactory().createBus("etc/server.xml");
        doPublish(baseUrl + "/APingService", new APingService());
        doPublish(baseUrl + "/A-NoTimestampPingService", new ANoTimestampPingService());
        doPublish(baseUrl + "/ADPingService", new ADPingService());
        doPublish(baseUrl + "/A-ESPingService", new AESPingService());
        doPublish(baseUrl + "/AD-ESPingService", new ADESPingService());
        doPublish(baseUrl + "/UXPingService", new UXPingService());
        doPublish(baseUrl + "/UX-NoTimestampPingService", new UXNoTimestampPingService());
        doPublish(baseUrl + "/UXDPingService", new UXDPingService());
        doPublish(baseUrl + "/UX-SEESPingService", new UXSEESPingService());
        doPublish(baseUrl + "/UXD-SEESPingService", new UXDSEESPingService()); 
        doPublish(baseUrl + "/XPingService", new XPingService());
        doPublish(baseUrl + "/X-NoTimestampPingService", new XNoTimestampPingService());
        doPublish(baseUrl + "/X-AES128PingService", new XAES128PingService());
        doPublish(baseUrl + "/X-AES192PingService", new XAES192PingService());
        doPublish(baseUrl + "/X-TripleDESPingService", new XTripleDESPingService());
        doPublish(baseUrl + "/XDPingService", new XDPingService());
        doPublish(baseUrl + "/XD-ESPingService", new XDESPingService());
        doPublish(baseUrl + "/XD-SEESPingService", new XDSEESPingService());
    }
    private void doPublish(String url, Object obj) {
        Endpoint ep = Endpoint.create(obj);
        ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES, "etc/bob.properties");
        ep.publish(url);
    }
    
    public static void main(String args[]) throws Exception {
        new Server("http://localhost:9001");
        System.out.println("Server ready...");

        Thread.sleep(60 * 60 * 10000);
        System.out.println("Server exiting");
        System.exit(0);
    }
    
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "A_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class APingService extends PingService {
    }
    
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "A-NoTimestamp_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class ANoTimestampPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "AD_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class ADPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "A-ES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class AESPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "AD-ES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class ADESPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UX_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class UXPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UX-NoTimestamp_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class UXNoTimestampPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UXD_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class UXDPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UX-SEES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class UXSEESPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "UXD-SEES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class UXDSEESPingService extends PingService {
    }


    
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "X_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "X-NoTimestamp_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XNoTimestampPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "XD_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XDPingService extends PingService {
    }

    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "XD-ES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XDESPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "XD-SEES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XDSEESPingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "X-AES128_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XAES128PingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "X-AES192_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XAES192PingService extends PingService {
    }
    @javax.jws.WebService(targetNamespace = "http://InteropBaseAddress/interop", 
                          serviceName = "PingService11", 
                          portName = "X-TripleDES_IPingService", 
                          endpointInterface = "interopbaseaddress.interop.IPingService",
                          wsdlLocation = "target/wsdl2/WsSecurity11.wsdl")        
    public static class XTripleDESPingService extends PingService {
    }

}
