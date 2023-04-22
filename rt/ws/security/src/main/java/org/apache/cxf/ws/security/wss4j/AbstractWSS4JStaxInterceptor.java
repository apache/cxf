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

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptor;
import org.apache.cxf.rt.security.utils.SecurityUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.wss4j.common.ConfigurationConstants;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.crypto.JasyptPasswordEncryptor;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.util.Loader;
import org.apache.wss4j.stax.ext.WSSConstants;
import org.apache.wss4j.stax.ext.WSSSecurityProperties;
import org.apache.wss4j.stax.setup.ConfigurationConverter;
import org.apache.xml.security.stax.ext.XMLSecurityConstants;

public abstract class AbstractWSS4JStaxInterceptor implements SoapInterceptor,
    PhaseInterceptor<SoapMessage> {

    private static final Logger LOG = LogUtils.getL7dLogger(AbstractWSS4JStaxInterceptor.class);
    private static final Set<QName> HEADERS = new HashSet<>();

    static {
        HEADERS.add(new QName(WSSConstants.NS_WSSE10, "Security"));
        HEADERS.add(new QName(XMLSecurityConstants.NS_XMLENC, "EncryptedData"));
        HEADERS.add(new QName(WSSConstants.NS_WSSE11, "EncryptedHeader"));
    }

    private final Map<String, Object> properties;
    private final WSSSecurityProperties userSecurityProperties;
    private Map<String, Crypto> cryptos = new ConcurrentHashMap<>();
    private final Set<String> before = new HashSet<>();
    private final Set<String> after = new HashSet<>();
    private String phase;
    private String id;

    public AbstractWSS4JStaxInterceptor(WSSSecurityProperties securityProperties) {
        super();
        id = getClass().getName();
        userSecurityProperties = securityProperties;
        properties = null;
    }

    public AbstractWSS4JStaxInterceptor(Map<String, Object> properties) {
        super();
        id = getClass().getName();
        this.properties = properties;
        userSecurityProperties = null;
    }

    public AbstractWSS4JStaxInterceptor() {
        super();
        id = getClass().getName();
        userSecurityProperties = null;
        properties = null;
    }

    protected WSSSecurityProperties createSecurityProperties() {
        if (userSecurityProperties != null) {
            return new WSSSecurityProperties(userSecurityProperties);
        }
        WSSSecurityProperties securityProperties = new WSSSecurityProperties();
        ConfigurationConverter.parseActions(properties, securityProperties);
        ConfigurationConverter.parseUserProperties(properties, securityProperties);
        ConfigurationConverter.parseCallback(properties, securityProperties);
        ConfigurationConverter.parseBooleanProperties(properties, securityProperties);
        ConfigurationConverter.parseNonBooleanProperties(properties, securityProperties);
        securityProperties.setSkipDocumentEvents(true);
        return securityProperties;
    }

    protected void translateProperties(SoapMessage msg, WSSSecurityProperties securityProperties) {
        String bspCompliant = (String)msg.getContextualProperty(SecurityConstants.IS_BSP_COMPLIANT);
        if (bspCompliant != null) {
            securityProperties.setDisableBSPEnforcement(!Boolean.valueOf(bspCompliant));
        }

        String futureTTL =
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_FUTURE_TTL);
        if (futureTTL != null) {
            securityProperties.setTimeStampFutureTTL(Integer.parseInt(futureTTL));
        }

        String ttl =
            (String)msg.getContextualProperty(SecurityConstants.TIMESTAMP_TTL);
        if (ttl != null) {
            securityProperties.setTimestampTTL(Integer.parseInt(ttl));
        }

        String utFutureTTL =
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_FUTURE_TTL);
        if (utFutureTTL != null) {
            securityProperties.setUtFutureTTL(Integer.parseInt(utFutureTTL));
        }

        String utTTL =
            (String)msg.getContextualProperty(SecurityConstants.USERNAMETOKEN_TTL);
        if (utTTL != null) {
            securityProperties.setUtTTL(Integer.parseInt(utTTL));
        }

        String certConstraints =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SUBJECT_CERT_CONSTRAINTS, msg);
        if (certConstraints != null && !"".equals(certConstraints)) {
            String certConstraintsSeparator =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.CERT_CONSTRAINTS_SEPARATOR, msg);
            if (certConstraintsSeparator == null || certConstraintsSeparator.isEmpty()) {
                certConstraintsSeparator = ",";
            }
            securityProperties.setSubjectCertConstraints(
                convertCertConstraints(certConstraints, certConstraintsSeparator));
        }

        // Now set SAML SenderVouches + Holder Of Key requirements
        String validateSAMLSubjectConf =
            (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.VALIDATE_SAML_SUBJECT_CONFIRMATION,
                                                           msg);
        if (validateSAMLSubjectConf != null) {
            securityProperties.setValidateSamlSubjectConfirmation(Boolean.valueOf(validateSAMLSubjectConf));
        }

        String actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
        if (actor != null) {
            securityProperties.setActor(actor);
        }

        boolean mustUnderstand =
            MessageUtils.getContextualBoolean(msg, SecurityConstants.MUST_UNDERSTAND, true);
        securityProperties.setMustUnderstand(mustUnderstand);

        boolean validateSchemas =
            MessageUtils.getContextualBoolean(msg, "schema-validation-enabled", false);
        securityProperties.setDisableSchemaValidation(!validateSchemas);

        securityProperties.setSoap12(WSSConstants.NS_SOAP12.equals(msg.getVersion().getNamespace()));
    }

    private Collection<Pattern> convertCertConstraints(String certConstraints, String separator) {
        String[] certConstraintsList = certConstraints.split(separator);
        if (certConstraintsList.length > 0) {
            Collection<Pattern> subjectCertConstraints = new ArrayList<>(certConstraintsList.length);
            for (String certConstraint : certConstraintsList) {
                try {
                    subjectCertConstraints.add(Pattern.compile(certConstraint.trim()));
                } catch (PatternSyntaxException ex) {
                    LOG.log(Level.SEVERE, ex.getMessage(), ex);
                }
            }
            return subjectCertConstraints;
        }

        return null;
    }

    protected void configureCallbackHandler(
        SoapMessage soapMessage, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Object o = SecurityUtils.getSecurityPropertyValue(SecurityConstants.CALLBACK_HANDLER, soapMessage);
        CallbackHandler callbackHandler;
        try {
            callbackHandler = SecurityUtils.getCallbackHandler(o);
        } catch (Exception ex) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
        }

        if (callbackHandler != null) {
            EndpointInfo info = soapMessage.getExchange().getEndpoint().getEndpointInfo();
            synchronized (info) {
                info.setProperty(SecurityConstants.CALLBACK_HANDLER, callbackHandler);
            }
            soapMessage.getExchange().getEndpoint().put(SecurityConstants.CALLBACK_HANDLER,
                                                              callbackHandler);
            soapMessage.getExchange().put(SecurityConstants.CALLBACK_HANDLER, callbackHandler);
        }


        // If we have a "password" but no CallbackHandler then construct one
        if (callbackHandler == null) {
            final boolean outbound = MessageUtils.isOutbound(soapMessage);
            final String password = getPassword(soapMessage);
            final String signatureUser =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_USERNAME, soapMessage);
            final String signaturePassword =
                (String)SecurityUtils.getSecurityPropertyValue(SecurityConstants.SIGNATURE_PASSWORD, soapMessage);

            if (!(StringUtils.isEmpty(password) && StringUtils.isEmpty(signaturePassword))) {
                callbackHandler = new CallbackHandler() {

                    @Override
                    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                        for (Callback callback : callbacks) {
                            if (callback instanceof WSPasswordCallback) {
                                WSPasswordCallback wsPasswordCallback = (WSPasswordCallback)callback;

                                if (signaturePassword != null && wsPasswordCallback.getIdentifier() != null
                                    && wsPasswordCallback.getIdentifier().equals(signatureUser)
                                    && (outbound && wsPasswordCallback.getUsage() == WSPasswordCallback.SIGNATURE)
                                        || (!outbound && wsPasswordCallback.getUsage() == WSPasswordCallback.DECRYPT)) {
                                    wsPasswordCallback.setPassword(signaturePassword);
                                } else if (password != null) {
                                    wsPasswordCallback.setPassword(password);
                                }
                            }
                        }
                    }
                };
            }
        }

        if (callbackHandler != null) {
            securityProperties.setCallbackHandler(callbackHandler);
        }
    }

    protected String getPassword(Object msgContext) {
        String password = (String)((Message)msgContext).getContextualProperty("password");
        if (password == null) {
            password = (String)((Message)msgContext).getContextualProperty(SecurityConstants.PASSWORD);
        }
        return password;
    }

    public Set<URI> getRoles() {
        return null;
    }

    public void handleFault(SoapMessage message) {
    }

    public void postHandleMessage(SoapMessage message) throws Fault {
    }
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public Object getOption(String key) {
        if (properties != null) {
            return properties.get(key);
        }
        return null;
    }

    public Object getProperty(Object msgContext, String key) {
        Object obj = SecurityUtils.getSecurityPropertyValue(key, (Message)msgContext);
        if (obj == null) {
            obj = getOption(key);
        }
        return obj;
    }

    public void setPassword(Object msgContext, String password) {
        ((Message)msgContext).put("password", password);
    }

    public void setProperty(Object msgContext, String key, Object value) {
        ((Message)msgContext).put(key, value);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<QName> getUnderstoodHeaders() {
        return HEADERS;
    }

    public Map<String, Object> getProperties() {
        if (properties != null) {
            return properties;
        }
        return Collections.emptyMap();
    }

    public Set<String> getAfter() {
        return after;
    }

    public Set<String> getBefore() {
        return before;
    }

    protected boolean isRequestor(SoapMessage message) {
        return MessageUtils.isRequestor(message);
    }

    /**
     * Load a Crypto instance. Firstly, it tries to use the cryptoPropertyRefId tag to retrieve
     * a Crypto object via a custom reference Id. Failing this, it tries to load the crypto
     * instance via the cryptoPropertyFile tag.
     */
    protected Crypto loadCrypto(
        SoapMessage soapMessage,
        String cryptoPropertyFile,
        String cryptoPropertyRefId,
        WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        Crypto crypto = null;

        //
        // Try the Property Ref Id first
        //
        String refId = (String)getProperty(soapMessage, cryptoPropertyRefId);
        if (refId != null) {
            crypto = cryptos.get(refId);
            if (crypto == null) {
                Object obj = getProperty(soapMessage, refId);
                if (obj instanceof Properties) {
                    crypto = CryptoFactory.getInstance((Properties)obj,
                                                       getClassLoader(),
                                                       getPasswordEncryptor(soapMessage, securityProperties));
                    cryptos.put(refId, crypto);
                } else if (obj instanceof Crypto) {
                    crypto = (Crypto)obj;
                    cryptos.put(refId, crypto);
                }
            }
            if (crypto == null && LOG.isLoggable(Level.INFO)) {
                LOG.info("The Crypto reference " + refId + " specified by "
                    + cryptoPropertyRefId + " could not be loaded"
                );
            }
        }

        //
        // Now try loading the properties file
        //
        if (crypto == null) {
            String propFile = (String)getProperty(soapMessage, cryptoPropertyFile);
            if (propFile != null) {
                crypto = cryptos.get(propFile);
                if (crypto == null) {
                    crypto = loadCryptoFromPropertiesFile(soapMessage, propFile, securityProperties);
                    cryptos.put(propFile, crypto);
                }
                if (crypto == null && LOG.isLoggable(Level.INFO)) {
                    LOG.info(
                         "The Crypto properties file " + propFile + " specified by "
                         + cryptoPropertyFile + " could not be loaded or found"
                    );
                }
            }
        }

        return crypto;
    }

    protected Crypto loadCryptoFromPropertiesFile(
        SoapMessage soapMessage, String propFilename, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(soapMessage, securityProperties);
        return
            WSS4JUtils.loadCryptoFromPropertiesFile(
                soapMessage, propFilename, getClassLoader(), passwordEncryptor
            );
    }

    protected PasswordEncryptor getPasswordEncryptor(
        SoapMessage soapMessage, WSSSecurityProperties securityProperties
    ) {
        PasswordEncryptor passwordEncryptor =
            (PasswordEncryptor)soapMessage.getContextualProperty(
                SecurityConstants.PASSWORD_ENCRYPTOR_INSTANCE
            );
        if (passwordEncryptor != null) {
            return passwordEncryptor;
        }

        CallbackHandler callbackHandler = securityProperties.getCallbackHandler();
        if (callbackHandler == null) {
            callbackHandler = (CallbackHandler)getProperties().get(ConfigurationConstants.PW_CALLBACK_REF);
        }

        if (callbackHandler != null) {
            return new JasyptPasswordEncryptor(callbackHandler);
        }

        return null;
    }

    protected Crypto getEncryptionCrypto(
            Object e, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(message, securityProperties);
        return WSS4JUtils.getEncryptionCrypto(e, message, passwordEncryptor);
    }

    protected Crypto getSignatureCrypto(
        Object s, SoapMessage message, WSSSecurityProperties securityProperties
    ) throws WSSecurityException {
        PasswordEncryptor passwordEncryptor = getPasswordEncryptor(message, securityProperties);
        return WSS4JUtils.getSignatureCrypto(s, message, passwordEncryptor);
    }

    private ClassLoader getClassLoader() {
        try {
            return Loader.getTCL();
        } catch (Exception ex) {
            return null;
        }
    }
}
