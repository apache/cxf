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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.MapNamespaceContext;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.apache.ws.security.WSDataRef;
import org.apache.ws.security.WSSecurityException;


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
     * elements be signed.
     * 
     * @param signedRefs references to the signed content in the message
     * @param encryptedRefs refernces to the encrypted content in the message
     */
    public static void reconcileEncryptedSignedRefs(final Collection<WSDataRef> signedRefs, 
            final Collection<WSDataRef> encryptedRefs) {
        
        final List<WSDataRef> encryptedSignedRefs = new LinkedList<WSDataRef>();
        
        for (WSDataRef encryptedRef : encryptedRefs) {
            final String encryptedRefId = encryptedRef.getWsuId();
            final Iterator<WSDataRef> signedRefsIt = signedRefs.iterator();
            while (signedRefsIt.hasNext()) {
                final WSDataRef signedRef = signedRefsIt.next();
                
                if (signedRef.getWsuId().equals(encryptedRefId)
                        || signedRef.getWsuId().equals("#" + encryptedRefId)) {
                    
                    final WSDataRef encryptedSignedRef = 
                        new WSDataRef(signedRef.getDataref());
                    
                    encryptedSignedRef.setContent(false);
                    encryptedSignedRef.setName(encryptedRef.getName());
                    encryptedSignedRef.setProtectedElement(encryptedRef
                            .getProtectedElement());
                    // This value is the ID of the encrypted element, not
                    // the value of the ID in the decrypted content 
                    // (WSS4J 1.5.8).  Therefore, passing it along does
                    // not provide much value.
                    //encryptedSignedRef.setWsuId(encryptedRef.getWsuId());
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
     * @param message
     *            the soap message containing the signature/encryption and content
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
     *             covered by the signture/encryption.
     */
    public static void checkBodyCoverage(
        SOAPMessage message,
        final Collection<WSDataRef> refs,
        CoverageType type,
        CoverageScope scope) throws WSSecurityException {
        
        final Element body;
        
        try {
            body = message.getSOAPBody();
        } catch (SOAPException e1) {
            // Can't get the SAAJ parts out of the document.
            throw new WSSecurityException(WSSecurityException.FAILURE);
        }
        
        if (!CryptoCoverageUtil.matchElement(refs, type, scope, body)) {
            throw new WSSecurityException("The " + getCoverageTypeString(type)
                    + " does not cover the required elements (soap:Body).");
        }
    }

    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted SOAP header element(s) matching the provided name and
     * namespace.  If {@code name} is null, all headers from {@code namespace}
     * are inspected for coverage.
     * 
     * @param message
     *            the soap message containing the signature/encryption and content
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
     *             covered by the signture/encryption.
     */
    public static void checkHeaderCoverage(
            SOAPMessage message,
            final Collection<WSDataRef> refs,
            String namespace,
            String name,
            CoverageType type,
            CoverageScope scope) throws WSSecurityException {
        
        final List<Element> elements;
        final Element parent;
        
        try {
            parent = message.getSOAPHeader();
        } catch (SOAPException e1) {
            // Can't get the SAAJ parts out of the document.
            throw new WSSecurityException(WSSecurityException.FAILURE);
        }
        
        if (name == null) {
            elements = DOMUtils.getChildrenWithNamespace(parent, namespace);
        } else {
            elements = DOMUtils.getChildrenWithName(
                    parent, namespace, name);
        }
        
        for (Element el : elements) {
            if (!CryptoCoverageUtil.matchElement(refs, type, scope, el)) {
                throw new WSSecurityException("The " + getCoverageTypeString(type)
                        + " does not cover the required elements ({"
                        + namespace + "}" + name + ").");
            }
        }          
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted elements as defined by the XPath expression in {@code
     * xPath}.
     * 
     * @param message
     *            the soap message containing the signature/encryption and content
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
     *             covered by the signture/encryption.
     */
    public static void checkCoverage(
            SOAPMessage message,
            final Collection<WSDataRef> refs,
            Map<String, String> namespaces,
            String xPath,
            CoverageType type,
            CoverageScope scope) throws WSSecurityException {
        
        CryptoCoverageUtil.checkCoverage(message, refs, namespaces, Arrays
                .asList(xPath), type, scope);
    }
    
    /**
     * Checks that the references provided refer to the required
     * signed/encrypted elements as defined by the XPath expressions in {@code
     * xPaths}.
     * 
     * @param message
     *            the soap message containing the signature/encryption and content
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
     *             covered by the signture/encryption.
     */
    public static void checkCoverage(
            SOAPMessage message,
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
        
        // For each XPath
        for (String xpathString : xPaths) {
            // Get the matching nodes
            NodeList list;
            try {
                list = (NodeList)xpath.evaluate(
                        xpathString, 
                        message.getSOAPPart().getEnvelope(),
                        XPathConstants.NODESET);
            } catch (XPathExpressionException e) {
                // The xpath's are not valid in the config.
                throw new WSSecurityException(WSSecurityException.FAILURE);
            } catch (SOAPException e) {
                // Can't get the SAAJ parts out of the document.
                throw new WSSecurityException(WSSecurityException.FAILURE);
            }
            
            // If we found nodes then we need to do the check.
            if (list.getLength() != 0) {
                // For each matching element, check for a ref that
                // covers it.
                for (int x = 0; x < list.getLength(); x++) {
                    
                    final Element el = (Element)list.item(x);
                    
                    boolean instanceMatched = CryptoCoverageUtil.
                            matchElement(refs, type, scope, el);
                    
                    // We looked through all of the refs, but the element was
                    // not signed.
                    if (!instanceMatched) {
                        throw new WSSecurityException("The " + getCoverageTypeString(type)
                                + " does not cover the required elements ("
                                + xpathString + ").");
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
        
        boolean instanceMatched = false;
        
        for (WSDataRef r : refs) {
            
            // If the element is the same object instance
            // as that in the ref, we found it and can
            // stop looking at this element.
            if (r.getProtectedElement() == el 
                    && r.isContent() == content) {
                instanceMatched = true;
                break;
            }
            
            // Only if checking signature coverage do we attempt to
            // do matches based on ID and element names and not object
            // equality.
            if (!instanceMatched && CoverageType.SIGNED.equals(type)) {
                // If we get here, we haven't found it yet
                // so we will look based on the element's
                // wsu:Id and see if the ref references the
                // ID specified in the attr.
                Attr idAttr = el.getAttributeNodeNS(
                        PolicyConstants.WSU_NAMESPACE_URI,
                        "Id");
                
                // We didn't get it with a qualified name, so
                // look for the attribute using only the local name.
                if (idAttr == null) {
                    idAttr = el.getAttributeNode("Id");
                }
                
                String id = idAttr == null ? null : idAttr.getValue();
                
                // If the ref's qualified name equals the name of the
                // element and the ref has a wsu:Id and it matches the
                // element's wsu:Id attribute value, we found it.
                if (r.getName().equals(
                        new QName(el.getNamespaceURI(), el
                                .getLocalName()))
                        && r.getWsuId() != null
                        && (r.getWsuId().equals(id) || r.getWsuId()
                                .equals("#" + id))) {
                    instanceMatched = true;
                    break;
                }
            }
        }
        return instanceMatched;
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

