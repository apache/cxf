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
package org.apache.cxf.rs.security.saml.sso.filter;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.core.UriInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.ext.RequestHandler;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.apache.ws.security.util.DOM2Writer;
import org.opensaml.common.SAMLVersion;
import org.opensaml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml2.core.AuthnContextComparisonTypeEnumeration;
import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.saml2.core.Issuer;
import org.opensaml.saml2.core.NameIDPolicy;
import org.opensaml.saml2.core.RequestedAuthnContext;
import org.opensaml.xml.io.MarshallingException;

public abstract class AbstractServiceProviderFilter implements RequestHandler {
    
    protected static final String SAML_REQUEST = "SAMLRequest"; 
    protected static final String RELAY_STATE = "RelayState";
    
    private String idpServiceAddress;
    private String issuerId;
    private String assertionConsumerServiceAddress;
    
    public String getAssertionConsumerServiceAddress() {
        return assertionConsumerServiceAddress;
    }

    public void setAssertionConsumerServiceAddress(
            String assertionConsumerServiceAddress) {
        this.assertionConsumerServiceAddress = assertionConsumerServiceAddress;
    }

    protected boolean checkSecurityContext(Message m) {
        return false;
    }

    public void setIdpServiceAddress(String idpServiceAddress) {
        this.idpServiceAddress = idpServiceAddress;
    }

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }

    protected AuthnRequest createAuthnRequest(Message m, Document doc) throws Exception {
        Issuer issuer =
            SamlpRequestComponentBuilder.createIssuer(issuerId);
        NameIDPolicy nameIDPolicy =
            SamlpRequestComponentBuilder.createNameIDPolicy(
                true, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", "Issuer"
            );
        
        AuthnContextClassRef authnCtxClassRef =
            SamlpRequestComponentBuilder.createAuthnCtxClassRef(
                "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport"
            );
        RequestedAuthnContext authnCtx =
            SamlpRequestComponentBuilder.createRequestedAuthnCtxPolicy(
                AuthnContextComparisonTypeEnumeration.EXACT,
                Collections.singletonList(authnCtxClassRef), null
            );
        
        //CHECKSTYLE:OFF
        return SamlpRequestComponentBuilder.createAuthnRequest(
                assertionConsumerServiceAddress, 
                false, 
                false,
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST", 
                SAMLVersion.VERSION_20,
                issuer, 
                nameIDPolicy, 
                authnCtx
        );
      //CHECKSTYLE:ON
    }
    
    protected String encodeAuthnRequest(Element authnRequestElement)
        throws MarshallingException, IOException {
        String requestMessage = DOM2Writer.nodeToString(authnRequestElement);
        
        DeflateEncoderDecoder encoder = new DeflateEncoderDecoder();
        byte[] deflatedBytes = encoder.deflateToken(requestMessage.getBytes("UTF-8"));
        
        String encodedRequestMessage = Base64Utility.encode(deflatedBytes);
        return URLEncoder.encode(encodedRequestMessage, "UTF-8");
    }

    protected SamlRequestInfo createSamlRequestInfo(Message m) throws Exception {
        Document doc = DOMUtils.createDocument();
        doc.appendChild(doc.createElement("root"));
 
        AuthnRequest authnRequest = createAuthnRequest(m, doc);
        Element authnRequestElement = OpenSAMLUtil.toDom(authnRequest, doc);
        String authnRequestEncoded = encodeAuthnRequest(authnRequestElement);
        
        SamlRequestInfo info = new SamlRequestInfo();
        info.setEncodedSamlRequest(authnRequestEncoded);
        info.setRelayState(UUID.randomUUID().toString());
        return info;
    }
    
    public void setIssuerId(String issuerId) {
        this.issuerId = issuerId;
    }
}
