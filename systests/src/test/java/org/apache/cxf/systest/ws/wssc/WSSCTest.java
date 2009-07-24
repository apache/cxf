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

package org.apache.cxf.systest.ws.wssc;

import java.net.URL;

import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.systest.ws.wssc.server.Server;
import org.apache.cxf.systest.ws.wssec11.WSSecurity11Common;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xmlsoap.ping.Ping;

import wssec.wssc.IPingService;
import wssec.wssc.PingRequest;
import wssec.wssc.PingResponse;
import wssec.wssc.PingService;

/**
 *
 */
public class WSSCTest extends AbstractBusClientServerTestBase {

       
    private static final String OUT = "CXF : ping";

    @BeforeClass
    public static void startServers() throws Exception {
        if (!WSSecurity11Common.checkUnrestrictedPoliciesInstalled()) {
            //do nothing
            return;
        } 
        assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            launchServer(Server.class, true)
        );
    }

    @Test
    public void testClientServer() throws Exception {
        if (!WSSecurity11Common.checkUnrestrictedPoliciesInstalled()) {
            //do nothing
            return;
        }
        String[] argv = new String[] {
            "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
            "AC_IPingService",
            "ADC_IPingService",
            "ADC-ES_IPingService",   
            "_A_IPingService",
            "_AD_IPingService",
            "_AD-ES_IPingService",
            
            "UXC_IPingService", 
            "UXDC_IPingService",
            "UXDC-SEES_IPingService",
            "_UX_IPingService",
            "_UXD_IPingService",
            "_UXD-SEES_IPingService", 
            
            
            "XC_IPingService", 
            "XDC_IPingService", 
            "XDC_IPingService1", 
            "XDC-ES_IPingService", 
            "XDC-SEES_IPingService", 
            "_X_IPingService", 
            "_X10_IPingService", 
            "_XD_IPingService", 
            "_XD-SEES_IPingService", 
            "_XD-ES_IPingService",
        };
        //argv = new String[] {argv[1]};
        final Bus bus = 
            new SpringBusFactory().createBus("org/apache/cxf/systest/ws/wssc/client/client.xml");
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);
        URL wsdlLocation = null;
        
        for (String portPrefix : argv) {
            PingService svc;
            wsdlLocation = new URL("http://localhost:9001/" + portPrefix + "?wsdl");
            
            svc = new PingService(wsdlLocation);
            final IPingService port = 
                svc.getPort(
                    new QName(
                        "http://WSSec/wssc",
                        portPrefix
                    ),
                    IPingService.class
                );
           
            
            if (portPrefix.charAt(0) == '_') {
                //MS would like the _ versions to send a cancel
                ((BindingProvider)port).getRequestContext()
                    .put(SecurityConstants.STS_TOKEN_DO_CANCEL, Boolean.TRUE);
            }
            PingRequest params = new PingRequest();
            Ping ping = new Ping();
            ping.setOrigin("CXF");
            ping.setScenario("Scenario5");
            ping.setText("ping");
            params.setPing(ping);
            try {
                PingResponse output = port.ping(params);
                assertEquals(OUT, output.getPingResponse().getText());
            } catch (Exception ex) {
                throw new Exception("Error doing " + portPrefix, ex);
            }
        }
    }

    
}
