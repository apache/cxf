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
package org.apache.cxf.rt.security.saml.xacml;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.message.Message;
import org.apache.cxf.rt.security.saml.claims.SAMLSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;

public class CXFMessageParser {
    private Message message;

    /**
     * @param message
     */
    public CXFMessageParser(Message message) {
        this.message = message;
    }

    public boolean isSOAPService() {
        return getWSDLOperation() != null;
    }

    public QName getWSDLOperation() {
        if (message != null && message.get(Message.WSDL_OPERATION) != null) {
            return (QName)message.get(Message.WSDL_OPERATION);
        }
        return null;
    }

    public QName getWSDLService() {
        if (message != null && message.get(Message.WSDL_SERVICE) != null) {
            return (QName)message.get(Message.WSDL_SERVICE);
        }
        return null;
    }

    /**
     * @param fullRequestURL Whether to send the full Request URL as the resource or not. If set to true, the
     *        full Request URL will be sent for both a JAX-WS and JAX-RS service. If set to false (the
     *        default), a JAX-WS service will send the "{namespace}operation" QName, and a JAX-RS service
     *        will send the RequestURI (i.e. minus the initial https:<ip> prefix)
     */
    public String getResourceURI(boolean fullRequestURL) {
        String property = fullRequestURL ? Message.REQUEST_URL : Message.REQUEST_URI;
        if (message != null && message.get(property) != null) {
            return (String)message.get(property);
        }
        return null;
    }

    public String getAction(String defaultSOAPAction) {
        String actionToUse = defaultSOAPAction;
        // For REST use the HTTP Verb
        if (message.get(Message.WSDL_OPERATION) == null && message.get(Message.HTTP_REQUEST_METHOD) != null) {
            actionToUse = (String)message.get(Message.HTTP_REQUEST_METHOD);
        }
        return actionToUse;
    }

    /**
     * Get the Issuer of the SAML Assertion
     */
    public String getIssuer() throws WSSecurityException {
        SecurityContext sc = message.get(SecurityContext.class);

        if (sc instanceof SAMLSecurityContext) {
            Element assertionElement = ((SAMLSecurityContext)sc).getAssertionElement();
            if (assertionElement != null) {
                SamlAssertionWrapper wrapper = new SamlAssertionWrapper(assertionElement);
                return wrapper.getIssuerString();
            }
        }

        return null;
    }
}
