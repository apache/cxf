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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.EncryptionActionToken;
import org.apache.wss4j.common.SignatureActionToken;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;
import org.apache.wss4j.dom.engine.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.HandlerAction;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

/**
 * Some tests for configuring outbound security using SecurityActionTokens.
 */
public class SecurityActionTokenTest extends AbstractSecurityTest {

    @Test
    public void testSignature() throws Exception {
        SignatureActionToken actionToken = new SignatureActionToken();
        actionToken.setCryptoProperties("outsecurity.properties");
        actionToken.setUser("myalias");
        List<HandlerAction> actions =
            Collections.singletonList(new HandlerAction(WSConstants.SIGN, actionToken));

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(WSHandlerConstants.HANDLER_ACTIONS, actions);
        outProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.SIGNATURE);
        inProperties.put(ConfigurationConstants.SIG_VER_PROP_FILE, "insecurity.properties");

        List<String> xpaths = new ArrayList<>();
        xpaths.add("//wsse:Security");
        xpaths.add("//wsse:Security/ds:Signature");

        List<WSHandlerResult> handlerResults =
            getResults(makeInvocation(outProperties, xpaths, inProperties));
        WSSecurityEngineResult actionResult =
            handlerResults.get(0).getActionResults().get(WSConstants.SIGN).get(0);

        X509Certificate certificate =
            (X509Certificate) actionResult.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
        assertNotNull(certificate);
    }

    @Test
    public void testEncryption() throws Exception {
        EncryptionActionToken actionToken = new EncryptionActionToken();
        actionToken.setCryptoProperties("outsecurity.properties");
        actionToken.setUser("myalias");
        List<HandlerAction> actions =
            Collections.singletonList(new HandlerAction(WSConstants.ENCR, actionToken));

        Map<String, Object> outProperties = new HashMap<>();
        outProperties.put(WSHandlerConstants.HANDLER_ACTIONS, actions);

        Map<String, Object> inProperties = new HashMap<>();
        inProperties.put(ConfigurationConstants.ACTION, ConfigurationConstants.ENCRYPTION);
        inProperties.put(ConfigurationConstants.DEC_PROP_FILE, "insecurity.properties");
        inProperties.put(ConfigurationConstants.PW_CALLBACK_REF, new TestPwdCallback());

        List<String> xpaths = new ArrayList<>();
        xpaths.add("//wsse:Security");
        xpaths.add("//s:Body/xenc:EncryptedData");

        List<WSHandlerResult> handlerResults =
            getResults(makeInvocation(outProperties, xpaths, inProperties));

        assertNotNull(handlerResults);
        assertSame(handlerResults.size(), 1);
        //
        // This should contain exactly 1 protection result
        //
        final java.util.List<WSSecurityEngineResult> protectionResults =
            handlerResults.get(0).getResults();
        assertNotNull(protectionResults);
        assertSame(protectionResults.size(), 1);
        //
        // This result should contain a reference to the decrypted element,
        // which should contain the soap:Body Qname
        //
        final java.util.Map<String, Object> result =
            protectionResults.get(0);
        final java.util.List<WSDataRef> protectedElements =
            CastUtils.cast((List<?>)result.get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
        assertNotNull(protectedElements);
        assertSame(protectedElements.size(), 1);
        assertEquals(
            protectedElements.get(0).getName(),
            new javax.xml.namespace.QName(
                "http://schemas.xmlsoap.org/soap/envelope/",
                "Body"
            )
        );
    }

    private List<WSHandlerResult> getResults(SoapMessage inmsg) {
        return CastUtils.cast((List<?>)inmsg.get(WSHandlerConstants.RECV_RESULTS));
    }

    // FOR DEBUGGING ONLY
    /*private*/ static String serialize(Document doc) {
        return StaxUtils.toString(doc);
    }
}
