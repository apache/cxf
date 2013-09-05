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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceContext;

import org.w3c.dom.Element;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.apache.ws.security.saml.ext.builder.SAML1Constants;
import org.apache.ws.security.saml.ext.builder.SAML2Constants;
import org.opensaml.saml1.core.AudienceRestrictionCondition;

/**
 * The Default DelegationHandler implementation. It disallows ActAs or OnBehalfOf for
 * all cases apart from the case of a Bearer SAML Token. In addition, the AppliesTo
 * address (if supplied) must match an AudienceRestriction address (if in token)
 */
public class DefaultDelegationHandler implements DelegationHandler {
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(DefaultDelegationHandler.class);
    
    /**
     * Returns true if delegation is allowed.
     * @param context WebServiceContext
     * @param tokenRequirements The parameters extracted from the request
     * @param appliesToAddress The AppliesTo address (if any)
     * @param onBehalfOf whether the token was received OnBehalfOf or ActAs
     * @return true if delegation is allowed.
     */
    public boolean isDelegationAllowed(
        WebServiceContext context,
        TokenRequirements tokenRequirements, 
        String appliesToAddress
    ) {
        if (tokenRequirements.getOnBehalfOf() != null 
            && !isDelegationAllowed(context, tokenRequirements.getOnBehalfOf(), appliesToAddress)) {
            return false;
        }
        
        if (tokenRequirements.getActAs() != null 
            && !isDelegationAllowed(context, tokenRequirements.getActAs(), appliesToAddress)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Is Delegation allowed for a particular token
     */
    protected boolean isDelegationAllowed(
        WebServiceContext context,
        ReceivedToken receivedToken, 
        String appliesToAddress
    ) {
        // It must be a SAML Token
        if (!isSAMLToken(receivedToken)) {
            LOG.fine("Received token is not a SAML Token");
            return false;
        }

        Element validateTargetElement = (Element)receivedToken.getToken();
        try {
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);

            for (String confirmationMethod : assertion.getConfirmationMethods()) {
                if (!(SAML1Constants.CONF_BEARER.equals(confirmationMethod)
                    || SAML2Constants.CONF_BEARER.equals(confirmationMethod))) {
                    LOG.fine("An unsupported Confirmation Method was used: " + confirmationMethod);
                    return false;
                }
            }

            if (appliesToAddress != null) {
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
    
    protected boolean isSAMLToken(ReceivedToken target) {
        Object token = target.getToken();
        if (token instanceof Element) {
            Element tokenElement = (Element)token;
            String namespace = tokenElement.getNamespaceURI();
            String localname = tokenElement.getLocalName();
            if ((WSConstants.SAML_NS.equals(namespace) || WSConstants.SAML2_NS.equals(namespace))
                && "Assertion".equals(localname)) {
                return true;
            }
        }
        return false;
    }
    
    protected List<String> getAudienceRestrictions(AssertionWrapper assertion) {
        List<String> addresses = new ArrayList<String>();
        if (assertion.getSaml1() != null) {
            for (AudienceRestrictionCondition restriction 
                : assertion.getSaml1().getConditions().getAudienceRestrictionConditions()) {
                for (org.opensaml.saml1.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getUri());
                }
            }
        } else if (assertion.getSaml2() != null) {
            for (org.opensaml.saml2.core.AudienceRestriction restriction 
                : assertion.getSaml2().getConditions().getAudienceRestrictions()) {
                for (org.opensaml.saml2.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getAudienceURI());
                }
            }
        }
        
        return addresses;
    }
    
}