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
package org.apache.cxf.rs.security.saml.sso.state;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class ResponseState implements Serializable {

    private static final long serialVersionUID = -3247188797004342462L;
    
    private String assertion;
    private String relayState;
    private String webAppContext;
    private String webAppDomain;
    private long createdAt;
    private long expiresAt;
    
    public ResponseState() {
        
    }
    
    public ResponseState(String assertion,
                         String relayState,
                         String webAppContext,
                         String webAppDomain,
                         long createdAt, 
                         long expiresAt) {
        this.assertion = assertion;
        this.relayState = relayState;
        this.webAppContext = webAppContext;
        this.webAppDomain = webAppDomain;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }
    
    public long getExpiresAt() {
        return expiresAt;
    }

    public String getRelayState() {
        return relayState;
    }
    
    public String getWebAppContext() {
        return webAppContext;
    }

    public String getWebAppDomain() {
        return webAppDomain;
    }
    
    public String getAssertion() {
        return assertion;
    }
}
