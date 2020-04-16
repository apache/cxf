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

package org.apache.cxf.ws.security.wss4j.policyvalidators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.policy.AssertionInfo;
import org.apache.cxf.ws.policy.AssertionInfoMap;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.policy.PolicyUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.tokenstore.TokenStoreUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.util.KeyUtils;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.policy.SP11Constants;
import org.apache.wss4j.policy.SP12Constants;
import org.apache.wss4j.policy.SPConstants;
import org.apache.wss4j.policy.model.KerberosToken;
import org.apache.wss4j.policy.model.KerberosToken.ApReqTokenType;
import org.apache.xml.security.utils.XMLUtils;

/**
 * Validate a WSSecurityEngineResult corresponding to the processing of a Kerberos Token
 * against the appropriate policy.
 */
public class KerberosTokenPolicyValidator extends AbstractSecurityPolicyValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(KerberosTokenPolicyValidator.class);

    /**
     * Return true if this SecurityPolicyValidator implementation is capable of validating a
     * policy defined by the AssertionInfo parameter
     */
    public boolean canValidatePolicy(AssertionInfo assertionInfo) {
        return assertionInfo.getAssertion() != null
            && (SP12Constants.KERBEROS_TOKEN.equals(assertionInfo.getAssertion().getName())
                || SP11Constants.KERBEROS_TOKEN.equals(assertionInfo.getAssertion().getName()));
    }

    /**
     * Validate policies.
     */
    public void validatePolicies(PolicyValidatorParameters parameters, Collection<AssertionInfo> ais) {
        List<WSSecurityEngineResult> kerberosResults =
            findKerberosResults(parameters.getResults().getActionResults().get(WSConstants.BST));

        for (WSSecurityEngineResult kerberosResult : kerberosResults) {
            KerberosSecurity kerberosToken =
                (KerberosSecurity)kerberosResult.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);

            boolean asserted = true;
            for (AssertionInfo ai : ais) {
                KerberosToken kerberosTokenPolicy = (KerberosToken)ai.getAssertion();
                ai.setAsserted(true);
                assertToken(kerberosTokenPolicy, parameters.getAssertionInfoMap());

                if (!isTokenRequired(kerberosTokenPolicy, parameters.getMessage())) {
                    PolicyUtils.assertPolicy(
                        parameters.getAssertionInfoMap(),
                        new QName(kerberosTokenPolicy.getVersion().getNamespace(),
                                  "WssKerberosV5ApReqToken11")
                    );
                    PolicyUtils.assertPolicy(
                        parameters.getAssertionInfoMap(),
                        new QName(kerberosTokenPolicy.getVersion().getNamespace(),
                                  "WssGssKerberosV5ApReqToken11")
                    );
                    continue;
                }

                if (!checkToken(parameters.getAssertionInfoMap(), kerberosTokenPolicy, kerberosToken)) {
                    asserted = false;
                    ai.setNotAsserted("An incorrect Kerberos Token Type is detected");
                    continue;
                }
            }

            if (asserted) {
                SecurityToken token = createSecurityToken(kerberosToken);
                token.setSecret((byte[])kerberosResult.get(WSSecurityEngineResult.TAG_SECRET));
                try {
                    TokenStoreUtils.getTokenStore(parameters.getMessage()).add(token);
                } catch (TokenStoreException ex) {
                    LOG.warning(ex.getMessage());
                }
                parameters.getMessage().getExchange().put(SecurityConstants.TOKEN_ID, token.getId());
                return;
            }
        }
    }

    private void assertToken(KerberosToken token, AssertionInfoMap aim) {
        String namespace = token.getName().getNamespaceURI();

        if (token.isRequireKeyIdentifierReference()) {
            PolicyUtils.assertPolicy(aim, new QName(namespace, SPConstants.REQUIRE_KEY_IDENTIFIER_REFERENCE));
        }
    }

    private boolean checkToken(
        AssertionInfoMap aim,
        KerberosToken kerberosTokenPolicy,
        KerberosSecurity kerberosToken
    ) {
        ApReqTokenType apReqTokenType = kerberosTokenPolicy.getApReqTokenType();

        if (apReqTokenType == ApReqTokenType.WssKerberosV5ApReqToken11
            && kerberosToken.isV5ApReq()) {
            PolicyUtils.assertPolicy(
                aim,
                new QName(kerberosTokenPolicy.getVersion().getNamespace(), "WssKerberosV5ApReqToken11")
            );
            return true;
        } else if (apReqTokenType == ApReqTokenType.WssGssKerberosV5ApReqToken11
            && kerberosToken.isGssV5ApReq()) {
            PolicyUtils.assertPolicy(
                aim,
                new QName(kerberosTokenPolicy.getVersion().getNamespace(), "WssGssKerberosV5ApReqToken11")
            );
            return true;
        }

        return false;
    }

    private List<WSSecurityEngineResult> findKerberosResults(List<WSSecurityEngineResult> bstResults) {
        List<WSSecurityEngineResult> results = new ArrayList<>();
        if (bstResults != null) {
            for (WSSecurityEngineResult wser : bstResults) {
                BinarySecurity binarySecurity =
                    (BinarySecurity)wser.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                if (binarySecurity instanceof KerberosSecurity) {
                    results.add(wser);
                }
            }
        }
        return results;
    }

    private SecurityToken createSecurityToken(KerberosSecurity binarySecurityToken) {
        SecurityToken token = new SecurityToken(binarySecurityToken.getID());
        token.setToken(binarySecurityToken.getElement());
        token.setTokenType(binarySecurityToken.getValueType());
        byte[] tokenBytes = binarySecurityToken.getToken();
        try {
            token.setSHA1(XMLUtils.encodeToString(KeyUtils.generateDigest(tokenBytes)));
        } catch (WSSecurityException e) {
            // Just consume this for now as it isn't critical...
        }
        return token;
    }
}
