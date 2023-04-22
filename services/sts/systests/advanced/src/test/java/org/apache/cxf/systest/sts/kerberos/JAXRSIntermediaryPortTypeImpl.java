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
package org.apache.cxf.systest.sts.kerberos;

import java.net.URL;
import java.util.Map;

import javax.xml.namespace.QName;

import jakarta.jws.WebService;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Service;
import org.apache.cxf.feature.Features;
import org.apache.cxf.jaxrs.security.KerberosAuthenticationFilter.KerberosSecurityContext;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItPortType")
@Features(classes = org.apache.cxf.ext.logging.LoggingFeature.class)
public class JAXRSIntermediaryPortTypeImpl extends AbstractBusClientServerTestBase implements DoubleItPortType {

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    public int doubleIt(int numberToDouble) {
        URL wsdl = JAXRSIntermediaryPortTypeImpl.class.getResource("DoubleIt.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItTransportSAML2Port");
        DoubleItPortType transportPort =
            service.getPort(portQName, DoubleItPortType.class);
        try {
            updateAddressPort(transportPort, KerberosDelegationTokenTest.PORT);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Retrieve delegated credential + set it on the outbound message
        SecurityContext securityContext =
            PhaseInterceptorChain.getCurrentMessage().get(SecurityContext.class);
        if (securityContext instanceof KerberosSecurityContext) {
            KerberosSecurityContext ksc = (KerberosSecurityContext)securityContext;
            try {
                GSSCredential delegatedCredential = ksc.getGSSContext().getDelegCred();
                Map<String, Object> context = ((BindingProvider)transportPort).getRequestContext();
                context.put(SecurityConstants.DELEGATED_CREDENTIAL, delegatedCredential);
            } catch (GSSException e) {
                e.printStackTrace();
            }
        }

        return transportPort.doubleIt(numberToDouble);
    }

}
