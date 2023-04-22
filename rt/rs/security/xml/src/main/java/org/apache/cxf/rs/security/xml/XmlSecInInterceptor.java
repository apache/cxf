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

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.jaxrs.impl.ReaderInterceptorContextImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rs.security.common.CryptoLoader;
import org.apache.cxf.rs.security.common.RSSecurityUtils;
import org.apache.cxf.rs.security.common.TrustValidator;
import org.apache.cxf.rt.security.SecurityConstants;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.stax.ext.InboundXMLSec;
import org.apache.xml.security.stax.ext.XMLSec;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;
import org.apache.xml.security.stax.ext.XMLSecurityProperties;
import org.apache.xml.security.stax.impl.securityToken.KeyNameSecurityToken;
import org.apache.xml.security.stax.securityEvent.AlgorithmSuiteSecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEvent;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants;
import org.apache.xml.security.stax.securityEvent.SecurityEventConstants.Event;
import org.apache.xml.security.stax.securityEvent.SecurityEventListener;
import org.apache.xml.security.stax.securityEvent.TokenSecurityEvent;
import org.apache.xml.security.stax.securityToken.SecurityToken;

/**
 * A new StAX-based interceptor for processing messages with XML Signature + Encryption content.
 */
public class XmlSecInInterceptor extends AbstractPhaseInterceptor<Message> implements ReaderInterceptor  {

    private static final Logger LOG = LogUtils.getL7dLogger(XmlSecInInterceptor.class);

    private EncryptionProperties encryptionProperties;
    private SignatureProperties sigProps;
    private String decryptionAlias;
    private String signatureVerificationAlias;
    private boolean persistSignature = true;
    private boolean requireSignature;
    private boolean requireEncryption;
    /**
     * a collection of compiled regular expression patterns for the subject DN
     */
    private Collection<Pattern> subjectDNPatterns = new ArrayList<>();

    public XmlSecInInterceptor() {
        super(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        if (!canDocumentBeRead(message)) {
            return;
        }
        prepareMessage(message);
        message.getInterceptorChain().add(
              new StaxActionInInterceptor(requireSignature, requireEncryption));
    }

    private void prepareMessage(Message inMsg) throws Fault {

        XMLStreamReader originalXmlStreamReader = inMsg.getContent(XMLStreamReader.class);
        if (originalXmlStreamReader == null) {
            InputStream is = inMsg.getContent(InputStream.class);
            if (is != null) {
                originalXmlStreamReader = StaxUtils.createXMLStreamReader(is);
            }
        }

        try {
            XMLSecurityProperties properties = new XMLSecurityProperties();
            configureDecryptionKeys(inMsg, properties);
            Crypto signatureCrypto = getSignatureCrypto(inMsg);
            configureSignatureKeys(signatureCrypto, inMsg, properties);

            SecurityEventListener securityEventListener =
                configureSecurityEventListener(signatureCrypto, inMsg, properties);
            InboundXMLSec inboundXMLSec = XMLSec.getInboundWSSec(properties);

            XMLStreamReader newXmlStreamReader =
                inboundXMLSec.processInMessage(originalXmlStreamReader, null, securityEventListener);
            inMsg.setContent(XMLStreamReader.class, newXmlStreamReader);

        } catch (XMLStreamException | XMLSecurityException | IOException | UnsupportedCallbackException e) {
            throwFault(e.getMessage(), e);
        }
    }

    private boolean canDocumentBeRead(Message message) {
        if (isServerGet(message)) {
            return false;
        }
        Integer responseCode = (Integer)message.get(Message.RESPONSE_CODE);
        return !(responseCode != null && responseCode != 200);
    }
    
    private boolean isServerGet(Message message) {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        return "GET".equals(method) && !MessageUtils.isRequestor(message);
    }


    private void configureDecryptionKeys(Message message, XMLSecurityProperties properties)
        throws IOException,
        UnsupportedCallbackException, WSSecurityException {
        final String cryptoKey;
        final String propKey;
        if (RSSecurityUtils.isSignedAndEncryptedTwoWay(message)) {
            cryptoKey = SecurityConstants.SIGNATURE_CRYPTO;
            propKey = SecurityConstants.SIGNATURE_PROPERTIES;
        } else {
            cryptoKey = SecurityConstants.ENCRYPT_CRYPTO;
            propKey = SecurityConstants.ENCRYPT_PROPERTIES;
        }

        Crypto crypto = null;
        try {
            crypto = new CryptoLoader().getCrypto(message, cryptoKey, propKey);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
        }

        if (crypto != null) {
            String alias = decryptionAlias;
            if (alias == null) {
                alias = crypto.getDefaultX509Identifier();
            }
            if (alias != null) {
                CallbackHandler callback = RSSecurityUtils.getCallbackHandler(message, this.getClass());
                WSPasswordCallback passwordCallback =
                    new WSPasswordCallback(alias, WSPasswordCallback.DECRYPT);
                callback.handle(new Callback[] {passwordCallback});

                Key privateKey = crypto.getPrivateKey(alias, passwordCallback.getPassword());
                properties.setDecryptionKey(privateKey);
            }
        }
    }

    private Crypto getSignatureCrypto(Message message) {
        final String cryptoKey;
        final String propKey;
        if (RSSecurityUtils.isSignedAndEncryptedTwoWay(message)) {
            cryptoKey = SecurityConstants.ENCRYPT_CRYPTO;
            propKey = SecurityConstants.ENCRYPT_PROPERTIES;
        } else {
            cryptoKey = SecurityConstants.SIGNATURE_CRYPTO;
            propKey = SecurityConstants.SIGNATURE_PROPERTIES;
        }

        try {
            return new CryptoLoader().getCrypto(message, cryptoKey, propKey);
        } catch (Exception ex) {
            throwFault("Crypto can not be loaded", ex);
            return null;
        }
    }

    private void configureSignatureKeys(
        Crypto sigCrypto, Message message, XMLSecurityProperties properties
    ) throws IOException,
        UnsupportedCallbackException, WSSecurityException {

        if (sigCrypto != null && signatureVerificationAlias != null) {
            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
            cryptoType.setAlias(signatureVerificationAlias);
            X509Certificate[] certs = sigCrypto.getX509Certificates(cryptoType);
            if (certs != null && certs.length > 0) {
                properties.setSignatureVerificationKey(certs[0].getPublicKey());
            }
        } else if (sigCrypto != null && sigProps != null && sigProps.getKeyNameAliasMap() != null) {
            Map<String, String> keyNameAliasMap = sigProps.getKeyNameAliasMap();
            for (Map.Entry<String, String> mapping: keyNameAliasMap.entrySet()) {
                CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
                cryptoType.setAlias(mapping.getValue());
                X509Certificate[] certs = sigCrypto.getX509Certificates(cryptoType);
                if (certs != null && certs.length > 0) {
                    properties.addKeyNameMapping(mapping.getKey(), certs[0].getPublicKey());
                }
            }
        }


    }

    protected SecurityEventListener configureSecurityEventListener(
        final Crypto sigCrypto, final Message msg, XMLSecurityProperties securityProperties
    ) {
        final List<SecurityEvent> incomingSecurityEventList = new LinkedList<>();
        SecurityEventListener securityEventListener = new SecurityEventListener() {
            @Override
            public void registerSecurityEvent(SecurityEvent securityEvent) throws XMLSecurityException {
                if (securityEvent.getSecurityEventType() == SecurityEventConstants.AlgorithmSuite) {
                    if (encryptionProperties != null) {
                        checkEncryptionAlgorithms((AlgorithmSuiteSecurityEvent)securityEvent);
                    }
                    if (sigProps != null) {
                        checkSignatureAlgorithms((AlgorithmSuiteSecurityEvent)securityEvent);
                    }
                } else if (securityEvent.getSecurityEventType() != SecurityEventConstants.EncryptedKeyToken
                    && securityEvent instanceof TokenSecurityEvent<?>) {
                    checkSignatureTrust(sigCrypto, msg, (TokenSecurityEvent<?>)securityEvent);
                }
                incomingSecurityEventList.add(securityEvent);
            }
        };
        msg.getExchange().put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);
        msg.put(SecurityEvent.class.getName() + ".in", incomingSecurityEventList);

        return securityEventListener;
    }

    private void checkEncryptionAlgorithms(AlgorithmSuiteSecurityEvent event)
        throws XMLSecurityException {
        if (XMLSecurityConstants.Enc.equals(event.getAlgorithmUsage())
            && encryptionProperties.getEncryptionSymmetricKeyAlgo() != null
            && !encryptionProperties.getEncryptionSymmetricKeyAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The symmetric encryption algorithm "
                                           + event.getAlgorithmURI() + " is not allowed"});
        } else if ((XMLSecurityConstants.Sym_Key_Wrap.equals(event.getAlgorithmUsage())
            || XMLSecurityConstants.Asym_Key_Wrap.equals(event.getAlgorithmUsage()))
            && encryptionProperties.getEncryptionKeyTransportAlgo() != null
            && !encryptionProperties.getEncryptionKeyTransportAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The key transport algorithm "
                + event.getAlgorithmURI() + " is not allowed"});
        } else if (XMLSecurityConstants.EncDig.equals(event.getAlgorithmUsage())
            && encryptionProperties.getEncryptionDigestAlgo() != null
            && !encryptionProperties.getEncryptionDigestAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The encryption digest algorithm "
                + event.getAlgorithmURI() + " is not allowed"});
        }
    }

    private void checkSignatureAlgorithms(AlgorithmSuiteSecurityEvent event)
        throws XMLSecurityException {
        if ((XMLSecurityConstants.Asym_Sig.equals(event.getAlgorithmUsage())
            || XMLSecurityConstants.Sym_Sig.equals(event.getAlgorithmUsage()))
            && sigProps.getSignatureAlgo() != null
            && !sigProps.getSignatureAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The signature algorithm "
                                           + event.getAlgorithmURI() + " is not allowed"});
        } else if (XMLSecurityConstants.SigDig.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureDigestAlgo() != null
            && !sigProps.getSignatureDigestAlgo().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The signature digest algorithm "
                + event.getAlgorithmURI() + " is not allowed"});
        } else if (XMLSecurityConstants.SigC14n.equals(event.getAlgorithmUsage())
            && sigProps.getSignatureC14nMethod() != null
            && !sigProps.getSignatureC14nMethod().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The signature c14n algorithm "
                + event.getAlgorithmURI() + " is not allowed"});
        } else if (XMLSecurityConstants.SigTransform.equals(event.getAlgorithmUsage())
            && !XMLSecurityConstants.NS_XMLDSIG_ENVELOPED_SIGNATURE.equals(event.getAlgorithmURI())
            && sigProps.getSignatureC14nTransform() != null
            && !sigProps.getSignatureC14nTransform().equals(event.getAlgorithmURI())) {
            throw new XMLSecurityException("empty", new Object[] {"The signature transformation algorithm "
                + event.getAlgorithmURI() + " is not allowed"});
        }
    }

    private void checkSignatureTrust(
        Crypto sigCrypto, Message msg, TokenSecurityEvent<?> event
    ) throws XMLSecurityException {
        SecurityToken token = event.getSecurityToken();
        if (token != null) {
            X509Certificate[] certs = token.getX509Certificates();
            if (certs == null && token.getPublicKey() == null && token instanceof KeyNameSecurityToken) {
                certs = getX509CertificatesForKeyName(sigCrypto, msg, (KeyNameSecurityToken)token);
            }

            PublicKey publicKey = token.getPublicKey();
            X509Certificate cert = null;
            if (certs != null && certs.length > 0) {
                cert = certs[0];
            }

            // validate trust
            try {
                new TrustValidator().validateTrust(sigCrypto, cert, publicKey,
                                                   getSubjectContraints(msg));
            } catch (WSSecurityException e) {
                String error = "Signature validation failed";
                throw new XMLSecurityException("empty", new Object[] {error});
            }

            if (persistSignature) {
                msg.setContent(X509Certificate.class, cert);
            }
        }
    }

    private X509Certificate[] getX509CertificatesForKeyName(Crypto sigCrypto, Message msg, KeyNameSecurityToken token)
        throws XMLSecurityException {
        X509Certificate[] certs;
        KeyNameSecurityToken keyNameSecurityToken = token;
        String keyName = keyNameSecurityToken.getKeyName();
        String alias = null;
        if (sigProps != null && sigProps.getKeyNameAliasMap() != null) {
            alias = sigProps.getKeyNameAliasMap().get(keyName);
        }
        try {
            certs = RSSecurityUtils.getCertificates(sigCrypto, alias);
        } catch (Exception e) {
            throw new XMLSecurityException("empty", new Object[] {"Error during Signature Trust "
                + "validation"});
        }
        return certs;
    }


    protected void throwFault(String error, Exception ex) {
        LOG.warning(error);
        Response response = JAXRSUtils.toResponseBuilder(400).entity(error).type("text/plain").build();
        throw ExceptionUtils.toBadRequestException(null, response);
    }

    public void setEncryptionProperties(EncryptionProperties properties) {
        this.encryptionProperties = properties;
    }

    public void setSignatureProperties(SignatureProperties properties) {
        this.sigProps = properties;
    }

    public String getDecryptionAlias() {
        return decryptionAlias;
    }

    public void setDecryptionAlias(String decryptionAlias) {
        this.decryptionAlias = decryptionAlias;
    }

    public String getSignatureVerificationAlias() {
        return signatureVerificationAlias;
    }

    public void setSignatureVerificationAlias(String signatureVerificationAlias) {
        this.signatureVerificationAlias = signatureVerificationAlias;
    }

    public void setPersistSignature(boolean persist) {
        this.persistSignature = persist;
    }

    public boolean isRequireSignature() {
        return requireSignature;
    }

    public void setRequireSignature(boolean requireSignature) {
        this.requireSignature = requireSignature;
    }

    public boolean isRequireEncryption() {
        return requireEncryption;
    }

    public void setRequireEncryption(boolean requireEncryption) {
        this.requireEncryption = requireEncryption;
    }

    /**
     * Set a list of Strings corresponding to regular expression constraints on the subject DN
     * of a certificate
     */
    public void setSubjectConstraints(List<String> constraints) {
        if (constraints != null) {
            subjectDNPatterns = new ArrayList<>();
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

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext ctx) throws IOException, WebApplicationException {
        Message message = ((ReaderInterceptorContextImpl)ctx).getMessage();

        if (!canDocumentBeRead(message)) {
            return ctx.proceed();
        }
        prepareMessage(message);
        Object object = ctx.proceed();
        new StaxActionInInterceptor(requireSignature,
                                    requireEncryption).handleMessage(message);
        return object;

    }

    /**
     * This interceptor handles parsing the StaX results (events) + checks to see whether the
     * required (if any) Actions (signature or encryption) were fulfilled.
     */
    private static class StaxActionInInterceptor extends AbstractPhaseInterceptor<Message> {

        private static final Logger LOG =
            LogUtils.getL7dLogger(StaxActionInInterceptor.class);

        private final boolean signatureRequired;
        private final boolean encryptionRequired;

        StaxActionInInterceptor(boolean signatureRequired, boolean encryptionRequired) {
            super(Phase.PRE_LOGICAL);
            this.signatureRequired = signatureRequired;
            this.encryptionRequired = encryptionRequired;
        }

        @Override
        public void handleMessage(Message message) throws Fault {

            if (!(signatureRequired || encryptionRequired)) {
                return;
            }

            @SuppressWarnings("unchecked")
            final List<SecurityEvent> incomingSecurityEventList =
                (List<SecurityEvent>)message.get(SecurityEvent.class.getName() + ".in");

            if (incomingSecurityEventList == null) {
                LOG.warning("Security processing failed (actions mismatch)");
                XMLSecurityException ex =
                    new XMLSecurityException("empty", new Object[] {"The request was not signed or encrypted"});
                throwFault(ex.getMessage(), ex);
            }

            if (signatureRequired) {
                Event requiredEvent = SecurityEventConstants.SignatureValue;
                if (!isEventInResults(requiredEvent, incomingSecurityEventList)) {
                    LOG.warning("The request was not signed");
                    XMLSecurityException ex =
                        new XMLSecurityException("empty", new Object[] {"The request was not signed"});
                    throwFault(ex.getMessage(), ex);
                }
            }

            if (encryptionRequired) {
                boolean foundEncryptionPart =
                    isEventInResults(SecurityEventConstants.EncryptedElement, incomingSecurityEventList);
                if (!foundEncryptionPart) {
                    LOG.warning("The request was not encrypted");
                    XMLSecurityException ex =
                        new XMLSecurityException("empty", new Object[] {"The request was not encrypted"});
                    throwFault(ex.getMessage(), ex);
                }
            }

        }

        private boolean isEventInResults(Event event, List<SecurityEvent> incomingSecurityEventList) {
            for (SecurityEvent incomingEvent : incomingSecurityEventList) {
                if (event == incomingEvent.getSecurityEventType()) {
                    return true;
                }
            }
            return false;
        }

        protected void throwFault(String error, Exception ex) {
            LOG.warning(error);
            Response response = JAXRSUtils.toResponseBuilder(400).entity(error).build();
            throw ExceptionUtils.toBadRequestException(null, response);
        }


    }

}
