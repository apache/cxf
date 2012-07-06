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
public class RequestState implements Serializable {

    private static final long serialVersionUID = 869323136115571943L;
    
    private String targetAddress;
    private String idpServiceAddress;
    private String samlRequestId;
    private String issuerId;
    private String webAppContext;
    private String webAppDomain;
    private long createdAt;
 
    public RequestState() {
        
    }
    
    public RequestState(String targetAddress,
                        String idpServiceAddress,
                        String samlRequestId,
                        String issuerId,
                        String webAppContext,
                        String webAppDomain,
                        long createdAt) {
        this.targetAddress = targetAddress;
        this.idpServiceAddress = idpServiceAddress;
        this.samlRequestId = samlRequestId;
        this.issuerId = issuerId;
        this.webAppContext = webAppContext;
        this.webAppDomain = webAppDomain;
        this.createdAt = createdAt;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public String getIdpServiceAddress() {
        return idpServiceAddress;
    }

    public String getSamlRequestId() {
        return samlRequestId;
    }

    public String getIssuerId() {
        return issuerId;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String getWebAppContext() {
        return webAppContext;
    }

    public String getWebAppDomain() {
        return webAppDomain;
    }
}
