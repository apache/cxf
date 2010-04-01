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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageScope;
import org.apache.cxf.ws.security.wss4j.CryptoCoverageUtil.CoverageType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityEngineResult;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.handler.WSHandlerConstants;
import org.apache.ws.security.handler.WSHandlerResult;
import org.apache.ws.security.util.WSSecurityUtil;


/**
 * Utility to enable the checking of WS-Security signature/encryption
 * coverage based on the results of the WSS4J processors.  This interceptor
 * provides an alternative to using WS-Policy based configuration for crypto
 * coverage enforcement.
 * <p/>
 * Note that the processor must properly address the Security Token
 * Reference Dereference transform in the case of a signed security token
 * such as a SAML assertion.  Consequently, a version of WSS4J that properly
 * addresses this transform must be used with this utility if you wish to 
 * check coverage over a message part referenced through the Security Token
 * Reference Dereference transform.
 * See <a href="https://issues.apache.org/jira/browse/WSS-222">WSS-222</a>
 * for more details.
 */
public class CryptoCoverageChecker extends AbstractSoapInterceptor {
    
    /**
     * The XPath expressions for locating elements in SOAP messages
     * that must be covered.  See {@link #prefixMap}
     * for namespace prefixes available.
     */
    protected List<XPathExpression> xPaths = new ArrayList<XPathExpression>();
    
    /**
     * Mapping of namespace prefixes to namespace URIs.
     */
    protected Map<String, String> prefixMap = new HashMap<String, String>();
    
    /**
     * Creates a new instance.  See {@link #setPrefixes()} and {@link #setXpaths()}
     * for providing configuration options.
     */
    public CryptoCoverageChecker() {
        this(null, null);
    }
    
    /**
     * Creates a new instance that checks for signature coverage over matches to
     * the provided XPath expressions making defensive copies of provided arguments.
     * 
     * @param prefixes
     *            mapping of namespace prefixes to namespace URIs
     * @param xPaths
     *            a list of XPath expressions
     */
    public CryptoCoverageChecker(Map<String, String> prefixes, List<XPathExpression> xPaths)
    {
        super(Phase.PRE_PROTOCOL);
        this.addAfter(WSS4JInInterceptor.class.getName());
        this.setPrefixes(prefixes);
        this.setXPaths(xPaths);
    }

    /**
     * Checks that the WSS4J results refer to the required signed/encrypted
     * elements as defined by the XPath expressions in {@link #xPaths}.
     * 
     * @param message
     *            the SOAP message containing the signature
     * 
     * @throws SoapFault
     *             if there is an error evaluating an XPath or an element is not
     *             covered by the required cryptographic operation
     */
    public void handleMessage(SoapMessage message) throws Fault {
        final Collection<WSDataRef> signed = new HashSet<WSDataRef>();
        final Collection<WSDataRef> encrypted = new HashSet<WSDataRef>();
        
        List<Object> results = CastUtils.cast(
                (List<?>) message.get(WSHandlerConstants.RECV_RESULTS));
        
        for (Object result : results) {
        
            final WSHandlerResult wshr = (WSHandlerResult) result;
            final Vector<Object> wsSecurityEngineSignResults = new Vector<Object>();
            final Vector<Object> wsSecurityEngineEncResults = new Vector<Object>();
            
            WSSecurityUtil.fetchAllActionResults(wshr.getResults(),
                    WSConstants.SIGN, wsSecurityEngineSignResults);
            
            WSSecurityUtil.fetchAllActionResults(wshr.getResults(),
                    WSConstants.ENCR, wsSecurityEngineEncResults);
            
            for (Object o : wsSecurityEngineSignResults) {
                WSSecurityEngineResult wser = (WSSecurityEngineResult) o;
            
                List<WSDataRef> sl = CastUtils.cast((List<?>) wser
                        .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));
                if (sl != null) {
                    if (sl.size() == 1
                        && sl.get(0).getName().equals(new QName(WSConstants.SIG_NS, WSConstants.SIG_LN))) {
                        //endorsing the signature so don't include
                        break;
                    }
                    
                    for (WSDataRef r : sl) {
                        signed.add(r);
                    }
                }
            }
            
            for (Object o : wsSecurityEngineEncResults) {
                WSSecurityEngineResult wser = (WSSecurityEngineResult) o;
            
                List<WSDataRef> el = CastUtils.cast((List<?>) wser
                        .get(WSSecurityEngineResult.TAG_DATA_REF_URIS));

                if (el != null) {
                    for (WSDataRef r : el) {
                        encrypted.add(r);
                    }
                }
            }
        }
        
        for (XPathExpression xPathExpression : this.xPaths) {
            Collection<WSDataRef> refsToCheck = null;
            
            switch (xPathExpression.getType()) {
            case SIGNED:
                refsToCheck = signed;
                break;
            case ENCRYPTED:
                refsToCheck = encrypted;
                break;
            default:
                throw new IllegalStateException("Unexpected crypto type: " 
                        + xPathExpression.getType());
            }
                    
            try {
                CryptoCoverageUtil.checkCoverage(
                        message.getContent(SOAPMessage.class),
                        refsToCheck,
                        this.prefixMap, 
                        xPathExpression.getXPath(),
                        xPathExpression.getType(),
                        xPathExpression.getScope());
            } catch (WSSecurityException e) {
                throw new SoapFault("No " + xPathExpression.getType()
                        + " element found matching XPath "
                        + xPathExpression.getXPath(), Fault.FAULT_CODE_CLIENT);
            }
        }
    }

    /**
     * Sets the XPath expressions to check for, clearing all previously
     * set expressions.
     *
     * @param xPaths the XPath expressions to check for
     */
    public final void setXPaths(List<XPathExpression> xPaths) {
        this.xPaths.clear();
        if (xPaths != null) {
            this.xPaths.addAll(xPaths);
        }
    }

    /**
     * Sets the mapping of namespace prefixes to namespace URIs, clearing all previously
     * set mappings.
     *
     * @param prefixes the mapping of namespace prefixes to namespace URIs
     */
    public final void setPrefixes(Map<String, String> prefixes) {
        this.prefixMap.clear();
        if (prefixes != null) {
            this.prefixMap.putAll(prefixes);
        }
    }

    /**
     * A simple wrapper for an XPath expression and coverage type / scope
     * indicating how the XPath expression should be enforced as a cryptographic
     * coverage requirement.
     */
    public static class XPathExpression {
        
        /**
         * The XPath expression.
         */
        private final String xPath;
        
        /**
         * The type of coverage that is being enforced.
         */
        private final CoverageType type;
        
        /**
         * The scope of the coverage that is being enforced.
         */
        private final CoverageScope scope;

        /**
         * Create a new expression indicating a cryptographic coverage
         * requirement. If {@code type} is {@link CoverageType#SIGNED}, the
         * {@code scope} {@link CoverageScope#CONTENT} does not represent a
         * configuration supported in WS-Security.
         * 
         * @param xPath
         *            the XPath expression
         * @param type
         *            the type of coverage that the expression is meant to
         *            enforce
         * @param scope
         *            the scope of coverage that the expression is meant to
         *            enforce, defaults to {@link CoverageScope#ELEMENT}
         * 
         * @throws NullPointerException
         *             if {@code xPath} or {@code type} is {@code null}
         */
        public XPathExpression(String xPath, CoverageType type, CoverageScope scope) {
            if (xPath == null) {
                throw new NullPointerException("xPath cannot be null.");
            } else if (type == null) {
                throw new NullPointerException("type cannot be null.");
            }
            
            this.xPath = xPath;
            this.type = type;
            this.scope = scope;
        }

        /**
         * Returns the XPath expression.
         * @return the XPath expression
         */
        public String getXPath() {
            return this.xPath;
        }

        /**
         * Returns the coverage type.
         * @return the coverage type
         */
        public CoverageType getType() {
            return this.type;
        }

        /**
         * Returns the coverage scope.
         * @return the coverage scope
         */
        public CoverageScope getScope() {
            return this.scope;
        }
    }
}
