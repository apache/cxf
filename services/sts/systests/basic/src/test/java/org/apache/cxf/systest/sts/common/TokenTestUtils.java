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
package org.apache.cxf.systest.sts.common;

import java.util.List;


import org.w3c.dom.Element;

import jakarta.xml.ws.BindingProvider;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.trust.STSClient;
import org.example.contract.doubleit.DoubleItPortType;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class TokenTestUtils {

    private TokenTestUtils() {
        // complete
    }

    public static void verifyToken(DoubleItPortType port) throws Exception {
        Client client = ClientProxy.getClient(port);
        Endpoint ep = client.getEndpoint();
        String id = (String)ep.get(SecurityConstants.TOKEN_ID);
        TokenStore store = (TokenStore)ep.getEndpointInfo().getProperty(TokenStore.class.getName());
        org.apache.cxf.ws.security.tokenstore.SecurityToken tok = store.getToken(id);
        assertNotNull(tok);
        STSClient sts = (STSClient)ep.get(SecurityConstants.STS_CLIENT);
        if (sts == null) {
            sts = (STSClient)ep.get("ws-" + SecurityConstants.STS_CLIENT);
        }

        List<SecurityToken> validTokens = sts.validateSecurityToken(tok);
        assertTrue(validTokens != null && !validTokens.isEmpty());

        //mess with the token a bit to force it to fail to validate
        Element e = tok.getToken();
        Element e2 = DOMUtils.getFirstChildWithName(e, e.getNamespaceURI(), "Conditions");
        String nb = e2.getAttributeNS(null, "NotBefore");
        String noa = e2.getAttributeNS(null, "NotOnOrAfter");
        nb = "2010" + nb.substring(4);
        noa = "2010" + noa.substring(4);
        e2.setAttributeNS(null, "NotBefore", nb);
        e2.setAttributeNS(null, "NotOnOrAfter", noa);
        try {
            sts.validateSecurityToken(tok);
            fail("Failure expected on an invalid token");
        } catch (org.apache.cxf.ws.security.trust.TrustException ex) {
            // expected
        }
    }

    public static void updateSTSPort(BindingProvider p, String port) {
        STSClient stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT);
        if (stsClient == null) {
            stsClient = (STSClient)p.getRequestContext().get("ws-" + SecurityConstants.STS_CLIENT);
        }
        if (stsClient != null) {
            String location = stsClient.getWsdlLocation();
            if (location != null && location.contains("8080")) {
                stsClient.setWsdlLocation(location.replace("8080", port));
            } else if (location != null && location.contains("8443")) {
                stsClient.setWsdlLocation(location.replace("8443", port));
            }
        }
        stsClient = (STSClient)p.getRequestContext().get(SecurityConstants.STS_CLIENT + ".sct");
        if (stsClient == null) {
            stsClient = (STSClient)p.getRequestContext().get("ws-" + SecurityConstants.STS_CLIENT + ".sct");
        }
        if (stsClient != null) {
            String location = stsClient.getWsdlLocation();
            if (location.contains("8080")) {
                stsClient.setWsdlLocation(location.replace("8080", port));
            } else if (location.contains("8443")) {
                stsClient.setWsdlLocation(location.replace("8443", port));
            }
        }
    }

}
