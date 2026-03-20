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

import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.JavaUtils;
import org.apache.cxf.systest.ws.common.KeystorePasswordCallback;
import org.apache.cxf.systest.ws.common.UTPasswordCallback;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;
import org.apache.cxf.ws.security.SecurityConstants;


public class StaxServer extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(StaxServer.class);
    static final String PORT2 = allocatePort(StaxServer.class, 2);

    public StaxServer() throws Exception {

    }

    protected StaxServer(String baseUrl) throws Exception {

        doPublish(baseUrl.replace(PORT, PORT2).replace("http", "https")
                + "SecureConversation_UserNameOverTransport_IPingService",
                JavaUtils.isFIPSEnabled()
                ? new SCTLSPingServiceFips()
                : new SCTLSPingService());
        doPublish(baseUrl + "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
                  JavaUtils.isFIPSEnabled()
                  ? new SCMCSEIPingServiceFips()
                      : new SCMCSEIPingService());

        doPublish(baseUrl + "AC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new ACIPingServiceFips()
                      : new ACIPingService());
        doPublish(baseUrl + "ADC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new ADCIPingServiceFips()
                      : new ADCIPingService());
        doPublish(baseUrl + "ADC-ES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new ADCESIPingServiceFips()
                      : new ADCESIPingService());
        doPublish(baseUrl + "_A_IPingService", JavaUtils.isFIPSEnabled()
                  ? new AIPingServiceFips()
                      : new AIPingService());
        doPublish(baseUrl + "_AD_IPingService", JavaUtils.isFIPSEnabled()
                  ? new ADIPingServiceFips()
                      : new ADIPingService());
        doPublish(baseUrl + "_AD-ES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new ADESIPingServiceFips()
                      : new ADESIPingService());

        doPublish(baseUrl + "UXC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXCIPingServiceFips()
                      : new UXCIPingService());
        doPublish(baseUrl + "UXDC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXDCIPingServiceFips()
                      : new UXDCIPingService());
        doPublish(baseUrl + "UXDC-SEES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXDCSEESIPingServiceFips()
                      : new UXDCSEESIPingService());
        doPublish(baseUrl + "_UX_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXIPingServiceFips()
                      : new UXIPingService());
        doPublish(baseUrl + "_UXD_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXDIPingServiceFips()
                      : new UXDIPingService());
        doPublish(baseUrl + "_UXD-SEES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new UXDSEESIPingServiceFips()
                      : new UXDSEESIPingService());

        doPublish(baseUrl + "XC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XCIPingServiceFips()
                      : new XCIPingService());
        doPublish(baseUrl + "XDC_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XDCIPingServiceFips()
                      : new XDCIPingService());
        doPublish(baseUrl + "XDC_IPingService1", JavaUtils.isFIPSEnabled()
                  ? new XDC1IPingServiceFips()
                      : new XDC1IPingService());
        doPublish(baseUrl + "XDC-ES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XDCESIPingServiceFips()
                      : new XDCESIPingService());
        doPublish(baseUrl + "XDC-SEES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XDCSEESIPingServiceFips()
                      : new XDCSEESIPingService());
        doPublish(baseUrl + "_X_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XIPingServiceFips()
                      : new XIPingService());
        doPublish(baseUrl + "_X10_IPingService", JavaUtils.isFIPSEnabled()
                  ? new X10IPingServiceFips()
                      : new X10IPingService());
        doPublish(baseUrl + "_XD_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XDIPingServiceFips()
                      : new XDIPingService());
        doPublish(baseUrl + "_XD-SEES_IPingService", JavaUtils.isFIPSEnabled()
                  ? new XDSEESIPingServiceFips()
                      : new XDSEESIPingService());
        doPublish(baseUrl + "_XD-ES_IPingService",  JavaUtils.isFIPSEnabled()
                  ? new XDESIPingServiceFips()
                      : new XDESIPingService());


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
            Bus busLocal = new SpringBusFactory().createBus(
                "org/apache/cxf/systest/ws/wssc/server.xml");
            BusFactory.setDefaultBus(busLocal);
            setBus(busLocal);
            new StaxServer("http://localhost:" + PORT + "/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doPublish(String url, Object obj) {
        Endpoint ep = Endpoint.create(obj);
        ep.getProperties().put(SecurityConstants.ENABLE_STREAMING_SECURITY, "true");
        ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER + ".sct", new KeystorePasswordCallback());
        ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct",
                "bob.properties");

        if (url.contains("X10_I")) {
            ep.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES + ".sct",
                    "bob.properties");
            ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct",
                    "alice.properties");
        } else if (url.contains("MutualCert")) {
            ep.getProperties().put(SecurityConstants.ENCRYPT_PROPERTIES + ".sct",
                "bob.properties");
            ep.getProperties().put(SecurityConstants.SIGNATURE_PROPERTIES + ".sct",
                "alice.properties");
            ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER, new KeystorePasswordCallback());
        } else if (url.contains("UserNameOverTransport")) {
            ep.getProperties().put(SecurityConstants.CALLBACK_HANDLER + ".sct", new UTPasswordCallback());
        }
        ep.publish(url);
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "SecureConversation_UserNameOverTransport_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class SCTLSPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class SCMCSEIPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "AC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class ACIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "ADC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class ADCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "ADC-ES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class ADCESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_A_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class AIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_AD_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class ADIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_AD-ES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class ADESIPingService extends PingServiceImpl {
    }


    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "UXC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "UXDC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXDCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "UXDC-SEES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXDCSEESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_UX_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_UXD_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXDIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_UXD-SEES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class UXDSEESIPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "XC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "XDC_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDCIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "XDC_IPingService1",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDC1IPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "XDC-ES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDCESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "XDC-SEES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDCSEESIPingService extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_X_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_X10_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class X10IPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_XD_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_XD-SEES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDSEESIPingService extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
                serviceName = "PingService",
                portName = "_XD-ES_IPingService",
                endpointInterface = "wssec.wssc.IPingService",
                wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation.wsdl")
    public static class XDESIPingService extends PingServiceImpl {
    }
    
    
    
    @WebService(targetNamespace = "http://WSSec/wssc",
        serviceName = "PingService",
        portName = "SecureConversation_UserNameOverTransport_IPingService",
        endpointInterface = "wssec.wssc.IPingService",
        wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class SCTLSPingServiceFips extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "SecureConversation_MutualCertificate10SignEncrypt_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class SCMCSEIPingServiceFips extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "AC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class ACIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "ADC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class ADCIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "ADC-ES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class ADCESIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_A_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class AIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_AD_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class ADIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_AD-ES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class ADESIPingServiceFips extends PingServiceImpl {
    }


    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "UXC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXCIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "UXDC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXDCIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "UXDC-SEES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXDCSEESIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_UX_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_UXD_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXDIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_UXD-SEES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class UXDSEESIPingServiceFips extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "XC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XCIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "XDC_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDCIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "XDC_IPingService1",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDC1IPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "XDC-ES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDCESIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "XDC-SEES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDCSEESIPingServiceFips extends PingServiceImpl {
    }

    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_X_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_X10_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class X10IPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_XD_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_XD-SEES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDSEESIPingServiceFips extends PingServiceImpl {
    }
    @WebService(targetNamespace = "http://WSSec/wssc",
            serviceName = "PingService",
            portName = "_XD-ES_IPingService",
            endpointInterface = "wssec.wssc.IPingService",
            wsdlLocation = "target/test-classes/wsdl_systest_wssec/wssc/WSSecureConversation-fips.wsdl")
    public static class XDESIPingServiceFips extends PingServiceImpl {
    }
}
