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

package org.apache.cxf.sts.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.transform.Source;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.claims.ClaimsManager;
import org.apache.cxf.sts.event.STSEventListener;
import org.apache.cxf.sts.operation.AbstractOperation;
import org.apache.cxf.sts.operation.TokenIssueOperation;
import org.apache.cxf.sts.operation.TokenRenewOperation;
import org.apache.cxf.sts.operation.TokenValidateOperation;
import org.apache.cxf.sts.service.ServiceMBean;
import org.apache.cxf.sts.token.provider.SAMLTokenProvider;
import org.apache.cxf.sts.token.provider.TokenProvider;
import org.apache.cxf.sts.token.renewer.SAMLTokenRenewer;
import org.apache.cxf.sts.token.renewer.TokenRenewer;
import org.apache.cxf.sts.token.validator.SAMLTokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.UsernameTokenValidator;
import org.apache.cxf.sts.token.validator.X509TokenValidator;
import org.apache.cxf.ws.security.sts.provider.SecurityTokenServiceProvider;
import org.apache.cxf.ws.security.tokenstore.TokenStore;

/**
 * A "default" SecurityTokenServiceProvider implementation that defines the Issue and Validate
 * Operations of the STS and adds support for issuing and validating SAML Assertions, and
 * validating UsernameTokens and X.509 Tokens. It also defines the Renew Operation for SAML
 * tokens.
 */
public class DefaultSecurityTokenServiceProvider extends SecurityTokenServiceProvider {

    private static final Logger LOG = LogUtils.getL7dLogger(DefaultSecurityTokenServiceProvider.class);

    private STSPropertiesMBean stsProperties;
    private boolean encryptIssuedToken;
    private List<ServiceMBean> services;
    private boolean returnReferences = true;
    private TokenStore tokenStore;
    private ClaimsManager claimsManager = new ClaimsManager();
    private STSEventListener eventListener;

    public DefaultSecurityTokenServiceProvider() throws Exception {
        super();
    }

    public void setReturnReferences(boolean returnReferences) {
        this.returnReferences = returnReferences;
    }

    public void setTokenStore(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public void setStsProperties(STSPropertiesMBean stsProperties) {
        this.stsProperties = stsProperties;
    }

    public void setEncryptIssuedToken(boolean encryptIssuedToken) {
        this.encryptIssuedToken = encryptIssuedToken;
    }

    public void setServices(List<ServiceMBean> services) {
        this.services = services;
    }

    public void setClaimsManager(ClaimsManager claimsManager) {
        this.claimsManager = claimsManager;
    }

    public void setEventListener(STSEventListener listener) {
        this.eventListener = listener;
    }

    @Override
    public Source invoke(Source request) {
        if (getIssueOperation() == null) {
            setIssueOperation(createTokenIssueOperation());
        }
        if (getValidateOperation() == null) {
            setValidateOperation(createTokenValidateOperation());
        }
        if (getRenewOperation() == null) {
            setRenewOperation(createTokenRenewOperation());
        }
        return super.invoke(request);
    }

    private TokenIssueOperation createTokenIssueOperation() {
        TokenIssueOperation issueOperation = new TokenIssueOperation();
        populateAbstractOperation(issueOperation);

        return issueOperation;
    }

    private TokenValidateOperation createTokenValidateOperation() {
        TokenValidateOperation validateOperation = new TokenValidateOperation();
        populateAbstractOperation(validateOperation);

        return validateOperation;
    }

    private TokenRenewOperation createTokenRenewOperation() {
        TokenRenewOperation renewOperation = new TokenRenewOperation();
        populateAbstractOperation(renewOperation);

        List<TokenRenewer> tokenRenewers = new ArrayList<>();
        tokenRenewers.add(new SAMLTokenRenewer());
        renewOperation.setTokenRenewers(tokenRenewers);

        return renewOperation;
    }

    private void populateAbstractOperation(AbstractOperation abstractOperation) {
        if (stsProperties == null) {
            LOG.warning("No 'stsProperties' configured on the DefaultSecurityTokenServiceProvider");
            return;
        }

        List<TokenProvider> tokenProviders = new ArrayList<>();
        tokenProviders.add(new SAMLTokenProvider());

        List<TokenValidator> tokenValidators = new ArrayList<>();
        tokenValidators.add(new SAMLTokenValidator());
        tokenValidators.add(new UsernameTokenValidator());
        tokenValidators.add(new X509TokenValidator());

        abstractOperation.setTokenProviders(tokenProviders);
        abstractOperation.setTokenValidators(tokenValidators);
        abstractOperation.setStsProperties(stsProperties);
        abstractOperation.setEncryptIssuedToken(encryptIssuedToken);
        abstractOperation.setServices(services);
        abstractOperation.setReturnReferences(returnReferences);
        abstractOperation.setTokenStore(tokenStore);
        abstractOperation.setClaimsManager(claimsManager);
        abstractOperation.setEventListener(eventListener);
    }
}