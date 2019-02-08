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
package org.apache.cxf.systest.sts.intermediary_transformation;

import java.net.URL;
import java.security.Principal;
import java.util.Map;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.feature.Features;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.feature.LoggingFeature")
public class IntermediaryPortTypeImpl extends AbstractBusClientServerTestBase implements DoubleItPortType {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @Resource
    WebServiceContext wsc;

    public int doubleIt(int numberToDouble) {
        Principal pr = wsc.getUserPrincipal();

        Assert.assertNotNull("Principal must not be null", pr);
        Assert.assertNotNull("Principal.getName() must not return null", pr.getName());

        URL wsdl = IntermediaryPortTypeImpl.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        try {
            updateAddressPort(transportPort, IntermediaryTransformationTest.PORT2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if ("standalone".equals(System.getProperty("sts.deployment"))) {
            Map<String, Object> context = ((BindingProvider)transportPort).getRequestContext();
            STSClient stsClient = (STSClient)context.get(SecurityConstants.STS_CLIENT);
            if (stsClient == null) {
                stsClient = (STSClient)context.get("ws-" + SecurityConstants.STS_CLIENT);
            }
            if (stsClient != null) {
                String location = stsClient.getWsdlLocation();
                if (location.contains("8080")) {
                    stsClient.setWsdlLocation(
                        location.replace("8080", IntermediaryTransformationTest.STSPORT2)
                    );
                } else if (location.contains("8443")) {
                    stsClient.setWsdlLocation(
                        location.replace("8443", IntermediaryTransformationTest.STSPORT)
                    );
                }
            }
        }

        return transportPort.doubleIt(numberToDouble);
    }

}
