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
package org.apache.cxf.sts.token.delegation;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.builder.SAML1Constants;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.opensaml.saml.saml1.core.AudienceRestrictionCondition;

/**
 * The SAML TokenDelegationHandler implementation. It disallows ActAs or OnBehalfOf for
 * all cases apart from the case of a Bearer SAML Token. In addition, the AppliesTo
 * address (if supplied) must match an AudienceRestriction address (if in token), if the
 * "checkAudienceRestriction" property is set to "true".
 */
public class SAMLDelegationHandler implements TokenDelegationHandler {

    private static final Logger LOG =
        LogUtils.getL7dLogger(SAMLDelegationHandler.class);

    private boolean checkAudienceRestriction;

    public boolean canHandleToken(ReceivedToken delegateTarget) {
        Object token = delegateTarget.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((WSS4JConstants.SAML_NS.equals(namespace) || WSS4JConstants.SAML2_NS.equals(namespace))
                && "Assertion".equals(localname)) {
                return true;
            }
        }
        return false;
    }

    public TokenDelegationResponse isDelegationAllowed(TokenDelegationParameters tokenParameters) {
        TokenDelegationResponse response = new TokenDelegationResponse();
        ReceivedToken delegateTarget = tokenParameters.getToken();
        response.setToken(delegateTarget);

        if (delegateTarget.getState() != STATE.VALID || !delegateTarget.isDOMElement()) {
            LOG.fine("Delegation token is not valid");
            return response;
        }

        if (isDelegationAllowed(delegateTarget, tokenParameters.getAppliesToAddress())) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Delegation is allowed for principal " + tokenParameters.getPrincipal());
            }
            response.setDelegationAllowed(true);
        } else if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Delegation is not allowed for principal " + tokenParameters.getPrincipal());
        }

        return response;
    }

    /**
     * Is Delegation allowed for a particular token
     */
    protected boolean isDelegationAllowed(
        ReceivedToken receivedToken, String appliesToAddress
    ) {
        Element validateTargetElement = (Element)receivedToken.getToken();
        try {
            SamlAssertionWrapper assertion = new SamlAssertionWrapper(validateTargetElement);

            for (String confirmationMethod : assertion.getConfirmationMethods()) {
                if (!(SAML1Constants.CONF_BEARER.equals(confirmationMethod)
                    || SAML2Constants.CONF_BEARER.equals(confirmationMethod))) {
                    LOG.fine("An unsupported Confirmation Method was used: " + confirmationMethod);
                    return false;
                }
            }

            if (checkAudienceRestriction && appliesToAddress != null) {
                List<String> addresses = getAudienceRestrictions(assertion);
                if (!(addresses.isEmpty() || addresses.contains(appliesToAddress))) {
                    LOG.fine("The AppliesTo address " + appliesToAddress + " is not contained"
                             + " in the Audience Restriction addresses in the assertion");
                    return false;
                }
            }
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "Error in ascertaining whether delegation is allowed", ex);
            return false;
        }

        return true;
    }

    protected List<String> getAudienceRestrictions(SamlAssertionWrapper assertion) {
        List<String> addresses = new ArrayList<>();
        if (assertion.getSaml1() != null) {
            for (AudienceRestrictionCondition restriction
                : assertion.getSaml1().getConditions().getAudienceRestrictionConditions()) {
                for (org.opensaml.saml.saml1.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getURI());
                }
            }
        } else if (assertion.getSaml2() != null) {
            for (org.opensaml.saml.saml2.core.AudienceRestriction restriction
                : assertion.getSaml2().getConditions().getAudienceRestrictions()) {
                for (org.opensaml.saml.saml2.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getURI());
                }
            }
        }

        return addresses;
    }

    public boolean isCheckAudienceRestriction() {
        return checkAudienceRestriction;
    }

    /**
     * Set whether to perform a check that the received AppliesTo address is contained in the
     * token as one of the AudienceRestriction URIs. The default is false.
     * @param checkAudienceRestriction whether to perform an audience restriction check or not
     */
    public void setCheckAudienceRestriction(boolean checkAudienceRestriction) {
        this.checkAudienceRestriction = checkAudienceRestriction;
    }
}
