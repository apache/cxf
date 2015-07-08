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

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.provider.ClientIdProvider;
import org.apache.cxf.rs.security.oauth2.provider.ClientSecretVerifier;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rs.security.oauth2.utils.AuthorizationUtils;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;
import org.apache.cxf.security.transport.TLSSessionInfo;

public class AbstractTokenService extends AbstractOAuthService {
    private boolean canSupportPublicClients;
    private boolean writeCustomErrors;
    private ClientIdProvider clientIdProvider;
    private ClientSecretVerifier clientSecretVerifier;
    
    /**
     * Make sure the client is authenticated
     */
    protected Client authenticateClientIfNeeded(MultivaluedMap<String, String> params) {
        Client client = null;
        SecurityContext sc = getMessageContext().getSecurityContext();
        Principal principal = sc.getUserPrincipal();
        
        if (principal == null) {
            String clientId = retrieveClientId(params);
            if (clientId != null) {
                client = getAndValidateClientFromIdAndSecret(clientId,
                                              params.getFirst(OAuthConstants.CLIENT_SECRET));
            }
        } else if (principal.getName() != null) {
            client = getClient(principal.getName());
        } else {
            String clientId = retrieveClientId(params);
            if (clientId != null) {
                client = getClient(clientId);
            } 
        }
        if (client == null) {
            client = getClientFromTLSCertificates(sc, getTlsSessionInfo());
            if (client == null) {
                // Basic Authentication is expected by default
                client = getClientFromBasicAuthScheme();
            }
        }
        if (client != null && !client.getApplicationCertificates().isEmpty()) {
            // Validate the client application certificates
            compareTlsCertificates(getTlsSessionInfo(), client.getApplicationCertificates());
        }
        if (client == null) {
            reportInvalidClient();
        }
        return client;
    }
    
    private TLSSessionInfo getTlsSessionInfo() {

        return (TLSSessionInfo)getMessageContext().get(TLSSessionInfo.class.getName());
    }
    
    protected String retrieveClientId(MultivaluedMap<String, String> params) {
        String clientId = params.getFirst(OAuthConstants.CLIENT_ID);
        if (clientId == null) {
            clientId = (String)getMessageContext().get(OAuthConstants.CLIENT_ID);
        }
        if (clientId == null && clientIdProvider != null) {
            clientId = clientIdProvider.getClientId(getMessageContext());
        }
        return clientId;
    }
    
    // Get the Client and check the id and secret
    protected Client getAndValidateClientFromIdAndSecret(String clientId, String providedClientSecret) {
        Client client = getClient(clientId);
        if (!client.getClientId().equals(clientId)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        if (isValidPublicClient(client, clientId, providedClientSecret)) {
            return client;
        }
        if (!client.isConfidential()
            || !isConfidenatialClientSecretValid(client, providedClientSecret)) {
            throw ExceptionUtils.toNotAuthorizedException(null, null);
        }
        return client;
    }
    protected boolean isConfidenatialClientSecretValid(Client client, String providedClientSecret) {
        if (clientSecretVerifier != null) {
            return clientSecretVerifier.validateClientSecret(client, providedClientSecret);
        } else {
            return client.getClientSecret() != null 
                && providedClientSecret != null && client.getClientSecret().equals(providedClientSecret);
        }
    }
    protected boolean isValidPublicClient(Client client, String clientId, String clientSecret) {
        return canSupportPublicClients 
            && !client.isConfidential() 
            && client.getClientSecret() == null 
            && clientSecret == null;
    }
    
    protected Client getClientFromBasicAuthScheme() {
        String[] userInfo = AuthorizationUtils.getBasicAuthUserInfo(getMessageContext());
        if (userInfo != null && userInfo.length == 2) {
            return getAndValidateClientFromIdAndSecret(userInfo[0], userInfo[1]);
        } else {
            return null;
        }
    }
    
    protected Client getClientFromTLSCertificates(SecurityContext sc, TLSSessionInfo tlsSessionInfo) {
        Client client = null;
        if (tlsSessionInfo != null && StringUtils.isEmpty(sc.getAuthenticationScheme())) {
            // Pure 2-way TLS authentication
            String clientId = getClientIdFromTLSCertificates(sc, tlsSessionInfo);
            if (!StringUtils.isEmpty(clientId)) {
                client = getClient(clientId);
            }
        }
        return client;
    }
    
    protected String getClientIdFromTLSCertificates(SecurityContext sc, TLSSessionInfo tlsInfo) {
        Certificate[] clientCerts = tlsInfo.getPeerCertificates();
        if (clientCerts != null && clientCerts.length > 0) {
            X500Principal x509Principal = ((X509Certificate)clientCerts[0]).getSubjectX500Principal();
            return x509Principal.getName();    
        }
        return null;
    }
    
    protected void compareTlsCertificates(TLSSessionInfo tlsInfo, 
                                          List<String> base64EncodedCerts) {
        if (tlsInfo != null) {
            Certificate[] clientCerts = tlsInfo.getPeerCertificates();
            if (clientCerts.length == base64EncodedCerts.size()) {
                try {
                    for (int i = 0; i < clientCerts.length; i++) {
                        X509Certificate x509Cert = (X509Certificate)clientCerts[i];
                        byte[] encodedKey = x509Cert.getEncoded();
                        byte[] clientKey = Base64Utility.decode(base64EncodedCerts.get(i));
                        if (!Arrays.equals(encodedKey, clientKey)) {
                            reportInvalidClient();
                        }
                    }
                    return;
                } catch (Exception ex) {
                    // throw exception later
                }    
            }
        }
        reportInvalidClient();
    }
    
    
    
    protected Response handleException(OAuthServiceException ex, String error) {
        OAuthError customError = ex.getError();
        if (writeCustomErrors && customError != null) {
            return createErrorResponseFromBean(customError);
        } else {
            return createErrorResponseFromBean(new OAuthError(error));
        }
    }
    
    protected Response createErrorResponse(MultivaluedMap<String, String> params,
                                           String error) {
        return createErrorResponseFromBean(new OAuthError(error));
    }
    
    protected Response createErrorResponseFromBean(OAuthError errorBean) {
        return JAXRSUtils.toResponseBuilder(400).entity(errorBean).build();
    }
    
    /**
     * Get the {@link Client} reference
     * @param clientId the provided client id
     * @return Client the client reference 
     * @throws {@link javax.ws.rs.WebApplicationException} if no matching Client is found
     */
    protected Client getClient(String clientId) {
        if (clientId == null) {
            reportInvalidRequestError("Client ID is null");
            return null;
        }
        Client client = null;
        try {
            client = getValidClient(clientId);
        } catch (OAuthServiceException ex) {
            if (ex.getError() != null) {
                reportInvalidClient(ex.getError());
                return null;
            }
        }
        if (client == null) {
            reportInvalidClient();
        }
        return client;
    }
    
    protected void reportInvalidClient() {
        reportInvalidClient(new OAuthError(OAuthConstants.INVALID_CLIENT));
    }
    
    protected void reportInvalidClient(OAuthError error) {
        ResponseBuilder rb = JAXRSUtils.toResponseBuilder(401);
        throw ExceptionUtils.toNotAuthorizedException(null, 
            rb.type(MediaType.APPLICATION_JSON_TYPE).entity(error).build());
    }
    
    public void setCanSupportPublicClients(boolean support) {
        this.canSupportPublicClients = support;
    }

    public boolean isCanSupportPublicClients() {
        return canSupportPublicClients;
    }
    
    public void setWriteCustomErrors(boolean writeCustomErrors) {
        this.writeCustomErrors = writeCustomErrors;
    }

    public void setClientIdProvider(ClientIdProvider clientIdProvider) {
        this.clientIdProvider = clientIdProvider;
    }

    public void setClientSecretVerifier(ClientSecretVerifier clientSecretVerifier) {
        this.clientSecretVerifier = clientSecretVerifier;
    }
}
