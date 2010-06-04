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

package org.apache.cxf.systest.ws.wssc.server;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssc.client.KeystorePasswordCallback;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.ws.security.WSSConfig;


public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);

    public Server() throws Exception {
        
    }
    
    protected Server(String baseUrl) throws Exception {
        
        
        //"SecureConversation_UserNameOverTransport_IPingService",
        doPublish(baseUrl + "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
                  new SCMCSEIPingService());
        
        doPublish(baseUrl + "AC_IPingService", new ACIPingService());
        doPublish(baseUrl + "ADC_IPingService", new ADCIPingService());
        doPublish(baseUrl + "ADC-ES_IPingService", new ADCESIPingService()); 
        doPublish(baseUrl + "_A_IPingService", new AIPingService());
        doPublish(baseUrl + "_AD_IPingService", new ADIPingService());
        doPublish(baseUrl + "_AD-ES_IPingService", new ADESIPingService());

        doPublish(baseUrl + "UXC_IPingService", new UXCIPingService());
        doPublish(baseUrl + "UXDC_IPingService", new UXDCIPingService());
        doPublish(baseUrl + "UXDC-SEES_IPingService", new UXDCSEESIPingService());
        doPublish(baseUrl + "_UX_IPingService", new UXIPingService());
        doPublish(baseUrl + "_UXD_IPingService", new UXDIPingService());
        doPublish(baseUrl + "_UXD-SEES_IPingService", new UXDSEESIPingService());

        doPublish(baseUrl + "XC_IPingService", new XCIPingService());
        doPublish(baseUrl + "XDC_IPingService", new XDCIPingService());
        doPublish(baseUrl + "XDC_IPingService1", new XDC1IPingService());
        doPublish(baseUrl + "XDC-ES_IPingService", new XDCESIPingService());
        doPublish(baseUrl + "XDC-SEES_IPingService", new XDCSEESIPingService());
        doPublish(baseUrl + "_X_IPingService", new XIPingService());
        doPublish(baseUrl + "_X10_IPingService", new X10IPingService());
        doPublish(baseUrl + "_XD_IPingService", new XDIPingService());
        doPublish(baseUrl + "_XD-SEES_IPingService", new XDSEESIPingService());
        doPublish(baseUrl + "_XD-ES_IPingService",  new XDESIPingService());
        
        
        //Kerberos token - not sure where the token comes from or how these work
        //"KC_IPingService",       
        //"KDC_IPingService",
        //"KC10_IPingService",
        //"KDC10_IPingService",
        //"_K10_IPingService",
        //"_KD10_IPingService",
        //"_K_IPingService",
        //"_KD_IPingService", 
        //"_KD-SEES_IPingService",
        
    }
    
    protected void run()  {
        try {
            WSSConfig.getDefaultWSConfig();
            new Server("http://localhost:" + PORT + "/");
            Bus busLocal = new SpringBusFactory().createBus(
                    "org/apache/cxf/systest/ws/wssc/server/server.xml");
            BusFactory.setDefaultBus(busLocal);
            setBus(busLocal);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doPublish(String url, Object obj) {
        Endpoint ep = Endpoint.create(obj);
        ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER + ".sct", new KeystorePasswordCallback());
        ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct", 
                "org/apache/cxf/systest/ws/wssec11/server/bob.properties");
        
        if (url.contains("X10_I")) {
            ep.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES + ".sct", 
                    "org/apache/cxf/systest/ws/wssec11/server/bob.properties");
            ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct", 
                    "org/apache/cxf/systest/ws/wssec11/server/alice.properties");
        } else if (url.contains("MutualCert")) {
            ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct", 
                "org/apache/cxf/systest/ws/wssec11/server/bob.properties");
            ep.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES + ".sct", 
                "org/apache/cxf/systest/ws/wssec11/server/alice.properties");
            ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        }
        ep.publish(url);
    }
    
    public static void main(String args[]) throws Exception {
        WSSConfig.getDefaultWSConfig();
        new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssc/server/server.xml");
        new Server("http://localhost:9001/");
        System.out.println("Server ready...");

        Thread.sleep(60 * 60 * 10000);
        System.out.println("Server exiting");
        System.exit(0);
    }
    
    
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "SecureConversation_MutualCertificate10SignEncrypt_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class SCMCSEIPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "AC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class ACIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "ADC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class ADCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "ADC-ES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class ADCESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_A_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class AIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_AD_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class ADIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_AD-ES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class ADESIPingService extends PingServiceImpl {
    }

    
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "UXC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "UXDC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXDCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "UXDC-SEES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXDCSEESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_UX_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_UXD_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXDIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_UXD-SEES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class UXDSEESIPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "XC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "XDC_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "XDC_IPingService1", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDC1IPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "XDC-ES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDCESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "XDC-SEES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDCSEESIPingService extends PingServiceImpl {
    }
    
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_X_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_X10_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class X10IPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_XD_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_XD-SEES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDSEESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc", 
                serviceName = "PingService", 
                portName = "_XD-ES_IPingService", 
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wsspec/wssc/WSSecureConversation.wsdl")
    public static class XDESIPingService extends PingServiceImpl {
    }
}
