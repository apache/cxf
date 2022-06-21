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
package org.apache.cxf.systest.sts.sendervouches;

import java.net.URL;

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.feature.Features;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.feature.LoggingFeature")
public class DoubleItPortTypeImpl extends AbstractBusClientServerTestBase implements DoubleItPortType {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @Resource
    WebServiceContext wsc;

    private String port;

    public int doubleIt(int numberToDouble) {
        // Delegate request to a provider
        URL wsdl = DoubleItPortTypeImpl.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2SupportingPort");
        DoubleItPortType transportSAML2SupportingPort =
            service.getPort(portQName, DoubleItPortType.class);
        try {
            updateAddressPort(transportSAML2SupportingPort, getPort());
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //
        // Get the principal from the request context and construct a SAML Assertion
        //
        Saml2CallbackHandler callbackHandler = new Saml2CallbackHandler(wsc.getUserPrincipal());
        ((BindingProvider)transportSAML2SupportingPort).getRequestContext().put(
            SecurityConstants.SAML_CALLBACK_HANDLER, callbackHandler
        );

        return transportSAML2SupportingPort.doubleIt(numberToDouble);
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

}
