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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSDataRef;


/**
 * Utility to enable the checking of WS-Security signature / WS-Security
 * encryption coverage based on the results of the WSS4J signature/encryption
 * processor.
 */
public final class CryptoCoverageUtil {
    /**
     * Hidden in utility class.
     */
    private CryptoCoverageUtil() {
    }
    
    /**
     * Inspects the signed and encrypted content in the message and accurately
     * resolves encrypted and then signed elements in {@code signedRefs}.
     * Entries in {@code signedRefs} that correspond to an encrypted element
     * are resolved to the decrypted element and added to {@code signedRefs}.
     * The original reference to the encrypted content remains unaltered in the
     * list to allow for matching against a requirement that xenc:EncryptedData
     * and xenc:EncryptedKey elements be signed.
     * 
     * @param signedRefs references to the signed content in the message
     * @param encryptedRefs references to the encrypted content in the message
     */
    public static void reconcileEncryptedSignedRefs(final Collection<WSDataRef> signedRefs, 
            final Collection<WSDataRef> encryptedRefs) {
        
        final List<WSDataRef> encryptedSignedRefs = new LinkedList<>();
        
        for (WSDataRef encryptedRef : encryptedRefs) {
            for (WSDataRef signedRef : signedRefs) {
                if (signedRef.getProtectedElement() == encryptedRef.getEncryptedElement()) {

                    final WSDataRef encryptedSignedRef = new WSDataRef();
                    encryptedSignedRef.setWsuId(signedRef.getWsuId());
                    
                    encryptedSignedRef.setContent(false);
                    encryptedSignedRef.setName(encryptedRef.getName());
                    encryptedSignedRef.setProtectedElement(encryptedRef
                            .getProtectedElement());
                    
                    encryptedSignedRef.setXpath(encryptedRef.getXpath());
                    
                    encryptedSignedRefs.add(encryptedSignedRef);
                }
            }
        }
        
        signedRefs.addAll(encryptedSignedRefs);
    }

    /**
     * Checks that the references provided refer to the
     * signed/encrypted SOAP body element.
     * 
     * @param soapBody
     *            the SOAP body element
     * @param refs
     *            the refs to the data extracted from the signature/encryption
     * @param type
     *            the type of cryptographic coverage to check for
     * @param scope
     *            the scope of the cryptographic coverage to check for, defaults
     *            to element
     * 
     * @throws WSSecurityException
     *             if there is an error evaluating the coverage or the body is not
     *             covered by the signature/encryption.
     */
    public static void checkBodyCoverage(
        Element soapBody,
        final Collection<WSDataRef> refs,
        CoverageType type,
        CoverageScope scope
    ) throws WSSecurityException {
        if (!CryptoCoverageUtil.matchElement(refs, type, scope, soapBody)) {
            Exception ex = new Exception("The " + getCoverageTypeString(type)
                    + " does not cover the required elements (soap:Body).");
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }
    }

    public static void checkAttachmentsCoverage(
        Collection<org.apache.cxf.message.Attachment> attachments,
        final Collection<WSDataRef> refs,
        CoverageType type,
        CoverageScope scope
    ) throws WSSecurityException {
        String requiredTransform = null;
        if (type == CoverageType.SIGNED && scope == CoverageScope.CONTENT) {
            requiredTransform = WSConstants.SWA_ATTACHMENT_CONTENT_SIG_TRANS;
        } else if (type == CoverageType.SIGNED) {
            requiredTransform = WSConstants.SWA_ATTACHMENT_COMPLETE_SIG_TRANS;
        }
        
        if (attachments != null) {
            // For each matching attachment, check for a ref that covers it.
            for (org.apache.cxf.message.Attachment attachment : attachments) {
                boolean matched = false;
                
                for (WSDataRef r : refs) {
                    String id = r.getWsuId();
                    if (id != null && id.startsWith("cid:")) {
                        id = id.substring(4);
                    }
                    
                    if (r.isAttachment() && attachment.getId() != null && attachment.getId().equals(id) 
                        && (CoverageType.ENCRYPTED == type || r.getTransformAlgorithms() != null
                        && r.getTransformAlgorithms().contains(requiredTransform))) {
                        matched = true;
                        break;
                    }
                }
                
                // We looked through all of the refs, but the element was not signed/encrypted
                if (!matched) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                            new Exception("The " + getCoverageTypeString(type)
                            + " does not cover the required elements"));
                }
            }
        }
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted SOAP header element(s) matching the provided name and
     * namespace.  If {@code name} is null, all headers from {@code namespace}
     * are inspected for coverage.
     * 
     * @param soapHeader
     *            the SOAP header element
     * @param refs
     *            the refs to the data extracted from the signature/encryption
     * @param namespaces
     *            the namespace of the header(s) to check for coverage
     * @param name
     *            the local part of the header name to check for coverage, may be null
     * @param type
     *            the type of cryptographic coverage to check for
     * @param scope
     *            the scope of the cryptographic coverage to check for, defaults
     *            to element
     * 
     * @throws WSSecurityException
     *             if there is an error evaluating the coverage or a header is not
     *             covered by the signature/encryption.
     */
    public static void checkHeaderCoverage(
            Element soapHeader,
            final Collection<WSDataRef> refs,
            String namespace,
            String name,
            CoverageType type,
            CoverageScope scope) throws WSSecurityException {
        
        final List<Element> elements;
        if (name == null) {
            elements = DOMUtils.getChildrenWithNamespace(soapHeader, namespace);
        } else {
            elements = DOMUtils.getChildrenWithName(soapHeader, namespace, name);
        }
        
        for (Element el : elements) {
            if (!CryptoCoverageUtil.matchElement(refs, type, scope, el)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                        new Exception("The " + getCoverageTypeString(type)
                        + " does not cover the required elements ({"
                        + namespace + "}" + name + ")."));
            }
        }          
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted elements as defined by the XPath expression in {@code
     * xPath}.
     * 
     * @param soapEnvelope
     *            the SOAP Envelope element
     * @param refs
     *            the refs to the data extracted from the signature/encryption
     * @param namespaces
     *            the prefix to namespace mapping, may be {@code null}
     * @param xPath
     *            the XPath expression
     * @param type
     *            the type of cryptographic coverage to check for
     * @param scope
     *            the scope of the cryptographic coverage to check for, defaults
     *            to element
     * 
     * @throws WSSecurityException
     *             if there is an error evaluating an XPath or an element is not
     *             covered by the signature/encryption.
     */
    public static void checkCoverage(
            Element soapEnvelope,
            final Collection<WSDataRef> refs,
            Map<String, String> namespaces,
            String xPath,
            CoverageType type,
            CoverageScope scope) throws WSSecurityException {
        
        CryptoCoverageUtil.checkCoverage(soapEnvelope, refs, namespaces, Arrays
                .asList(xPath), type, scope);
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted elements as defined by the XPath expressions in {@code
     * xPaths}.
     * 
     * @param soapEnvelope
     *            the SOAP Envelope element
     * @param refs
     *            the refs to the data extracted from the signature/encryption
     * @param namespaces
     *            the prefix to namespace mapping, may be {@code null}
     * @param xPaths
     *            the collection of XPath expressions
     * @param type
     *            the type of cryptographic coverage to check for
     * @param scope
     *            the scope of the cryptographic coverage to check for, defaults
     *            to element
     * 
     * @throws WSSecurityException
     *             if there is an error evaluating an XPath or an element is not
     *             covered by the signature/encryption.
     */
    public static void checkCoverage(
            Element soapEnvelope,
            final Collection<WSDataRef> refs,
            Map<String, String> namespaces,
            Collection<String> xPaths,
            CoverageType type,
            CoverageScope scope) throws WSSecurityException {
        
        // XPathFactory and XPath are not thread-safe so we must recreate them
        // each request.
        final XPathFactory factory = XPathFactory.newInstance();
        final XPath xpath = factory.newXPath();
        
        if (namespaces != null) {
            xpath.setNamespaceContext(new MapNamespaceContext(namespaces));
        }
        
        checkCoverage(soapEnvelope, refs, xpath, xPaths, type, scope);
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted elements as defined by the XPath expressions in {@code
     * xPaths}.
     */
    public static void checkCoverage(
            Element soapEnvelope,
            final Collection<WSDataRef> refs,
            final XPath xpath,
            Collection<String> xPaths,
            CoverageType type,
            CoverageScope scope
    ) throws WSSecurityException {
        
        // For each XPath
        for (String xpathString : xPaths) {
            // Get the matching nodes
            NodeList list;
            try {
                list = (NodeList)xpath.evaluate(
                        xpathString, 
                        soapEnvelope,
                        XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                // The xpath's are not valid in the config.
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE);
            }
            
            // If we found nodes then we need to do the check.
            if (list.getLength() != 0) {
                // For each matching element, check for a ref that
                // covers it.
                for (int x = 0; x < list.getLength(); x++) {
                    
                    final Element el = (Element)list.item(x);
                    
                    boolean instanceMatched = CryptoCoverageUtil.matchElement(refs, type, scope, el);
                    
                    // We looked through all of the refs, but the element was
                    // not signed.
                    if (!instanceMatched) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE,
                                new Exception("The " + getCoverageTypeString(type)
                                + " does not cover the required elements ("
                                + xpathString + ")."));
                    }
                }
            }
        }
    }
    
    private static boolean matchElement(Collection<WSDataRef> refs,
            CoverageType type, CoverageScope scope, Element el) {
        final boolean content;
        
        switch (scope) {
        case CONTENT:
            content = true;
            break;
        case ELEMENT:
        default:
            content = false;
        }
        
        for (WSDataRef r : refs) {
            // If the element is the same object instance
            // as that in the ref, we found it and can
            // stop looking at this element.
            if (r.getProtectedElement() == el && r.isContent() == content) {
                return true;
            }
        }
        return false;
    }
    
    private static String getCoverageTypeString(CoverageType type) {
        String typeString;
        
        switch (type) {
        case SIGNED:
            typeString = "signature";
            break;
        case ENCRYPTED:
            typeString = "encryption";
            break;
        default:
            typeString = "crpytography";
        }
        return typeString;
    }
    
    /**
     * Differentiates which type of cryptographic coverage to check for.
     */
    public static enum CoverageType {
        /**
         * Checks for encryption of the matching elements.
         */
        ENCRYPTED,
        /**
         * Checks for a signature over the matching elements.
         */
        SIGNED
    }
    
    /**
     * Differentiates which part of an element to check for cryptographic coverage.
     */
    public static enum CoverageScope {
        /**
         * Checks for encryption of the matching elements.
         */
        CONTENT,
        /**
         * Checks for a signature over the matching elements.
         */
        ELEMENT
    }
}

