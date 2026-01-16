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

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.WebServiceContext;
import org.apache.cxf.feature.Features;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.delegation.ReceivedTokenCallbackHandler;
import org.example.contract.doubleit.DoubleItPortType;

import org.junit.Assert;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class IntermediaryCachingPortTypeImpl extends AbstractClientServerTestBase
    implements DoubleItPortType {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @Resource
    private WebServiceContext wsc;

    private int i;

    private DoubleItPortType transportPort;

    public int doubleIt(int numberToDouble) {
        if (transportPort == null) {
            // Re-use the same proxy
            URL wsdl = IntermediaryCachingPortTypeImpl.class.getResource("DoubleIt.wsdl");
            Service service = Service.create(wsdl, SERVICE_QNAME);
            QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
            transportPort = service.getPort(portQName, DoubleItPortType.class);
            try {
                updateAddressPort(transportPort, IntermediaryTransformationCachingTest.PORT2);
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
                            location.replace("8080", IntermediaryTransformationCachingTest.STSPORT2)
                        );
                    } else if (location.contains("8443")) {
                        stsClient.setWsdlLocation(
                            location.replace("8443", IntermediaryTransformationCachingTest.STSPORT)
                        );
                    }
                }
            }
        }
        Principal pr = wsc.getUserPrincipal();

        Assert.assertNotNull("Principal must not be null", pr);
        Assert.assertNotNull("Principal.getName() must not return null", pr.getName());
        // Assert.assertTrue("Principal must be alice", pr.getName().contains("alice"));

        // Disable the STSClient after the second invocation
        if (i > 1) {
            BindingProvider p = (BindingProvider)transportPort;
            STSClient stsClient = new STSClient(null);
            stsClient.setOnBehalfOf(new ReceivedTokenCallbackHandler());
            p.getRequestContext().put(SecurityConstants.STS_CLIENT, stsClient);
        }

        i++;
        return transportPort.doubleIt(numberToDouble);
    }

}
