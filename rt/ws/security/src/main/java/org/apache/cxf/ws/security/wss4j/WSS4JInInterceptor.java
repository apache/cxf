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
import java.security.Principal;
import java.security.Provider;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.saaj.SAAJInInterceptor;
import org.apache.cxf.binding.soap.saaj.SAAJUtils;
import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.DefaultSecurityContext;
import org.apache.cxf.interceptor.security.RolePrefixSecurityContextImpl;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.rt.security.saml.SAMLSecurityContext;
import org.apache.cxf.rt.security.saml.SAMLUtils;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.wss4j.common.cache.ReplayCache;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.ThreadLocalSecurityProvider;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.WSSecurityEngine;
import org.apache.wss4j.dom.WSSecurityEngineResult;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.apache.wss4j.dom.handler.WSHandlerResult;
import org.apache.wss4j.dom.message.token.KerberosSecurity;
import org.apache.wss4j.dom.processor.Processor;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.apache.wss4j.dom.validate.NoOpValidator;
import org.apache.wss4j.dom.validate.Validator;

/**
 * Performs WS-Security inbound actions.
 */
public class WSS4JInInterceptor extends AbstractWSS4JInterceptor {

    /**
     * This configuration tag specifies the default attribute name where the roles are present
     * The default is "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role".
     */
    public static final String SAML_ROLE_ATTRIBUTENAME_DEFAULT =
        "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";
    
    public static final String TIMESTAMP_RESULT = "wss4j.timestamp.result";
    public static final String SIGNATURE_RESULT = "wss4j.signature.result";
    public static final String PRINCIPAL_RESULT = "wss4j.principal.result";
    public static final String PROCESSOR_MAP = "wss4j.processor.map";
    public static final String VALIDATOR_MAP = "wss4j.validator.map";

    public static final String SECURITY_PROCESSED = WSS4JInInterceptor.class.getName() + ".DONE";
    
    private static final Logger LOG = LogUtils.getL7dLogger(WSS4JInInterceptor.class);
    private boolean ignoreActions;

    /**
     *
     */
    private WSSecurityEngine secEngineOverride;
    
    public WSS4JInInterceptor() {
        super();

        setPhase(Phase.PRE_PROTOCOL);
        getAfter().add(SAAJInInterceptor.class.getName());
        getAfter().add("org.apache.cxf.ws.addressing.soap.MAPCodec");
    }
    public WSS4JInInterceptor(boolean ignore) {
        this();
        ignoreActions = ignore;
    }

    public WSS4JInInterceptor(Map<String, Object> properties) {
        this();
        setProperties(properties);
        final Map<QName, Object> processorMap = CastUtils.cast(
            (Map<?, ?>)properties.get(PROCESSOR_MAP));
        final Map<QName, Object> validatorMap = CastUtils.cast(
            (Map<?, ?>)properties.get(VALIDATOR_MAP));
        
        if (processorMap != null) {
            if (validatorMap != null) {
                processorMap.putAll(validatorMap);
            }
            secEngineOverride = createSecurityEngine(processorMap);
        } else if (validatorMap != null) {
            secEngineOverride = createSecurityEngine(validatorMap);
        }
    }
    
    public void setIgnoreActions(boolean i) {
        ignoreActions = i;
    }
    private SOAPMessage getSOAPMessage(SoapMessage msg) {
        SAAJInInterceptor.INSTANCE.handleMessage(msg);
        return msg.getContent(SOAPMessage.class);
    }
    
    @Override
    public Object getProperty(Object msgContext, String key) {
        // use the superclass first
        Object result = super.getProperty(msgContext, key);
        
        // handle the special case of the SEND_SIGV
        if (result == null 
            && WSHandlerConstants.SEND_SIGV.equals(key)
            && this.isRequestor((SoapMessage)msgContext)) {
            result = ((SoapMessage)msgContext).getExchange().getOutMessage().get(key);
        }               
        return result;
    }
    public final boolean isGET(SoapMessage message) {
        String method = (String)message.get(SoapMessage.HTTP_REQUEST_METHOD);
        boolean isGet = 
            "GET".equals(method) && message.getContent(XMLStreamReader.class) == null;
        return isGet;
    }
    
    public void handleMessage(SoapMessage msg) throws Fault {
        if (msg.containsKey(SECURITY_PROCESSED) || isGET(msg)) {
            return;
        }
        
        Object provider = msg.getExchange().get(Provider.class);
        final boolean useCustomProvider = provider != null && ThreadLocalSecurityProvider.isInstalled();
        try {
            if (useCustomProvider) {
                ThreadLocalSecurityProvider.setProvider((Provider)provider);
            }
            handleMessageInternal(msg);
        } finally {
            if (useCustomProvider) {
                ThreadLocalSecurityProvider.unsetProvider();
            }
        }
    }
    
    private void handleMessageInternal(SoapMessage msg) throws Fault {
        boolean utWithCallbacks = 
            MessageUtils.getContextualBoolean(msg, SecurityConstants.VALIDATE_TOKEN, true);
        translateProperties(msg);
        
        RequestData reqData = new CXFRequestData();

        WSSConfig config = (WSSConfig)msg.getContextualProperty(WSSConfig.class.getName()); 
        WSSecurityEngine engine;
        if (config != null) {
            engine = new WSSecurityEngine();
            engine.setWssConfig(config);
        } else {
            engine = getSecurityEngine(utWithCallbacks);
            if (engine == null) {
                engine = new WSSecurityEngine();
            }
            config = engine.getWssConfig();
        }
        reqData.setWssConfig(config);
        
        // Add Audience Restrictions for SAML
        configureAudienceRestriction(msg, reqData);
                
        SOAPMessage doc = getSOAPMessage(msg);
        
        boolean doDebug = LOG.isLoggable(Level.FINE);

        SoapVersion version = msg.getVersion();
        if (doDebug) {
            LOG.fine("WSS4JInInterceptor: enter handleMessage()");
        }

        /*
         * The overall try, just to have a finally at the end to perform some
         * housekeeping.
         */
        try {
            reqData.setMsgContext(msg);
            reqData.setAttachmentCallbackHandler(new AttachmentCallbackHandler(msg));
            
            setAlgorithmSuites(msg, reqData);
            
            reqData.setCallbackHandler(getCallback(reqData, utWithCallbacks));
            
            computeAction(msg, reqData);
            String action = getAction(msg, version);

            List<Integer> actions = WSSecurityUtil.decodeAction(action);

            String actor = (String)getOption(WSHandlerConstants.ACTOR);
            if (actor == null) {
                actor = (String)msg.getContextualProperty(SecurityConstants.ACTOR);
            }

            // Configure replay caching
            configureReplayCaches(reqData, actions, msg);
            
            TLSSessionInfo tlsInfo = msg.get(TLSSessionInfo.class);
            if (tlsInfo != null) {
                Certificate[] tlsCerts = tlsInfo.getPeerCertificates();
                reqData.setTlsCerts(tlsCerts);
            }

            /*
             * Get and check the Signature specific parameters first because
             * they may be used for encryption too.
             */
            doReceiverAction(actions, reqData);
            
            /*get chance to check msg context enableRevocation setting
             *when use policy based ws-security where the WSHandler configuration
             *isn't available
             */
            boolean enableRevocation = reqData.isRevocationEnabled() 
                || MessageUtils.isTrue(msg.getContextualProperty(SecurityConstants.ENABLE_REVOCATION));
            reqData.setEnableRevocation(enableRevocation);
            
            Element elem = WSSecurityUtil.getSecurityHeader(doc.getSOAPPart(), actor);

            List<WSSecurityEngineResult> wsResult = engine.processSecurityHeader(
                elem, reqData
            );
            
            if (wsResult != null && !wsResult.isEmpty()) { // security header found
                if (reqData.getWssConfig().isEnableSignatureConfirmation()) {
                    checkSignatureConfirmation(reqData, wsResult);
                }

                storeSignature(msg, reqData, wsResult);
                storeTimestamp(msg, reqData, wsResult);
                checkActions(msg, reqData, wsResult, actions, SAAJUtils.getBody(doc));
                doResults(
                    msg, actor, 
                    SAAJUtils.getHeader(doc),
                    SAAJUtils.getBody(doc),
                    wsResult, utWithCallbacks
                );
            } else { // no security header found
                // Create an empty result list to pass into the required validation
                // methods.
                wsResult = new ArrayList<WSSecurityEngineResult>();
                if (doc.getSOAPPart().getEnvelope().getBody().hasFault() && isRequestor(msg)) {
                    LOG.warning("The request is a SOAP Fault, but it is not secured");
                    // We allow lax action matching here for backwards compatibility
                    // with manually configured WSS4JInInterceptors that previously
                    // allowed faults to pass through even if their actions aren't
                    // a strict match against those configured.  In the WS-SP case,
                    // we will want to still call doResults as it handles asserting
                    // certain assertions that do not require a WS-S header such as
                    // a sp:TransportBinding assertion.  In the case of WS-SP,
                    // the unasserted assertions will provide confirmation that
                    // security was not sufficient.
                    // checkActions(msg, reqData, wsResult, actions);
                    doResults(msg, actor, 
                              SAAJUtils.getHeader(doc),
                              SAAJUtils.getBody(doc),
                              wsResult);
                } else {
                    checkActions(msg, reqData, wsResult, actions, SAAJUtils.getBody(doc));
                    doResults(msg, actor,
                              SAAJUtils.getHeader(doc),
                              SAAJUtils.getBody(doc),
                              wsResult);
                }
            }
            advanceBody(msg, SAAJUtils.getBody(doc));
            SAAJInInterceptor.replaceHeaders(doc, msg);

            if (doDebug) {
                LOG.fine("WSS4JInInterceptor: exit handleMessage()");
            }
            msg.put(SECURITY_PROCESSED, Boolean.TRUE);

        } catch (WSSecurityException e) {
            throw WSS4JUtils.createSoapFault(msg, version, e);
        } catch (XMLStreamException e) {
            throw new SoapFault(new Message("STAX_EX", LOG), e, version.getSender());
        } catch (SOAPException e) {
            throw new SoapFault(new Message("SAAJ_EX", LOG), e, version.getSender());
        } finally {
            reqData.clear();
            reqData = null;
        }
    }
    
    private void configureAudienceRestriction(SoapMessage msg, RequestData reqData) {
        // Add Audience Restrictions for SAML
        boolean enableAudienceRestriction = 
            MessageUtils.getContextualBoolean(msg, 
                                              SecurityConstants.AUDIENCE_RESTRICTION_VALIDATION, 
                                              true);
        if (enableAudienceRestriction) {
            List<String> audiences = new ArrayList<String>();
            if (msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL) != null) {
                audiences.add((String)msg.getContextualProperty(org.apache.cxf.message.Message.REQUEST_URL));
            }
            if (msg.getContextualProperty("javax.xml.ws.wsdl.service") != null) {
                audiences.add(msg.getContextualProperty("javax.xml.ws.wsdl.service").toString());
            }
            reqData.setAudienceRestrictions(audiences);
        }
    }

    private void checkActions(
        SoapMessage msg, 
        RequestData reqData, 
        List<WSSecurityEngineResult> wsResult, 
        List<Integer> actions,
        Element body
    ) throws WSSecurityException {
        if (ignoreActions) {
            // Not applicable for the WS-SecurityPolicy case
            return;
        }
        
        // now check the security actions: do they match, in any order?
        if (!checkReceiverResultsAnyOrder(wsResult, actions)) {
            LOG.warning("Security processing failed (actions mismatch)");
            throw new WSSecurityException(WSSecurityException.ErrorCode.INVALID_SECURITY);
        }
        
        // Now check to see if SIGNATURE_PARTS are specified
        String signatureParts = 
            (String)getProperty(msg, WSHandlerConstants.SIGNATURE_PARTS);
        if (signatureParts != null) {
            String warning = "To enforce that particular elements were signed you must either "
                + "use WS-SecurityPolicy, or else use the CryptoCoverageChecker or "
                + "SignatureCoverageChecker";
            LOG.warning(warning);
        }
        
    }
    
    private void storeSignature(
        SoapMessage msg, RequestData reqData, List<WSSecurityEngineResult> wsResult
    ) throws WSSecurityException {
        // Extract the signature action result from the action list
        List<WSSecurityEngineResult> signatureResults = 
            WSSecurityUtil.fetchAllActionResults(wsResult, WSConstants.SIGN);

        // Store the last signature result
        if (!signatureResults.isEmpty()) {
            msg.put(SIGNATURE_RESULT, signatureResults.get(signatureResults.size() - 1));
        }
    }
    
    private void storeTimestamp(
        SoapMessage msg, RequestData reqData, List<WSSecurityEngineResult> wsResult
    ) throws WSSecurityException {
        // Extract the timestamp action result from the action list
        List<WSSecurityEngineResult> timestampResults = 
            WSSecurityUtil.fetchAllActionResults(wsResult, WSConstants.TS);

        if (!timestampResults.isEmpty()) {
            msg.put(TIMESTAMP_RESULT, timestampResults.get(timestampResults.size() - 1));
        }
    }
    
    /**
     * Do whatever is necessary to determine the action for the incoming message and 
     * do whatever other setup work is necessary.
     * 
     * @param msg
     * @param reqData
     */
    protected void computeAction(SoapMessage msg, RequestData reqData) throws WSSecurityException {
        //
        // Try to get Crypto Provider from message context properties. 
        // It gives a possibility to use external Crypto Provider 
        //
        Crypto encCrypto = (Crypto)msg.getContextualProperty(SecurityConstants.ENCRYPT_CRYPTO);
        if (encCrypto != null) {
            reqData.setDecCrypto(encCrypto);
        }
        Crypto sigCrypto = (Crypto)msg.getContextualProperty(SecurityConstants.SIGNATURE_CRYPTO);
        if (sigCrypto != null) {
            reqData.setSigVerCrypto(sigCrypto);
        }
    }
    
    protected void configureReplayCaches(RequestData reqData, List<Integer> actions, SoapMessage msg) 
        throws WSSecurityException {
        reqData.setEnableNonceReplayCache(false);
        if (isNonceCacheRequired(actions, msg)) {
            ReplayCache nonceCache = 
                getReplayCache(
                    msg, SecurityConstants.ENABLE_NONCE_CACHE, SecurityConstants.NONCE_CACHE_INSTANCE
                );
            reqData.setNonceReplayCache(nonceCache);
            if (nonceCache != null) {
                reqData.setEnableNonceReplayCache(true);
            }
        }
        
        reqData.setEnableTimestampReplayCache(false);
        if (isTimestampCacheRequired(actions, msg)) {
            ReplayCache timestampCache = 
                getReplayCache(
                    msg, SecurityConstants.ENABLE_TIMESTAMP_CACHE, SecurityConstants.TIMESTAMP_CACHE_INSTANCE
                );
            reqData.setTimestampReplayCache(timestampCache);
            if (timestampCache != null) {
                reqData.setEnableTimestampReplayCache(true);
            }
        }
        
        reqData.setEnableSamlOneTimeUseReplayCache(false);
        if (isSamlCacheRequired(actions, msg)) {
            ReplayCache samlCache = 
                getReplayCache(
                    msg, SecurityConstants.ENABLE_SAML_ONE_TIME_USE_CACHE, 
                    SecurityConstants.SAML_ONE_TIME_USE_CACHE_INSTANCE
                );
            reqData.setSamlOneTimeUseReplayCache(samlCache);
            if (samlCache != null) {
                reqData.setEnableSamlOneTimeUseReplayCache(true);
            }
        }
    }
    
    /**
     * Is a Nonce Cache required, i.e. are we expecting a UsernameToken 
     */
    protected boolean isNonceCacheRequired(List<Integer> actions, SoapMessage msg) {
        if (actions.contains(WSConstants.UT) || actions.contains(WSConstants.UT_NOPASSWORD)) {
            return true;
        }
        return false;
    }
    
    /**
     * Is a Timestamp cache required, i.e. are we expecting a Timestamp 
     */
    protected boolean isTimestampCacheRequired(List<Integer> actions, SoapMessage msg) {
        if (actions.contains(WSConstants.TS)) {
            return true;
        }
        return false;
    }
    
    /**
     * Is a SAML Cache required, i.e. are we expecting a SAML Token 
     */
    protected boolean isSamlCacheRequired(List<Integer> actions, SoapMessage msg) {
        if (actions.contains(WSConstants.ST_UNSIGNED) || actions.contains(WSConstants.ST_SIGNED)) {
            return true;
        }
        return false;
    }
    
    /**
     * Set a WSS4J AlgorithmSuite object on the RequestData context, to restrict the
     * algorithms that are allowed for encryption, signature, etc.
     */
    protected void setAlgorithmSuites(SoapMessage message, RequestData data) throws WSSecurityException {
        super.decodeAlgorithmSuite(data);
    }

    protected void doResults(
        SoapMessage msg, 
        String actor, 
        Element soapHeader,
        Element soapBody,
        List<WSSecurityEngineResult> wsResult
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        doResults(msg, actor, soapHeader, soapBody, wsResult, false);
    }

    protected void doResults(
        SoapMessage msg, 
        String actor,
        Element soapHeader,
        Element soapBody,
        List<WSSecurityEngineResult> wsResult, 
        boolean utWithCallbacks
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        /*
         * All ok up to this point. Now construct and setup the security result
         * structure. The service may fetch this and check it.
         */
        List<WSHandlerResult> results = CastUtils.cast((List<?>)msg.get(WSHandlerConstants.RECV_RESULTS));
        if (results == null) {
            results = new ArrayList<WSHandlerResult>();
            msg.put(WSHandlerConstants.RECV_RESULTS, results);
        }
        WSHandlerResult rResult = new WSHandlerResult(actor, wsResult);
        results.add(0, rResult);
        
        Boolean allowUnsignedSamlPrincipals = 
                MessageUtils.getContextualBoolean(msg, 
                        SecurityConstants.ENABLE_UNSIGNED_SAML_ASSERTION_PRINCIPAL, false);
        final boolean useJAASSubject = MessageUtils
            .getContextualBoolean(msg, SecurityConstants.SC_FROM_JAAS_SUBJECT, true);

        // Now go through the results in a certain order to set up a security context. Highest priority is first.
        
        List<Integer> resultPriorities = new ArrayList<Integer>();
        resultPriorities.add(WSConstants.ST_SIGNED);
        resultPriorities.add(WSConstants.ST_UNSIGNED);
        resultPriorities.add(WSConstants.UT);
        resultPriorities.add(WSConstants.BST);
        resultPriorities.add(WSConstants.SIGN);
        resultPriorities.add(WSConstants.UT_NOPASSWORD);
        
        for (Integer resultPriority : resultPriorities) {
            if (resultPriority == WSConstants.ST_UNSIGNED && !allowUnsignedSamlPrincipals) {
                continue;
            }
            
            for (WSSecurityEngineResult result : wsResult) {
                Integer action = (Integer)result.get(WSSecurityEngineResult.TAG_ACTION);
                if (action.intValue() == resultPriority.intValue()) {
                    final Object binarySecurity = result.get(WSSecurityEngineResult.TAG_BINARY_SECURITY_TOKEN);
                    PublicKey publickey = 
                        (PublicKey)result.get(WSSecurityEngineResult.TAG_PUBLIC_KEY);
                    X509Certificate cert = 
                        (X509Certificate)result.get(WSSecurityEngineResult.TAG_X509_CERTIFICATE);
    
                    if ((resultPriority == WSConstants.BST && !(binarySecurity instanceof KerberosSecurity))
                        || (resultPriority == WSConstants.SIGN && publickey == null && cert == null)) {
                        continue;
                    }
                    SecurityContext context = 
                        createSecurityContext(msg, useJAASSubject, result, utWithCallbacks);
                    if (context != null) {
                        msg.put(SecurityContext.class, context);
                        return;
                    }
                }
            }
        }
    }
    
    private SecurityContext createSecurityContext(
        SoapMessage msg, boolean useJAASSubject,
        WSSecurityEngineResult wsResult, boolean utWithCallbacks
    ) {
        final Principal p = (Principal)wsResult.get(WSSecurityEngineResult.TAG_PRINCIPAL);
        final Subject subject = (Subject)wsResult.get(WSSecurityEngineResult.TAG_SUBJECT);
        
        return createSecurityContext(msg, subject, p, useJAASSubject, wsResult, utWithCallbacks);
    }
    
    protected SecurityContext createSecurityContext(
        SoapMessage msg, Subject subject, Principal p, boolean useJAASSubject,
        WSSecurityEngineResult wsResult, boolean utWithCallbacks
    ) {
        if (subject != null && !(p instanceof KerberosPrincipal) && useJAASSubject) {
            String roleClassifier = 
                (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER);
            if (roleClassifier != null && !"".equals(roleClassifier)) {
                String roleClassifierType = 
                    (String)msg.getContextualProperty(SecurityConstants.SUBJECT_ROLE_CLASSIFIER_TYPE);
                if (roleClassifierType == null || "".equals(roleClassifierType)) {
                    roleClassifierType = "prefix";
                }
                return new RolePrefixSecurityContextImpl(subject, roleClassifier, roleClassifierType);
            } else {
                return new DefaultSecurityContext(p, subject);
            }
        } else if (p != null) {
            msg.put(PRINCIPAL_RESULT, p);
            if (!utWithCallbacks) {
                WSS4JTokenConverter.convertToken(msg, p);
            }
            Object receivedAssertion = wsResult.get(WSSecurityEngineResult.TAG_TRANSFORMED_TOKEN);
            if (receivedAssertion == null) {
                receivedAssertion = wsResult.get(WSSecurityEngineResult.TAG_SAML_ASSERTION);
            }
            if (wsResult.get(WSSecurityEngineResult.TAG_DELEGATION_CREDENTIAL) != null) {
                msg.put(SecurityConstants.DELEGATED_CREDENTIAL, 
                        wsResult.get(WSSecurityEngineResult.TAG_DELEGATION_CREDENTIAL));
            }
            
            if (receivedAssertion instanceof SamlAssertionWrapper) {
                String roleAttributeName = (String)msg.getContextualProperty(
                        SecurityConstants.SAML_ROLE_ATTRIBUTENAME);
                if (roleAttributeName == null || roleAttributeName.length() == 0) {
                    roleAttributeName = SAML_ROLE_ATTRIBUTENAME_DEFAULT;
                }
                
                ClaimCollection claims = 
                    SAMLUtils.getClaims((SamlAssertionWrapper)receivedAssertion);
                Set<Principal> roles = 
                    SAMLUtils.parseRolesFromClaims(claims, roleAttributeName, null);
                
                SAMLSecurityContext context = 
                    new SAMLSecurityContext(p, roles, claims);
                context.setIssuer(SAMLUtils.getIssuer(receivedAssertion));
                context.setAssertionElement(SAMLUtils.getAssertionElement(receivedAssertion));
                return context;
            } else {
                return createSecurityContext(p);
            }
        }
        
        return null;
    }

    protected void advanceBody(
        SoapMessage msg, Node body
    ) throws SOAPException, XMLStreamException, WSSecurityException {
        XMLStreamReader reader = StaxUtils.createXMLStreamReader(new DOMSource(body));
        // advance just past body
        int evt = reader.next();
        int i = 0;
        while (reader.hasNext() && i < 1
               && (evt != XMLStreamConstants.END_ELEMENT || evt != XMLStreamConstants.START_ELEMENT)) {
            reader.next();
            i++;
        }
        msg.setContent(XMLStreamReader.class, reader);
    }
    
    protected SecurityContext createSecurityContext(final Principal p) {
        return new SecurityContext() {

            public Principal getUserPrincipal() {
                return p;
            }

            public boolean isUserInRole(String arg0) {
                return false;
            }
        };
    }
    
    private String getAction(SoapMessage msg, SoapVersion version) {
        String action = (String)getOption(WSHandlerConstants.ACTION);
        if (action == null) {
            action = (String)msg.get(WSHandlerConstants.ACTION);
        }
        if (action == null) {
            LOG.warning("No security action was defined!");
            throw new SoapFault("No security action was defined!", version.getReceiver());
        }
        return action;
    }
    
    private class TokenStoreCallbackHandler implements CallbackHandler {
        private CallbackHandler internal;
        private TokenStore store;
        public TokenStoreCallbackHandler(CallbackHandler in,
                                         TokenStore st) {
            internal = in;
            store = st;
        }
        
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (int i = 0; i < callbacks.length; i++) {
                if (callbacks[i] instanceof WSPasswordCallback) {
                    WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
                    
                    String id = pc.getIdentifier();
                    SecurityToken tok = store.getToken(id);
                    if (tok != null && !tok.isExpired()) {
                        pc.setKey(tok.getSecret());
                        pc.setCustomToken(tok.getToken());
                        return;
                    }
                }
            }
            if (internal != null) {
                internal.handle(callbacks);
            }
        }
        
    }

    protected CallbackHandler getCallback(RequestData reqData, boolean utWithCallbacks) 
        throws WSSecurityException {
        if (!utWithCallbacks) {
            CallbackHandler pwdCallback = null;
            try {
                pwdCallback = getCallback(reqData);
            } catch (Exception ex) {
                // ignore
            }
            return new DelegatingCallbackHandler(pwdCallback);
        } else {
            return getCallback(reqData);
        }
    }
    
    protected CallbackHandler getCallback(RequestData reqData) throws WSSecurityException {
        Object o = ((SoapMessage)reqData.getMsgContext())
            .getContextualProperty(SecurityConstants.CALLBACK_HANDLER);
        if (o instanceof String) {
            try {
                o = ClassLoaderUtils.loadClass((String)o, this.getClass()).newInstance();
            } catch (Exception e) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, e);
            }
        }         
        CallbackHandler cbHandler = null;
        if (o instanceof CallbackHandler) {
            cbHandler = (CallbackHandler)o;
        }
        if (cbHandler == null) {
            try {
                cbHandler = getPasswordCallbackHandler(reqData);
            } catch (WSSecurityException sec) {
                Endpoint ep = ((SoapMessage)reqData.getMsgContext()).getExchange().getEndpoint();
                if (ep != null && ep.getEndpointInfo() != null) {
                    TokenStore store = 
                        WSS4JUtils.getTokenStore((SoapMessage)reqData.getMsgContext(), false);
                    if (store != null) {
                        return new TokenStoreCallbackHandler(null, store);
                    }
                }                    
                throw sec;
            }
        }
            
        Endpoint ep = ((SoapMessage)reqData.getMsgContext()).getExchange().getEndpoint();
        if (ep != null && ep.getEndpointInfo() != null) {
            TokenStore store = WSS4JUtils.getTokenStore((SoapMessage)reqData.getMsgContext(), false);
            if (store != null) {
                return new TokenStoreCallbackHandler(cbHandler, store);
            }
        }
        return cbHandler;
    }


    
    /**
     * @return      the WSSecurityEngine in use by this interceptor.
     *              This engine is defined to be the secEngineOverride
     *              instance, if defined in this class (and supplied through
     *              construction); otherwise, it is taken to be the default
     *              WSSecEngine instance (currently defined in the WSHandler
     *              base class).
     */
    protected WSSecurityEngine getSecurityEngine(boolean utWithCallbacks) {
        if (secEngineOverride != null) {
            return secEngineOverride;
        }
        
        if (!utWithCallbacks) {
            Map<QName, Object> profiles = new HashMap<QName, Object>(1);
            Validator validator = new NoOpValidator();
            profiles.put(WSSecurityEngine.USERNAME_TOKEN, validator);
            return createSecurityEngine(profiles);
        }
        
        return null;
    }

    /**
     * @return      a freshly minted WSSecurityEngine instance, using the
     *              (non-null) processor map, to be used to initialize the
     *              WSSecurityEngine instance.
     */
    protected static WSSecurityEngine createSecurityEngine(final Map<QName, Object> map) {
        assert map != null;
        final WSSConfig config = WSSConfig.getNewInstance();
        for (Map.Entry<QName, Object> entry : map.entrySet()) {
            final QName key = entry.getKey();
            Object val = entry.getValue();
            if (val instanceof Class<?>) {
                config.setProcessor(key, (Class<?>)val);
            } else if (val instanceof Processor) {
                config.setProcessor(key, (Processor)val);
            } else if (val instanceof Validator) {
                config.setValidator(key, (Validator)val);
            } else if (val == null) {
                config.setProcessor(key, (Class<?>)null);
            }
        }
        final WSSecurityEngine ret = new WSSecurityEngine();
        ret.setWssConfig(config);
        return ret;
    }
    
    /**
     * Get a ReplayCache instance. It first checks to see whether caching has been explicitly 
     * enabled or disabled via the booleanKey argument. If it has been set to false then no
     * replay caching is done (for this booleanKey). If it has not been specified, then caching
     * is enabled only if we are not the initiator of the exchange. If it has been specified, then
     * caching is enabled.
     * 
     * It tries to get an instance of ReplayCache via the instanceKey argument from a 
     * contextual property, and failing that the message exchange. If it can't find any, then it
     * defaults to using an EH-Cache instance and stores that on the message exchange.
     */
    protected ReplayCache getReplayCache(
        SoapMessage message, String booleanKey, String instanceKey
    ) {
        return WSS4JUtils.getReplayCache(message, booleanKey, instanceKey);
    }

    static class CXFRequestData extends RequestData {
        public CXFRequestData() {
        }

        public Validator getValidator(QName qName) throws WSSecurityException {
            String key = null;
            if (WSSecurityEngine.SAML_TOKEN.equals(qName)) {
                key = SecurityConstants.SAML1_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SAML2_TOKEN.equals(qName)) {
                key = SecurityConstants.SAML2_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.USERNAME_TOKEN.equals(qName)) {
                key = SecurityConstants.USERNAME_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SIGNATURE.equals(qName)) {
                key = SecurityConstants.SIGNATURE_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.TIMESTAMP.equals(qName)) {
                key = SecurityConstants.TIMESTAMP_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.BINARY_TOKEN.equals(qName)) {
                key = SecurityConstants.BST_TOKEN_VALIDATOR;
            } else if (WSSecurityEngine.SECURITY_CONTEXT_TOKEN_05_02.equals(qName)
                || WSSecurityEngine.SECURITY_CONTEXT_TOKEN_05_12.equals(qName)) {
                key = SecurityConstants.SCT_TOKEN_VALIDATOR;
            }
            if (key != null) {
                Object o = ((SoapMessage)this.getMsgContext()).getContextualProperty(key);
                try {
                    if (o instanceof Validator) {
                        return (Validator)o;
                    } else if (o instanceof Class) {
                        return (Validator)((Class<?>)o).newInstance();
                    } else if (o instanceof String) {
                        return (Validator)ClassLoaderUtils.loadClass(o.toString(),
                                                                     WSS4JInInterceptor.class)
                                                                     .newInstance();
                    } else if (o != null) {
                        throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, 
                                                      "Cannot load Validator: " + o);
                    }
                } catch (RuntimeException t) {
                    throw t;
                } catch (Exception ex) {
                    throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex);
                }
            }
            return super.getValidator(qName);
        }
    };
    
}
