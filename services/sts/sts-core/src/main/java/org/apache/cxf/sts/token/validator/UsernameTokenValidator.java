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
package org.apache.cxf.sts.token.validator;

import java.security.Principal;
import java.util.Base64;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.realm.UsernameTokenRealmCodec;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.bsp.BSPEnforcer;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;

/**
 * This class validates a wsse UsernameToken.
 */
public class UsernameTokenValidator implements TokenValidator {

    private static final Logger LOG = LogUtils.getL7dLogger(UsernameTokenValidator.class);

    private Validator validator = new org.apache.wss4j.dom.validate.UsernameTokenValidator();

    private UsernameTokenRealmCodec usernameTokenRealmCodec;
    private SubjectRoleParser roleParser = new DefaultSubjectRoleParser();

    /**
     * Set the WSS4J Validator instance to use to validate the token.
     * @param validator the WSS4J Validator instance to use to validate the token
     */
    public void setValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * Set the UsernameTokenRealmCodec instance to use to return a realm from a validated token
     * @param usernameTokenRealmCodec the UsernameTokenRealmCodec instance to use to return a
     *                                realm from a validated token
     */
    public void setUsernameTokenRealmCodec(UsernameTokenRealmCodec usernameTokenRealmCodec) {
        this.usernameTokenRealmCodec = usernameTokenRealmCodec;
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        return validateTarget.getToken() instanceof UsernameTokenType;
    }

    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOG.fine("Validating UsernameToken");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

        RequestData requestData = new RequestData();
        requestData.setSigVerCrypto(sigCrypto);
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);
        requestData.setCallbackHandler(callbackHandler);
        requestData.setMsgContext(tokenParameters.getMessageContext());

        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);

        if (!validateTarget.isUsernameToken()) {
            return response;
        }

        //
        // Turn the JAXB UsernameTokenType into a DOM Element for validation
        //
        UsernameTokenType usernameTokenType = (UsernameTokenType)validateTarget.getToken();

        // Marshall the received JAXB object into a DOM Element
        final Element usernameTokenElement;
        try {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(ObjectFactory.class);
            classes.add(org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);

            CachedContextAndSchemas cache =
                JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
            JAXBContext jaxbContext = cache.getContext();

            Marshaller marshaller = jaxbContext.createMarshaller();
            Document doc = DOMUtils.getEmptyDocument();
            Element rootElement = doc.createElement("root-element");
            JAXBElement<UsernameTokenType> tokenType =
                new JAXBElement<UsernameTokenType>(
                    QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType
                );
            marshaller.marshal(tokenType, rootElement);
            usernameTokenElement = (Element)rootElement.getFirstChild();
        } catch (JAXBException ex) {
            LOG.log(Level.WARNING, "", ex);
            return response;
        }

        //
        // Validate the token
        //
        try {
            boolean allowNamespaceQualifiedPasswordTypes =
                requestData.isAllowNamespaceQualifiedPasswordTypes();
            UsernameToken ut =
                new UsernameToken(usernameTokenElement, allowNamespaceQualifiedPasswordTypes,
                                  new BSPEnforcer());
            // The parsed principal is set independent whether validation is successful or not
            response.setPrincipal(new CustomTokenPrincipal(ut.getName()));
            if (ut.getPassword() == null) {
                return response;
            }

            // See if the UsernameToken is stored in the cache
            int hash = ut.hashCode();
            SecurityToken secToken = null;
            if (tokenParameters.getTokenStore() != null) {
                secToken = tokenParameters.getTokenStore().getToken(Integer.toString(hash));
                if (secToken != null && (secToken.getTokenHash() != hash || secToken.isExpired())) {
                    secToken = null;
                }
            }

            Principal principal = null;
            if (secToken == null) {
                Credential credential = new Credential();
                credential.setUsernametoken(ut);
                credential = validator.validate(credential, requestData);
                principal = credential.getPrincipal();
                if (credential.getSubject() != null && roleParser != null) {
                    // Parse roles from the validated token
                    Set<Principal> roles =
                        roleParser.parseRolesFromSubject(principal, credential.getSubject());
                    response.setRoles(roles);
                }
            }

            if (principal == null) {
                principal =
                    createPrincipal(
                        ut.getName(), ut.getPassword(), ut.getPasswordType(), ut.getNonce(), ut.getCreated()
                    );
            }

            // Get the realm of the UsernameToken
            String tokenRealm = null;
            if (usernameTokenRealmCodec != null) {
                tokenRealm = usernameTokenRealmCodec.getRealmFromToken(ut);
                // verify the realm against the cached token
                if (secToken != null) {
                    Map<String, Object> props = secToken.getProperties();
                    if (props != null) {
                        String cachedRealm = (String)props.get(STSConstants.TOKEN_REALM);
                        if (!tokenRealm.equals(cachedRealm)) {
                            return response;
                        }
                    }
                }
            }

            // Store the successfully validated token in the cache
            if (tokenParameters.getTokenStore() != null && secToken == null) {
                secToken = new SecurityToken(ut.getID());
                secToken.setToken(ut.getElement());
                int hashCode = ut.hashCode();
                String identifier = Integer.toString(hashCode);
                secToken.setTokenHash(hashCode);
                tokenParameters.getTokenStore().add(identifier, secToken);
            }

            response.setPrincipal(principal);
            response.setTokenRealm(tokenRealm);
            validateTarget.setState(STATE.VALID);
            LOG.fine("Username Token successfully validated");
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "", ex);
        }

        return response;
    }

    /**
     * Create a principal based on the authenticated UsernameToken.
     * @throws Base64DecodingException
     */
    private Principal createPrincipal(
        String username,
        String passwordValue,
        String passwordType,
        String nonce,
        String createdTime
    ) {
        boolean hashed = false;
        if (WSS4JConstants.PASSWORD_DIGEST.equals(passwordType)) {
            hashed = true;
        }
        WSUsernameTokenPrincipalImpl principal = new WSUsernameTokenPrincipalImpl(username, hashed);
        if (nonce != null) {
            principal.setNonce(Base64.getMimeDecoder().decode(nonce));
        }
        principal.setPassword(passwordValue);
        principal.setCreatedTime(createdTime);
        principal.setPasswordType(passwordType);
        return principal;
    }

    public SubjectRoleParser getRoleParser() {
        return roleParser;
    }

    public void setRoleParser(SubjectRoleParser roleParser) {
        this.roleParser = roleParser;
    }

}
