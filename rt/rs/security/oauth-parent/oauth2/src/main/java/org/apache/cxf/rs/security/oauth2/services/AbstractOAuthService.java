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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.OAuthDataProvider;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.rs.security.oauth2.utils.OAuthUtils;

/**
 * Abstract OAuth service
 */
public abstract class AbstractOAuthService {
    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractOAuthService.class);
    private MessageContext mc;
    private OAuthDataProvider dataProvider;
    private boolean blockUnsecureRequests;
    private boolean writeOptionalParameters = true;
    
    public void setWriteOptionalParameters(boolean write) {
        writeOptionalParameters = write;
    }

    public boolean isWriteOptionalParameters() {
        return writeOptionalParameters;
    }

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
        injectContextIntoOAuthProviders();
    }

    protected void injectContextIntoOAuthProviders() {
        OAuthUtils.injectContextIntoOAuthProvider(mc, dataProvider);
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

    /**
     * Get the {@link Client} reference
     * @param clientId the provided client id
     * @return Client the client reference
     * @throws {@link OAuthServiceExcepption} if no matching Client is found
     */
    protected Client getValidClient(String clientId, MultivaluedMap<String, String> params)
        throws OAuthServiceException {
        return getValidClient(clientId, params.getFirst(OAuthConstants.CLIENT_SECRET), params);
    }
    
    protected Client getValidClient(String clientId, String clientSecret, MultivaluedMap<String, String> params)
        throws OAuthServiceException {
        if (clientId != null) {
            mc.put(OAuthConstants.CLIENT_SECRET, clientSecret);
            mc.put(OAuthConstants.GRANT_TYPE, params.getFirst(OAuthConstants.GRANT_TYPE));
            mc.put(OAuthConstants.TOKEN_REQUEST_PARAMS, params);
            return dataProvider.getClient(clientId);
        }
        LOG.fine("No valid client found as the given clientId is null");
        return null;
    }

    /**
     * HTTPS is the default transport for OAuth 2.0 services.
     * By default this method will issue a warning for open
     * endpoints
     */
    protected void checkTransportSecurity() {
        if (!mc.getSecurityContext().isSecure()) {
            LOG.warning("Unsecure HTTP, HTTPS is recommended");
            if (blockUnsecureRequests) {
                throw ExceptionUtils.toBadRequestException(null, null);
            }
        }
    }

    protected void reportInvalidRequestError(String errorDescription) {
        reportInvalidRequestError(errorDescription, MediaType.APPLICATION_JSON_TYPE);
    }

    protected void reportInvalidRequestError(String errorDescription, MediaType mt) {
        OAuthError error =
            new OAuthError(OAuthConstants.INVALID_REQUEST, errorDescription);
        reportInvalidRequestError(error, mt);
    }

    protected void reportInvalidRequestError(OAuthError entity) {
        reportInvalidRequestError(entity, MediaType.APPLICATION_JSON_TYPE);
    }

    protected void reportInvalidRequestError(OAuthError entity, MediaType mt) {
        ResponseBuilder rb = JAXRSUtils.toResponseBuilder(400);
        if (mt != null) {
            rb.type(mt);
        }
        throw ExceptionUtils.toBadRequestException(null, rb.entity(entity).build());
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
