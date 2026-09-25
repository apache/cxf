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

package org.apache.cxf.rs.security.xml;

import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.util.XMLUtils;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transform;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;

public class AbstractXmlSigInHandler extends AbstractXmlSecInHandler {

    private static final Set<String> ALLOWED_TRANSFORMS = Set.of(
        Transforms.TRANSFORM_ENVELOPED_SIGNATURE,
        Transforms.TRANSFORM_C14N_OMIT_COMMENTS,
        Transforms.TRANSFORM_C14N_WITH_COMMENTS,
        Transforms.TRANSFORM_C14N11_OMIT_COMMENTS,
        Transforms.TRANSFORM_C14N11_WITH_COMMENTS,
        Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS,
        Transforms.TRANSFORM_C14N_EXCL_WITH_COMMENTS);

    private boolean removeSignature = true;
    private boolean persistSignature = true;
    private boolean keyInfoMustBeAvailable = true;
    private SignatureProperties sigProps;
    /**
     * a collection of compiled regular expression patterns for the subject DN
     */
    private final Collection<Pattern> subjectDNPatterns = new ArrayList<>();

    public void setRemoveSignature(boolean remove) {
        this.removeSignature = remove;
    }

    public void setPersistSignature(boolean persist) {
        this.persistSignature = persist;
    }

    protected void checkSignature(Message message) {

        Document doc = getDocument(message);
        if (doc == null) {
            return;
        }

        Element root = doc.getDocumentElement();
        Element signatureElement = getSignatureElement(root);
        if (signatureElement == null) {
            throwFault("XML Signature is not available", null);
        }

        final String cryptoKey;
        final String propKey;
        if (RSSecurityUtils.isSignedAndEncryptedTwoWay(message)) {
            cryptoKey = SecurityConstants.ENCRYPT_CRYPTO;
            propKey = SecurityConstants.ENCRYPT_PROPERTIES;
        } else {
            cryptoKey = SecurityConstants.SIGNATURE_CRYPTO;
            propKey = SecurityConstants.SIGNATURE_PROPERTIES;
        }

        Crypto crypto = null;
        try {
            CryptoLoader loader = new CryptoLoader();
            crypto = loader.getCrypto(message, cryptoKey, propKey);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }
        boolean valid = false;
        Reference ref = null;
        try {
            XMLSignature signature = new XMLSignature(signatureElement, "", true);

            if (sigProps != null) {
                SignedInfo sInfo = signature.getSignedInfo();
                if (sigProps.getSignatureAlgo() != null
                    && !sigProps.getSignatureAlgo().equals(sInfo.getSignatureMethodURI())) {
                    throwFault("Signature Algorithm is not supported", null);
                }
                if (sigProps.getSignatureC14nMethod() != null
                    && !sigProps.getSignatureC14nMethod().equals(sInfo.getCanonicalizationMethodURI())) {
                    throwFault("Signature C14n Algorithm is not supported", null);
                }
            }

            ref = getReference(signature);
            Element signedElement = validateReference(root, ref);
            if (signedElement.hasAttributeNS(null, "ID")) {
                signedElement.setIdAttributeNS(null, "ID", true);
            }
            if (signedElement.hasAttributeNS(null, "Id")) {
                signedElement.setIdAttributeNS(null, "Id", true);
            }

            X509Certificate cert = null;
            PublicKey publicKey = null;


            // See also WSS4J SAMLUtil.getCredentialFromKeyInfo
            KeyInfo keyInfo = signature.getKeyInfo();

            if (keyInfo != null) {
                cert = keyInfo.getX509Certificate();
                if (cert != null) {
                    valid = signature.checkSignatureValue(cert);
                } else {
                    publicKey = keyInfo.getPublicKey();
                    if (publicKey != null) {
                        valid = signature.checkSignatureValue(publicKey);
                    }
                }
            } else if (!keyInfoMustBeAvailable) {
                String user = getUserName(crypto, message);
                cert = RSSecurityUtils.getCertificates(crypto, user)[0];
                publicKey = cert.getPublicKey();
                valid = signature.checkSignatureValue(cert);
            }

            // validate trust
            new TrustValidator().validateTrust(crypto, cert, publicKey, getSubjectContraints(message));
            if (valid && persistSignature) {
                if (signature.getKeyInfo() != null) {
                    message.put(SIGNING_CERT, signature.getKeyInfo().getX509Certificate());
                }
                if (signature.getKeyInfo() != null) {
                    message.put(SIGNING_PUBLIC_KEY, signature.getKeyInfo().getPublicKey());
                }
                message.setContent(Element.class, signedElement);
            }
        } catch (Exception ex) {
            throwFault("Signature validation failed", ex);
        }
        if (!valid) {
            throwFault("Signature validation failed", null);
        }
        Element signedEl = getSignedElement(root, ref);
        // Only pass on the signed element. This is the root for an enveloped signature, and
        // a child of the (unsigned) root for a detached signature. The reader's document
        // is still the full document, e.g. for SamlEnvelopedInHandler to get the assertion
        Element body = isEnveloping(root) ? root : signedEl;
        if (removeSignature) {
            if (!isEnveloping(root)) {
                signedEl.removeAttribute("ID");
                root.removeChild(signatureElement);
            } else {
                Document newDoc = DOMUtils.createDocument();
                newDoc.adoptNode(signedEl);
                body = signedEl;
            }
        }
        message.setContent(XMLStreamReader.class,
                           new W3CDOMStreamReader(body));
        message.setContent(InputStream.class, null);

    }

    protected String getUserName(Crypto crypto, Message message) {
        SecurityContext sc = message.get(SecurityContext.class);
        if (sc != null && sc.getUserPrincipal() != null) {
            return sc.getUserPrincipal().getName();
        }
        return RSSecurityUtils.getUserName(crypto, null);

    }

    private Element getSignatureElement(Element sigParentElement) {
        if (isEnveloping(sigParentElement)) {
            return sigParentElement;
        }
        return DOMUtils.getFirstChildWithName(sigParentElement, Constants.SignatureSpecNS, "Signature");
    }

    protected boolean isEnveloping(Element root) {
        return Constants.SignatureSpecNS.equals(root.getNamespaceURI())
                && "Signature".equals(root.getLocalName());
    }

    protected Reference getReference(XMLSignature sig) {
        int count = sig.getSignedInfo().getLength();
        if (count != 1) {
            throwFault("Multiple Signature References are not currently supported", null);
        }
        try {
            return sig.getSignedInfo().item(0);
        } catch (XMLSecurityException ex) {
            throwFault("Signature Reference is not available", ex);
        }
        return null;
    }

    protected Element validateReference(Element root, Reference ref) {
        boolean enveloped = false;

        String refId = ref.getURI();

        if (!refId.startsWith("#") || refId.length() <= 1) {
            throwFault("Only local Signature References are supported", null);
        }

        Element signedEl = getSignedElement(root, ref);
        if (signedEl != null) {
            enveloped = signedEl == root;
        } else {
            throwFault("Signature Reference ID is invalid", null);
        }


        Transforms transforms = null;
        try {
            transforms = ref.getTransforms();
        } catch (XMLSecurityException ex) {
            throwFault("Signature transforms can not be obtained", ex);
        }

        boolean c14TransformConfirmed = false;
        String c14TransformExpected = sigProps != null ? sigProps.getSignatureC14nTransform() : null;
        boolean envelopedConfirmed = false;
        for (int i = 0; i < transforms.getLength(); i++) {
            String transformURI = null;
            try {
                Transform tr = transforms.item(i);
                transformURI = tr.getURI();
            } catch (Exception ex) {
                throwFault("Problem accessing Transform instance", ex);
            }
            // Only allow transforms which cover the whole of the signed element, so that
            // no unsigned content (e.g. excluded via XPath) is passed on to the application
            if (!ALLOWED_TRANSFORMS.contains(transformURI)) {
                throwFault("Signature Transform is not supported", null);
            }
            if (Transforms.TRANSFORM_ENVELOPED_SIGNATURE.equals(transformURI)) {
                envelopedConfirmed = true;
            } else if (c14TransformExpected != null && c14TransformExpected.equals(transformURI)) {
                c14TransformConfirmed = true;
            }
        }
        if (enveloped && !envelopedConfirmed) {
            throwFault("Only enveloped signatures are currently supported", null);
        }
        // The Signature is a child of the document root, so an enveloped signature
        // must reference the root. Otherwise the signed element could be wrapped in
        // unsigned content which would then be passed on to the application.
        if (envelopedConfirmed && !enveloped) {
            throwFault("Enveloped signature must reference the document root", null);
        }
        if (isEnveloping(root)) {
            validateEnvelopingReference(root, signedEl);
        }
        if (c14TransformExpected != null && !c14TransformConfirmed) {
            throwFault("Transform Canonicalization is not supported", null);
        }

        if (sigProps != null && sigProps.getSignatureDigestAlgo() != null) {
            Element dm =
                DOMUtils.getFirstChildWithName(ref.getElement(), Constants.SignatureSpecNS, "DigestMethod");
            if (dm != null && !dm.getAttribute("Algorithm").equals(
                sigProps.getSignatureDigestAlgo())) {
                throwFault("Signature Digest Algorithm is not supported", null);
            }
        }
        return signedEl;
    }

    // The signed element must be the only element in the only Object of the enveloping
    // Signature, so that no unsigned content can be passed on to the application
    private void validateEnvelopingReference(Element root, Element signedEl) {
        List<Element> objects = DOMUtils.getChildrenWithName(root, Constants.SignatureSpecNS, "Object");
        if (objects.size() != 1 || signedEl.getParentNode() != objects.get(0)
            || DOMUtils.getFirstElement(objects.get(0)) != signedEl
            || DOMUtils.getNextElement(signedEl) != null) {
            throwFault("Enveloping signature must reference the signed Object content", null);
        }
    }

    private Element getSignedElement(Element root, Reference ref) {
        String rootId = root.getAttribute("ID");
        String expectedID = ref.getURI().substring(1);

        if (!expectedID.equals(rootId)) {
            return XMLUtils.findElementById(root, expectedID, true);
        }
        return root;
    }

    public void setSignatureProperties(SignatureProperties properties) {
        this.sigProps = properties;
    }

    public void setKeyInfoMustBeAvailable(boolean use) {
        this.keyInfoMustBeAvailable = use;
    }

    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate
     */
    public void setSubjectConstraints(List<String> constraints) {
        if (constraints != null) {
            subjectDNPatterns.clear();
            for (String constraint : constraints) {
                try {
                    subjectDNPatterns.add(Pattern.compile(constraint.trim()));
                } catch (PatternSyntaxException ex) {
                    throw ex;
                }
            }
        }
    }

    private Collection<Pattern> getSubjectContraints(Message msg) throws PatternSyntaxException {
        String certConstraints =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SUBJECT_CERT_CONSTRAINTS, msg);
        // Check the message property first. If this is not null then use it. Otherwise pick up
        // the constraints set as a property
        if (certConstraints != null) {
            String[] certConstraintsList = certConstraints.split(",");
            if (certConstraintsList != null) {
                subjectDNPatterns.clear();
                for (String certConstraint : certConstraintsList) {
                    subjectDNPatterns.add(Pattern.compile(certConstraint.trim()));
                }
            }
        }
        return subjectDNPatterns;
    }

}
