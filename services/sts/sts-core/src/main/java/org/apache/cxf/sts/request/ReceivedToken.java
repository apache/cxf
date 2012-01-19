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
package org.apache.cxf.sts.request;

import java.security.Principal;
import java.util.logging.Logger;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.STSException;

/**
 * This class contains values that have been extracted from a received Token. The Token can be a
 * JAXB UsernameTokenType/BinarySecurityTokenType or a DOM Element.
 */
public class ReceivedToken {
    
    private static final Logger LOG = LogUtils.getL7dLogger(ReceivedToken.class);
    
    private Object token;
    private boolean isBinarySecurityToken;
    private boolean isUsernameToken;
    private boolean isDOMElement;
    private String tokenContext; // WS-Security, OnBehalfOf, ActAs
    private STATE validationState;
    private Principal principal;
    
    public enum STATE { VALID, INVALID, NONE };
    
    public ReceivedToken(Object receivedToken) throws STSException {
        if (receivedToken instanceof JAXBElement<?>) {
            QName parentName = ((JAXBElement<?>)receivedToken).getName();
            if (QNameConstants.USERNAME_TOKEN.equals(parentName)) {
                isUsernameToken = true;
                LOG.fine("Found a UsernameToken");
            } else if (QNameConstants.BINARY_SECURITY_TOKEN.equals(parentName)) {
                LOG.fine("Found a BinarySecurityToken");
                isBinarySecurityToken = true;
            } else if (QNameConstants.SECURITY_TOKEN_REFERENCE.equals(parentName)) {
                LOG.fine("Found SecurityTokenReference");                
            } else {
                LOG.fine("Found unknown token object: " + parentName);
                throw new STSException(
                    "An unknown element was received", STSException.BAD_REQUEST
                );
            }
            token = ((JAXBElement<?>)receivedToken).getValue();
        } else if (receivedToken instanceof Element) {
            LOG.fine("Found ValidateTarget element: " + ((Element)receivedToken).getLocalName());
            this.token = (Element)receivedToken;
            isDOMElement = true;
        } else {
            LOG.fine("Found ValidateTarget object of unknown type");
            throw new STSException(
                "An unknown element was received", STSException.BAD_REQUEST
            );
        }
    }
    
    public Object getToken() {
        return token;
    }

    public void setToken(Object token) {
        this.token = token;
    }
    
    public boolean isBinarySecurityToken() {
        return isBinarySecurityToken;
    }

    public void setBinarySecurityToken(boolean binarySecurityToken) {
        this.isBinarySecurityToken = binarySecurityToken;
    }

    public boolean isUsernameToken() {
        return isUsernameToken;
    }

    public void setUsernameToken(boolean usernameToken) {
        this.isUsernameToken = usernameToken;
    }
    
    public boolean isDOMElement() {
        return isDOMElement;
    }

    public void setDOMElement(boolean domElement) {
        this.isDOMElement = domElement;
    }
    
    public String getTokenContext() {
        return tokenContext;
    }

    public void setTokenContext(String tokenContext) {
        this.tokenContext = tokenContext;
    }

    public STATE getValidationState() {
        return validationState;
    }

    public void setValidationState(STATE validationState) {
        this.validationState = validationState;
    }

    public Principal getPrincipal() {
        return principal;
    }

    public void setPrincipal(Principal principal) {
        this.principal = principal;
    }
    
}