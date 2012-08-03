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


import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.ws.security.WSConstants;

/**
 * This utility extends the CryptoCoverageChecker to provide an easy way to check to see
 * if the SOAP (1.1 + 1.2) Body was signed and/or encrypted, if the Timestamp was signed,
 * and if the WS-Addressing ReplyTo and FaultTo headers were signed.
 * 
 * The default configuration is that the SOAP Body, Timestamp must be signed, and WS-Addressing
 * ReplyTo and FaultTo headers must be signed (if they exist in the message payload).
 */
public class DefaultCryptoCoverageChecker extends CryptoCoverageChecker {
    
    public static final String SOAP_NS = WSConstants.URI_SOAP11_ENV;
    public static final String SOAP12_NS = WSConstants.URI_SOAP12_ENV;
    public static final String WSU_NS = WSConstants.WSU_NS;
    public static final String WSSE_NS = WSConstants.WSSE_NS;
    public static final String WSA_NS = Names.WSA_NAMESPACE_NAME;
    
    private boolean signBody;
    private boolean signTimestamp;
    private boolean encryptBody;
    private boolean signAddressingHeaders;
    
    /**
     * Creates a new instance. Enforces that the SOAP Body, Timestamp, and WS-Addressing
     * ReplyTo and FaultTo headers must be signed (if they exist in the message payload).
     */
    public DefaultCryptoCoverageChecker() {
        super(null, null);
        
        prefixMap.put("soapenv", SOAP_NS);
        prefixMap.put("soapenv12", SOAP12_NS);
        prefixMap.put("wsu", WSU_NS);
        prefixMap.put("wsse", WSSE_NS);
        prefixMap.put("wsa", WSA_NS);
        
        // Sign SOAP Body
        setSignBody(true);
        
        // Sign Timestamp
        setSignTimestamp(true);
        
        // Sign Addressing Headers
        setSignAddressingHeaders(true);
    }
    
    public boolean isSignBody() {
        return signBody;
    }

    public final void setSignBody(boolean signBody) {
        this.signBody = signBody;
        
        XPathExpression soap11Expression = 
            new XPathExpression("/soapenv:Envelope/soapenv:Body", CoverageType.SIGNED);
        XPathExpression soap12Expression = 
            new XPathExpression("/soapenv12:Envelope/soapenv12:Body", CoverageType.SIGNED);

        if (signBody) {
            if (!xPaths.contains(soap11Expression)) {
                xPaths.add(soap11Expression);
            }
            if (!xPaths.contains(soap12Expression)) {
                xPaths.add(soap12Expression);
            }
        } else {
            if (xPaths.contains(soap11Expression)) {
                xPaths.remove(soap11Expression);
            }
            if (xPaths.contains(soap12Expression)) {
                xPaths.remove(soap12Expression);
            }
        }
    }

    public boolean isSignTimestamp() {
        return signTimestamp;
    }

    public final void setSignTimestamp(boolean signTimestamp) {
        this.signTimestamp = signTimestamp;
        
        XPathExpression soap11Expression = 
            new XPathExpression(
                "/soapenv:Envelope/soapenv:Header/wsse:Security/wsu:Timestamp", 
                CoverageType.SIGNED
            );
        XPathExpression soap12Expression = 
            new XPathExpression(
                "/soapenv12:Envelope/soapenv12:Header/wsse:Security/wsu:Timestamp", 
                CoverageType.SIGNED
            );
        
        if (signTimestamp) {
            if (!xPaths.contains(soap11Expression)) {
                xPaths.add(soap11Expression);
            }
            if (!xPaths.contains(soap12Expression)) {
                xPaths.add(soap12Expression);
            }
        } else {
            if (xPaths.contains(soap11Expression)) {
                xPaths.remove(soap11Expression);
            }
            if (xPaths.contains(soap12Expression)) {
                xPaths.remove(soap12Expression);
            }
        }
    }

    public boolean isEncryptBody() {
        return encryptBody;
    }

    public final void setEncryptBody(boolean encryptBody) {
        this.encryptBody = encryptBody;
        
        XPathExpression soap11Expression = 
            new XPathExpression("/soapenv:Envelope/soapenv:Body", CoverageType.ENCRYPTED,
                    CoverageScope.CONTENT);
        XPathExpression soap12Expression = 
            new XPathExpression("/soapenv12:Envelope/soapenv12:Body", CoverageType.ENCRYPTED,
                    CoverageScope.CONTENT);

        if (encryptBody) {
            if (!xPaths.contains(soap11Expression)) {
                xPaths.add(soap11Expression);
            }
            if (!xPaths.contains(soap12Expression)) {
                xPaths.add(soap12Expression);
            }
        } else {
            if (xPaths.contains(soap11Expression)) {
                xPaths.remove(soap11Expression);
            }
            if (xPaths.contains(soap12Expression)) {
                xPaths.remove(soap12Expression);
            }
        }
    }

    public boolean isSignAddressingHeaders() {
        return signAddressingHeaders;
    }

    public final void setSignAddressingHeaders(boolean signAddressingHeaders) {
        this.signAddressingHeaders = signAddressingHeaders;
        
        XPathExpression soap11Expression = 
            new XPathExpression(
                "/soapenv:Envelope/soapenv:Header/wsa:ReplyTo", 
                CoverageType.SIGNED
            );
        XPathExpression soap11Expression2 = 
            new XPathExpression(
                "/soapenv:Envelope/soapenv:Header/wsa:FaultTo", 
                CoverageType.SIGNED
            );
        XPathExpression soap12Expression = 
            new XPathExpression(
                "/soapenv12:Envelope/soapenv12:Header/wsa:ReplyTo", 
                CoverageType.SIGNED
            );
        XPathExpression soap12Expression2 = 
            new XPathExpression(
                "/soapenv12:Envelope/soapenv12:Header/wsa:FaultTo", 
                CoverageType.SIGNED
            );

        if (signAddressingHeaders) {
            if (!xPaths.contains(soap11Expression)) {
                xPaths.add(soap11Expression);
            }
            if (!xPaths.contains(soap11Expression2)) {
                xPaths.add(soap11Expression2);
            }
            if (!xPaths.contains(soap12Expression)) {
                xPaths.add(soap12Expression);
            }
            if (!xPaths.contains(soap12Expression2)) {
                xPaths.add(soap12Expression2);
            }
        } else {
            if (xPaths.contains(soap11Expression)) {
                xPaths.remove(soap11Expression);
            }
            if (xPaths.contains(soap11Expression2)) {
                xPaths.remove(soap11Expression2);
            }
            if (xPaths.contains(soap12Expression)) {
                xPaths.remove(soap12Expression);
            }
            if (xPaths.contains(soap12Expression2)) {
                xPaths.remove(soap12Expression2);
            }
        }
    }
    
}
