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


import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.ws.security.WSConstants;

/**
 * This utility extends the CryptoCoverageChecker to provide an easy way to check to see
 * if the SOAP (1.1 + 1.2) Body and Timestamp were signed.
 */
public class SignatureCoverageChecker extends CryptoCoverageChecker {
    
    public static final String SOAP_NS = WSConstants.URI_SOAP11_ENV;
    public static final String SOAP12_NS = WSConstants.URI_SOAP12_ENV;
    public static final String WSU_NS = WSConstants.WSU_NS;
    public static final String WSSE_NS = WSConstants.WSSE_NS;
    
    /**
     * Creates a new instance. Enforces that the SOAP Body and Timestamp must be signed
     * (if they exist in the message body).
     */
    public SignatureCoverageChecker(
        boolean signBody, boolean signTimestamp
    ) {
        super(null, null);
        
        if (signBody) {
            XPathExpression bodyExpression = 
                new XPathExpression("/soapenv:Envelope/soapenv:Body", CoverageType.SIGNED);
            xPaths.add(bodyExpression);
            bodyExpression = 
                new XPathExpression("/soapenv12:Envelope/soapenv12:Body", CoverageType.SIGNED);
            xPaths.add(bodyExpression);
        }
        if (signTimestamp) {
            XPathExpression timestampExpression = 
                new XPathExpression(
                    "/soapenv:Envelope/soapenv:Header/wsse:Security/wsu:Timestamp", 
                    CoverageType.SIGNED
                );
            xPaths.add(timestampExpression);
            timestampExpression = 
                new XPathExpression(
                    "/soapenv12:Envelope/soapenv12:Header/wsse:Security/wsu:Timestamp", 
                    CoverageType.SIGNED
                );
            xPaths.add(timestampExpression);
        }
        
        prefixMap.put("soapenv", SOAP_NS);
        prefixMap.put("soapenv12", SOAP12_NS);
        prefixMap.put("wsu", WSU_NS);
        prefixMap.put("wsse", WSSE_NS);
    }
    
}
