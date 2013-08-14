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

package org.apache.cxf.ws.security.wss4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.policy.SP12Constants;
import org.apache.cxf.ws.security.policy.SPConstants;
import org.apache.cxf.ws.security.policy.model.Binding;
import org.apache.cxf.ws.security.policy.model.SamlToken;
import org.apache.cxf.ws.security.policy.model.SupportingToken;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.AlgorithmSuite;
import org.apache.ws.security.handler.RequestData;

/**
 * Translate any AlgorithmSuite policy that may be operative into a WSS4J AlgorithmSuite object
 * to enforce what algorithms are allowed in a request.
 */
public final class AlgorithmSuiteTranslater {
    
    public void translateAlgorithmSuites(AssertionInfoMap aim, RequestData data) throws WSSecurityException {
        if (aim == null) {
            return;
        }
        
        List<org.apache.cxf.ws.security.policy.model.AlgorithmSuite> algorithmSuites = 
            getAlgorithmSuites(getBindings(aim));
        if (!algorithmSuites.isEmpty()) {
            // Translate into WSS4J's AlgorithmSuite class
            AlgorithmSuite algorithmSuite = translateAlgorithmSuites(algorithmSuites);
            data.setAlgorithmSuite(algorithmSuite);
        }

        // Now look for an AlgorithmSuite for a SAML Assertion
        Collection<AssertionInfo> ais = aim.get(SP12Constants.SAML_TOKEN);
        if (ais != null && !ais.isEmpty()) {
            List<org.apache.cxf.ws.security.policy.model.AlgorithmSuite> samlAlgorithmSuites
                = new ArrayList<org.apache.cxf.ws.security.policy.model.AlgorithmSuite>();
            for (AssertionInfo ai : ais) {
                SamlToken samlToken = (SamlToken)ai.getAssertion();
                SupportingToken supportingToken = samlToken.getSupportingToken();
                if (supportingToken != null && supportingToken.getAlgorithmSuite() != null) {
                    samlAlgorithmSuites.add(supportingToken.getAlgorithmSuite());
                }
            }

            if (!samlAlgorithmSuites.isEmpty()) {
                data.setSamlAlgorithmSuite(translateAlgorithmSuites(samlAlgorithmSuites));
            }
        }
    }

    /**
     * Translate a list of CXF AlgorithmSuite objects into a single WSS4J AlgorithmSuite object
     */
    private AlgorithmSuite translateAlgorithmSuites(
        List<org.apache.cxf.ws.security.policy.model.AlgorithmSuite> algorithmSuites
    ) {
        AlgorithmSuite algorithmSuite = null;
        
        for (org.apache.cxf.ws.security.policy.model.AlgorithmSuite cxfAlgorithmSuite 
            : algorithmSuites) {
            if (cxfAlgorithmSuite == null) {
                continue;
            }
            
            // Translate into WSS4J's AlgorithmSuite class
            if (algorithmSuite == null) {
                algorithmSuite = new AlgorithmSuite();
            }
            
            // Set asymmetric key lengths
            if (algorithmSuite.getMaximumAsymmetricKeyLength() 
                < cxfAlgorithmSuite.getMaximumAsymmetricKeyLength()) {
                algorithmSuite.setMaximumAsymmetricKeyLength(
                    cxfAlgorithmSuite.getMaximumAsymmetricKeyLength());
            }
            if (algorithmSuite.getMinimumAsymmetricKeyLength() 
                > cxfAlgorithmSuite.getMinimumAsymmetricKeyLength()) {
                algorithmSuite.setMinimumAsymmetricKeyLength(
                    cxfAlgorithmSuite.getMinimumAsymmetricKeyLength());
            }
            
            // Set symmetric key lengths
            if (algorithmSuite.getMaximumSymmetricKeyLength() 
                < cxfAlgorithmSuite.getMaximumSymmetricKeyLength()) {
                algorithmSuite.setMaximumSymmetricKeyLength(
                    cxfAlgorithmSuite.getMaximumSymmetricKeyLength());
            }
            if (algorithmSuite.getMinimumSymmetricKeyLength() 
                > cxfAlgorithmSuite.getMinimumSymmetricKeyLength()) {
                algorithmSuite.setMinimumSymmetricKeyLength(
                    cxfAlgorithmSuite.getMinimumSymmetricKeyLength());
            }
                
            algorithmSuite.addEncryptionMethod(cxfAlgorithmSuite.getEncryption());
            algorithmSuite.addKeyWrapAlgorithm(cxfAlgorithmSuite.getSymmetricKeyWrap());
            algorithmSuite.addKeyWrapAlgorithm(cxfAlgorithmSuite.getAsymmetricKeyWrap());
    
            algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getAsymmetricSignature());
            algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getSymmetricSignature());
            algorithmSuite.addDigestAlgorithm(cxfAlgorithmSuite.getDigest());
            algorithmSuite.addC14nAlgorithm(cxfAlgorithmSuite.getInclusiveC14n());
    
            algorithmSuite.addTransformAlgorithm(cxfAlgorithmSuite.getInclusiveC14n());
            algorithmSuite.addTransformAlgorithm(SPConstants.STRT10);
            algorithmSuite.addTransformAlgorithm(WSConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE);
    
            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1);
            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1_L128);
        }

        return algorithmSuite;
    }

    /**
     * Get all of the WS-SecurityPolicy Bindings that are in operation
     */
    public static List<Binding> getBindings(AssertionInfoMap aim) {
        List<Binding> bindings = new ArrayList<Binding>();
        if (aim != null) {
            Collection<AssertionInfo> ais = aim.get(SP12Constants.TRANSPORT_BINDING);
            if (ais != null && !ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    bindings.add((Binding)ai.getAssertion());
                }
            }
            ais = aim.get(SP12Constants.ASYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {     
                for (AssertionInfo ai : ais) {
                    bindings.add((Binding)ai.getAssertion());
                }
            }
            ais = aim.get(SP12Constants.SYMMETRIC_BINDING);
            if (ais != null && !ais.isEmpty()) {     
                for (AssertionInfo ai : ais) {
                    bindings.add((Binding)ai.getAssertion());
                }
            }
        }
        return bindings;
    }
    
    /**
     * Get all of the CXF AlgorithmSuites from the bindings
     */
    public static List<org.apache.cxf.ws.security.policy.model.AlgorithmSuite> getAlgorithmSuites(
        List<Binding> bindings
    ) {
        List<org.apache.cxf.ws.security.policy.model.AlgorithmSuite> algorithmSuites = 
            new ArrayList<org.apache.cxf.ws.security.policy.model.AlgorithmSuite>();
        for (Binding binding : bindings) {
            if (binding.getAlgorithmSuite() != null) {
                algorithmSuites.add(binding.getAlgorithmSuite());
            }
        }
        return algorithmSuites;
    }

}
