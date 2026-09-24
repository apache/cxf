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
package org.apache.cxf.ws.security.tokenstore;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.saml.SAMLCallback;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.common.saml.bean.SubjectBean;
import org.apache.wss4j.common.saml.bean.Version;
import org.apache.wss4j.common.saml.builder.SAML2Constants;
import org.apache.wss4j.dom.message.token.UsernameToken;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TokenStoreUtilsTest {

    @org.junit.Test
    public void testUnsignedAssertionHasNoCacheKey() throws Exception {
        SamlAssertionWrapper assertion = createAssertion("alice", false);
        assertNull(TokenStoreUtils.getCacheKey(assertion));
    }

    @org.junit.Test
    public void testSignedAssertionCacheKey() throws Exception {
        SamlAssertionWrapper assertion = createAssertion("alice", true);
        String cacheKey = TokenStoreUtils.getCacheKey(assertion);
        assertNotNull(cacheKey);
        // SHA-256 in hex
        assertTrue(cacheKey.matches("[0-9a-f]{64}"));

        // The key must be the same for the Assertion once it has been serialized + re-parsed
        String serialized = StaxUtils.toString(assertion.getElement());
        Document doc = StaxUtils.read(new StringReader(serialized));
        SamlAssertionWrapper parsedAssertion = new SamlAssertionWrapper(doc.getDocumentElement());
        assertEquals(cacheKey, TokenStoreUtils.getCacheKey(parsedAssertion));
    }

    @org.junit.Test
    public void testIssuedAssertionCacheKey() throws Exception {
        // An Assertion that was just signed is not attached to a Document, and so the (non-validating) key
        // is used to store it. It must match the key used to look up the Assertion when it is received.
        SamlAssertionWrapper assertion = createAssertion("alice", true, false);
        String issuedKey = TokenStoreUtils.getCacheKey(assertion, false);
        assertNotNull(issuedKey);

        String serialized = StaxUtils.toString(assertion.getElement());
        Document doc = StaxUtils.read(new StringReader(serialized));
        SamlAssertionWrapper receivedAssertion = new SamlAssertionWrapper(doc.getDocumentElement());
        assertEquals(issuedKey, TokenStoreUtils.getCacheKey(receivedAssertion));
    }

    @org.junit.Test
    public void testDifferentAssertionsHaveDifferentCacheKeys() throws Exception {
        String aliceKey = TokenStoreUtils.getCacheKey(createAssertion("alice", true));
        String bobKey = TokenStoreUtils.getCacheKey(createAssertion("bob", true));
        assertNotEquals(aliceKey, bobKey);
    }

    @org.junit.Test
    public void testModifiedSignedInfoChangesCacheKey() throws Exception {
        SamlAssertionWrapper assertion = createAssertion("alice", true);
        String cacheKey = TokenStoreUtils.getCacheKey(assertion);

        // Change the DigestValue of the SignedInfo, keeping the same SignatureValue
        Element digestValue = (Element)assertion.getElement()
            .getElementsByTagNameNS(WSS4JConstants.SIG_NS, "DigestValue").item(0);
        digestValue.setTextContent("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        SamlAssertionWrapper modifiedAssertion = new SamlAssertionWrapper(assertion.getElement());

        assertNotEquals(cacheKey, TokenStoreUtils.getCacheKey(modifiedAssertion));
    }

    @org.junit.Test
    public void testSignatureNotConformingToProfileHasNoCacheKey() throws Exception {
        SamlAssertionWrapper assertion = createAssertion("alice", true);
        assertNotNull(TokenStoreUtils.getCacheKey(assertion));

        // The signature Reference no longer points to the Assertion itself
        Element reference = (Element)assertion.getElement()
            .getElementsByTagNameNS(WSS4JConstants.SIG_NS, "Reference").item(0);
        reference.setAttributeNS(null, "URI", "#some-other-id");
        SamlAssertionWrapper modifiedAssertion = new SamlAssertionWrapper(assertion.getElement());

        assertNull(TokenStoreUtils.getCacheKey(modifiedAssertion));
    }

    @org.junit.Test
    public void testUsernameTokenCacheKey() throws Exception {
        String key1 = TokenStoreUtils.getCacheKey(createUsernameToken("alice", "password"));
        String key2 = TokenStoreUtils.getCacheKey(createUsernameToken("alice", "password"));
        assertEquals(key1, key2);
        assertTrue(key1.matches("[0-9a-f]{64}"));

        assertNotEquals(key1, TokenStoreUtils.getCacheKey(createUsernameToken("bob", "password")));
        assertNotEquals(key1, TokenStoreUtils.getCacheKey(createUsernameToken("alice", "password2")));
        // Values must not be able to "shift" between fields
        assertNotEquals(TokenStoreUtils.getCacheKey(createUsernameToken("ab", "c")),
                        TokenStoreUtils.getCacheKey(createUsernameToken("a", "bc")));
    }

    private static UsernameToken createUsernameToken(String name, String password) {
        Document doc = DOMUtils.createDocument();
        UsernameToken usernameToken = new UsernameToken(true, doc, WSS4JConstants.PASSWORD_TEXT);
        usernameToken.setName(name);
        usernameToken.setPassword(password);
        return usernameToken;
    }

    private static SamlAssertionWrapper createAssertion(String subjectName, boolean signed) throws Exception {
        return createAssertion(subjectName, signed, true);
    }

    private static SamlAssertionWrapper createAssertion(String subjectName, boolean signed, boolean attach)
        throws Exception {
        SAMLCallback callback = new SAMLCallback();
        callback.setSamlVersion(Version.SAML_20);
        callback.setIssuer("sts");
        callback.setSubject(new SubjectBean(subjectName, null, SAML2Constants.CONF_BEARER));

        SamlAssertionWrapper assertion = new SamlAssertionWrapper(callback);
        if (signed) {
            Crypto crypto = CryptoFactory.getInstance("alice.properties");
            assertion.signAssertion("alice", "password", crypto, false);
        }
        Document doc = DOMUtils.createDocument();
        Element element = assertion.toDOM(doc);
        if (attach) {
            // As for a received token, the signature profile validation requires the Element to be attached
            doc.appendChild(element);
        }
        return assertion;
    }
}
