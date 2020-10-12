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


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageChecker.XPathExpression;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.wss4j.common.ConfigurationConstants;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test the DefaultCryptoCoverageChecker, which extends the CryptoCoverageChecker to provide
 * an easier way to check to see if the SOAP (1.1 + 1.2) Body was signed and/or encrypted, if
 * the Timestamp was signed, and if the WS-Addressing ReplyTo and FaultTo headers were signed,
 * and if a UsernameToken was encrypted.
 */
public class DefaultCryptoCoverageCheckerTest extends AbstractSecurityTest {

    @Test
    public void testSignedWithIncompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//ser:Header", CoverageType.SIGNED, CoverageScope.ELEMENT)),
                false);

        // This is mostly testing that things work with no prefixes.
        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                null,
                Arrays.asList(new XPathExpression(
                        "//*", CoverageType.SIGNED, CoverageScope.ELEMENT)),
                false);

        // This fails as the SOAP Body is not signed
        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                null,
                null,
                false);
    }

    @Test
    public void testSignedWithCompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                null,
                null,
                true);

        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//ser:Header", CoverageType.SIGNED, CoverageScope.ELEMENT)),
                true);
    }

    private Map<String, String> getPrefixes() {
        final Map<String, String> prefixes = new HashMap<>();
        prefixes.put("ser", "http://www.sdj.pl");

        return prefixes;
    }

    private void runInterceptorAndValidate(
            String document,
            Map<String, String> prefixes,
            List<XPathExpression> xpaths,
            boolean pass) throws Exception {

        final Document doc = this.readDocument(document);
        final SoapMessage msg = this.getSoapMessageForDom(doc);
        final CryptoCoverageChecker checker = new DefaultCryptoCoverageChecker();
        checker.addPrefixes(prefixes);
        checker.addXPaths(xpaths);
        final PhaseInterceptor<SoapMessage> wss4jInInterceptor = this.getWss4jInInterceptor();

        wss4jInInterceptor.handleMessage(msg);

        try {
            checker.handleMessage(msg);
            if (!pass) {
                fail("Passed interceptor erroneously.");
            }
        } catch (Fault e) {
            if (pass) {
                fail("Failed interceptor erroneously.");
            }

            assertTrue(e.getMessage().contains("element found matching XPath"));
        }
    }

    private PhaseInterceptor<SoapMessage> getWss4jInInterceptor() {
        final WSS4JInInterceptor inHandler = new WSS4JInInterceptor(true);
        final String action = ConfigurationConstants.SIGNATURE + " " + ConfigurationConstants.ENCRYPTION;

        inHandler.setProperty(ConfigurationConstants.ACTION, action);
        inHandler.setProperty(ConfigurationConstants.SIG_VER_PROP_FILE,
                "insecurity.properties");
        inHandler.setProperty(ConfigurationConstants.DEC_PROP_FILE,
                "insecurity.properties");
        inHandler.setProperty(ConfigurationConstants.PW_CALLBACK_CLASS,
                TestPwdCallback.class.getName());
        inHandler.setProperty(ConfigurationConstants.IS_BSP_COMPLIANT, "false");

        return inHandler;
    }
}
