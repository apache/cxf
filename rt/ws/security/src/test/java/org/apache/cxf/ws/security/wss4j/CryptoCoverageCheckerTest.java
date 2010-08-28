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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.w3c.dom.Document;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageChecker.XPathExpression;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.junit.Test;



public class CryptoCoverageCheckerTest extends AbstractSecurityTest {
    
    @Test
    public void testOrder() throws Exception {
        //make sure the interceptors get ordered correctly
        SortedSet<Phase> phases = new TreeSet<Phase>();
        phases.add(new Phase(Phase.PRE_PROTOCOL, 1));
        
        List<Interceptor> lst = 
            new ArrayList<Interceptor>();
        lst.add(new MustUnderstandInterceptor());
        lst.add(new WSS4JInInterceptor());
        lst.add(new SAAJInInterceptor());
        lst.add(new CryptoCoverageChecker());
        PhaseInterceptorChain chain = new PhaseInterceptorChain(phases);
        chain.add(lst);
        String output = chain.toString();
        assertTrue(output.contains("MustUnderstandInterceptor, SAAJInInterceptor, "
                + "WSS4JInInterceptor, CryptoCoverageChecker"));
    }
    
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
        
        // This is mostly testing that things work with no expressions.
        this.runInterceptorAndValidate(
                "signed_x509_issuer_serial_missing_signed_header.xml",
                null,
                null,
                true);
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
    
    @Test
    public void testEncryptedWithIncompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "encrypted_missing_enc_header.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//ser:Header", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
                false);
        
        this.runInterceptorAndValidate(
                "encrypted_body_content.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//soap:Body", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
                false);
        
        this.runInterceptorAndValidate(
                "encrypted_body_element.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//soap:Body", CoverageType.ENCRYPTED, CoverageScope.CONTENT)),
                false);
    }
    
    @Test
    public void testEncryptedWithCompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "encrypted_body_content.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//ser:Header", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
                true);
        
        this.runInterceptorAndValidate(
                "encrypted_body_element.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//soap:Body", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
                true);
        
        this.runInterceptorAndValidate(
                "encrypted_body_content.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//soap:Body", CoverageType.ENCRYPTED, CoverageScope.CONTENT)),
                true);
    }
    
    @Test
    public void testEncryptedSignedWithIncompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "encrypted_body_content_signed_missing_signed_header.xml",
                this.getPrefixes(),
                Arrays.asList(new XPathExpression(
                        "//ser:Header", CoverageType.SIGNED, CoverageScope.ELEMENT)),
                false);
    }
    
    @Test
    public void testEncryptedSignedWithCompleteCoverage() throws Exception {
        this.runInterceptorAndValidate(
                "encrypted_body_content_signed.xml",
                this.getPrefixes(),
                Arrays.asList(
                        new XPathExpression(
                                "//ser:Header", CoverageType.SIGNED, CoverageScope.ELEMENT),
                        new XPathExpression(
                                "//ser:Header", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
                true);
        
        this.runInterceptorAndValidate(
               "wss-242.xml",
               this.getPrefixes(),
               Arrays.asList(
                       new XPathExpression(
                               "//ser:Header", CoverageType.SIGNED, CoverageScope.ELEMENT),
                       new XPathExpression(
                               "//ser:Header", CoverageType.ENCRYPTED, CoverageScope.ELEMENT)),
               true);
    }
    
    private Map<String, String> getPrefixes() {
        final Map<String, String> prefixes = new HashMap<String, String>();
        prefixes.put("ser", "http://www.sdj.pl");
        prefixes.put("soap", "http://schemas.xmlsoap.org/soap/envelope/");
        
        return prefixes;
    }
    
    private void runInterceptorAndValidate(
            String document,
            Map<String, String> prefixes, 
            List<XPathExpression> xpaths,
            boolean pass) throws Exception {
        
        final Document doc = this.readDocument(document);
        final SoapMessage msg = this.getSoapMessageForDom(doc);
        final CryptoCoverageChecker checker = new CryptoCoverageChecker(prefixes, xpaths);
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
        final String action = WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.ENCRYPT;
        
        inHandler.setProperty(WSHandlerConstants.ACTION, action);
        inHandler.setProperty(WSHandlerConstants.SIG_PROP_FILE, 
                "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.DEC_PROP_FILE,
                "META-INF/cxf/insecurity.properties");
        inHandler.setProperty(WSHandlerConstants.PW_CALLBACK_CLASS, 
                TestPwdCallback.class.getName());
        
        return inHandler;
    }
}
