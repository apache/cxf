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
package org.apache.cxf.rs.security.oauth2.services;

import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;

/**
 * Abstract OAuth service
 */
public abstract class AbstractOAuthService {
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractOAuthService.class);
    private MessageContext mc;
    private OAuthDataProvider dataProvider;
    private boolean blockUnsecureRequests;
    
    @Context 
    public void setMessageContext(MessageContext context) {
        this.mc = context;    
    }
    
    public MessageContext getMessageContext() {
        return mc;
    }

    public void setDataProvider(OAuthDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    public OAuthDataProvider getDataProvider() {
        return dataProvider;
    }
    
    protected MultivaluedMap<String, String> getQueryParameters() {
        return getMessageContext().getUriInfo().getQueryParameters();
    }
    
    protected Client getClient(MultivaluedMap<String, String> params) {
        return getClient(params.getFirst(OAuthConstants.CLIENT_ID));
    }
    /**
     * Get the {@link Client} reference
     * @param clientId the provided client id
     * @return Client the client reference 
     * @throws WebApplicationException if no matching Client is found, 
     *         the error is returned directly to the end user without 
     *         following the redirect URI if any
     */
    protected Client getClient(String clientId) {
        Client client = null;
        
        if (clientId != null) {
            try {
                client = dataProvider.getClient(clientId);
            } catch (OAuthServiceException ex) {
                // log it
            }
        }
        if (client == null) {
            reportInvalidRequestError("Client ID is invalid");
        }
        return client;
        
    }
    
    /**
     * HTTPS is the default transport for OAuth 2.0 services.
     * By default this method will issue a warning for open 
     * endpoints
     */
    protected void checkTransportSecurity() {  
        if (!mc.getSecurityContext().isSecure()) {
            LOG.warning("Unsecure HTTP, Transport Layer Security is recommended");
            if (blockUnsecureRequests) {
                throw new WebApplicationException(400);    
            }
        }
    }
    
    protected void reportInvalidRequestError(String errorDescription) {
        OAuthError error = 
            new OAuthError(OAuthConstants.INVALID_REQUEST, errorDescription);
        throw new WebApplicationException(
                  Response.status(400).type(MediaType.APPLICATION_JSON).entity(error).build());
    }

    /**
     * HTTPS is the default transport for OAuth 2.0 services, this property 
     * can be used to block all the requests issued over HTTP
     * 
     * @param blockUnsecureRequests if set to true then HTTP requests will be blocked
     */
    public void setBlockUnsecureRequests(boolean blockUnsecureRequests) {
        this.blockUnsecureRequests = blockUnsecureRequests;
    }
}
