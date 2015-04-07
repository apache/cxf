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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.wss4j.common.crypto.AlgorithmSuite;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.AbstractBinding;
import org.apache.wss4j.policy.model.AbstractSecurityAssertion;
import org.apache.wss4j.policy.model.AlgorithmSuite.AlgorithmSuiteType;
import org.apache.wss4j.policy.model.SamlToken;
import org.apache.wss4j.policy.model.SupportingTokens;

/**
 * Translate any AlgorithmSuite policy that may be operative into a WSS4J AlgorithmSuite object
 * to enforce what algorithms are allowed in a request.
 */
public final class AlgorithmSuiteTranslater {
    
    public void translateAlgorithmSuites(AssertionInfoMap aim, RequestData data) throws WSSecurityException {
        if (aim == null) {
            return;
        }
        
        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites = 
            getAlgorithmSuites(getBindings(aim));
        if (!algorithmSuites.isEmpty()) {
            // Translate into WSS4J's AlgorithmSuite class
            AlgorithmSuite algorithmSuite = translateAlgorithmSuites(algorithmSuites);
            data.setAlgorithmSuite(algorithmSuite);
        }

        // Now look for an AlgorithmSuite for a SAML Assertion
        Collection<AssertionInfo> ais = getAllAssertionsByLocalname(aim, SPConstants.SAML_TOKEN);
        if (!ais.isEmpty()) {
            List<org.apache.wss4j.policy.model.AlgorithmSuite> samlAlgorithmSuites
                = new ArrayList<org.apache.wss4j.policy.model.AlgorithmSuite>();
            for (AssertionInfo ai : ais) {
                SamlToken samlToken = (SamlToken)ai.getAssertion();
                AbstractSecurityAssertion parentAssertion = samlToken.getParentAssertion();
                if ((parentAssertion instanceof SupportingTokens)
                    && ((SupportingTokens)parentAssertion).getAlgorithmSuite() != null) {
                    samlAlgorithmSuites.add(((SupportingTokens)parentAssertion).getAlgorithmSuite());
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
        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites
    ) {
        AlgorithmSuite algorithmSuite = null;
        
        for (org.apache.wss4j.policy.model.AlgorithmSuite cxfAlgorithmSuite 
            : algorithmSuites) {
            if (cxfAlgorithmSuite == null) {
                continue;
            }
            
            // Translate into WSS4J's AlgorithmSuite class
            if (algorithmSuite == null) {
                algorithmSuite = new AlgorithmSuite();
            }
            
            AlgorithmSuiteType algorithmSuiteType = cxfAlgorithmSuite.getAlgorithmSuiteType();
            // Set asymmetric key lengths
            if (algorithmSuite.getMaximumAsymmetricKeyLength() 
                < algorithmSuiteType.getMaximumAsymmetricKeyLength()) {
                algorithmSuite.setMaximumAsymmetricKeyLength(
                    algorithmSuiteType.getMaximumAsymmetricKeyLength());
            }
            if (algorithmSuite.getMinimumAsymmetricKeyLength() 
                > algorithmSuiteType.getMinimumAsymmetricKeyLength()) {
                algorithmSuite.setMinimumAsymmetricKeyLength(
                    algorithmSuiteType.getMinimumAsymmetricKeyLength());
            }
            
            // Set symmetric key lengths
            if (algorithmSuite.getMaximumSymmetricKeyLength() 
                < algorithmSuiteType.getMaximumSymmetricKeyLength()) {
                algorithmSuite.setMaximumSymmetricKeyLength(
                    algorithmSuiteType.getMaximumSymmetricKeyLength());
            }
            if (algorithmSuite.getMinimumSymmetricKeyLength() 
                > algorithmSuiteType.getMinimumSymmetricKeyLength()) {
                algorithmSuite.setMinimumSymmetricKeyLength(
                    algorithmSuiteType.getMinimumSymmetricKeyLength());
            }
                
            algorithmSuite.addEncryptionMethod(algorithmSuiteType.getEncryption());
            algorithmSuite.addKeyWrapAlgorithm(algorithmSuiteType.getSymmetricKeyWrap());
            algorithmSuite.addKeyWrapAlgorithm(algorithmSuiteType.getAsymmetricKeyWrap());
    
            algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getAsymmetricSignature());
            algorithmSuite.addSignatureMethod(cxfAlgorithmSuite.getSymmetricSignature());
            algorithmSuite.addDigestAlgorithm(algorithmSuiteType.getDigest());
            algorithmSuite.addC14nAlgorithm(cxfAlgorithmSuite.getC14n().getValue());
    
            algorithmSuite.addTransformAlgorithm(cxfAlgorithmSuite.getC14n().getValue());
            algorithmSuite.addTransformAlgorithm(SPConstants.STRT10);
            algorithmSuite.addTransformAlgorithm(WSConstants.C14N_EXCL_OMIT_COMMENTS);
            algorithmSuite.addTransformAlgorithm(WSConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE);
            algorithmSuite.addTransformAlgorithm(WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS);
            algorithmSuite.addTransformAlgorithm(WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS);
    
            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1);
            algorithmSuite.addDerivedKeyAlgorithm(SPConstants.P_SHA1_L128);
        }

        return algorithmSuite;
    }

    /**
     * Get all of the WS-SecurityPolicy Bindings that are in operation
     */
    private List<AbstractBinding> getBindings(AssertionInfoMap aim) {
        List<AbstractBinding> bindings = new ArrayList<AbstractBinding>();
        if (aim != null) {
            Collection<AssertionInfo> ais = 
                getAllAssertionsByLocalname(aim, SPConstants.TRANSPORT_BINDING);
            if (!ais.isEmpty()) {
                for (AssertionInfo ai : ais) {
                    bindings.add((AbstractBinding)ai.getAssertion());
                }
            }
            ais = getAllAssertionsByLocalname(aim, SPConstants.ASYMMETRIC_BINDING);
            if (!ais.isEmpty()) {     
                for (AssertionInfo ai : ais) {
                    bindings.add((AbstractBinding)ai.getAssertion());
                }
            }
            ais = getAllAssertionsByLocalname(aim, SPConstants.SYMMETRIC_BINDING);
            if (!ais.isEmpty()) {     
                for (AssertionInfo ai : ais) {
                    bindings.add((AbstractBinding)ai.getAssertion());
                }
            }
        }
        return bindings;
    }
    
    /**
     * Get all of the CXF AlgorithmSuites from the bindings
     */
    private List<org.apache.wss4j.policy.model.AlgorithmSuite> getAlgorithmSuites(
        List<AbstractBinding> bindings
    ) {
        List<org.apache.wss4j.policy.model.AlgorithmSuite> algorithmSuites = 
            new ArrayList<org.apache.wss4j.policy.model.AlgorithmSuite>();
        for (AbstractBinding binding : bindings) {
            if (binding.getAlgorithmSuite() != null) {
                algorithmSuites.add(binding.getAlgorithmSuite());
            }
        }
        return algorithmSuites;
    }
    
    private Collection<AssertionInfo> getAllAssertionsByLocalname(
        AssertionInfoMap aim,
        String localname
    ) {
        Collection<AssertionInfo> sp11Ais = aim.get(new QName(SP11Constants.SP_NS, localname));
        Collection<AssertionInfo> sp12Ais = aim.get(new QName(SP12Constants.SP_NS, localname));
        
        if ((sp11Ais != null && !sp11Ais.isEmpty()) || (sp12Ais != null && !sp12Ais.isEmpty())) {
            Collection<AssertionInfo> ais = new HashSet<AssertionInfo>();
            if (sp11Ais != null) {
                ais.addAll(sp11Ais);
            }
            if (sp12Ais != null) {
                ais.addAll(sp12Ais);
            }
            return ais;
        }
            
        return Collections.emptySet();
    }

}
