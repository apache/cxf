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

package org.apache.cxf.systest.sts.custom;

import java.io.IOException;
import java.util.Base64;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;

/**
 * This class validates a processed UsernameToken, extracted from the Credential passed to
 * the validate method.
 */
public class CustomUTValidator implements Validator {

    private static final org.slf4j.Logger LOG =
        org.slf4j.LoggerFactory.getLogger(CustomUTValidator.class);

    /**
     * Validate the credential argument. It must contain a non-null UsernameToken. A
     * CallbackHandler implementation is also required to be set.
     *
     * If the password type is either digest or plaintext, it extracts a password from the
     * CallbackHandler and then compares the passwords appropriately.
     *
     * If the password is null it queries a hook to allow the user to validate UsernameTokens
     * of this type.
     *
     * @param credential the Credential to be validated
     * @param data the RequestData associated with the request
     * @throws WSSecurityException on a failed validation
     */
    public Credential validate(Credential credential, RequestData data) throws WSSecurityException {
        if (credential == null || credential.getUsernametoken() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCredential");
        }

        boolean handleCustomPasswordTypes = data.isHandleCustomPasswordTypes();
        boolean passwordsAreEncoded = data.isEncodePasswords();
        String requiredPasswordType = data.getRequiredPasswordType();

        UsernameToken usernameToken = credential.getUsernametoken();
        usernameToken.setPasswordsAreEncoded(passwordsAreEncoded);

        String pwType = usernameToken.getPasswordType();
        if (LOG.isDebugEnabled()) {
            LOG.debug("UsernameToken user " + usernameToken.getName());
            LOG.debug("UsernameToken password type " + pwType);
        }

        if (requiredPasswordType != null && !requiredPasswordType.equals(pwType)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication failed as the received password type does not "
                    + "match the required password type of: " + requiredPasswordType);
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }

        //
        // If the UsernameToken is hashed or plaintext, then retrieve the password from the
        // callback handler and compare directly. If the UsernameToken is of some unknown type,
        // then delegate authentication to the callback handler
        //
        String password = usernameToken.getPassword();
        if (usernameToken.isHashed()) {
            verifyDigestPassword(usernameToken, data);
        } else if (WSConstants.PASSWORD_TEXT.equals(pwType)
            || password != null && (pwType == null || "".equals(pwType.trim()))) {
            verifyPlaintextPassword(usernameToken, data);
        } else if (password != null) {
            if (!handleCustomPasswordTypes) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Authentication failed as handleCustomUsernameTokenTypes is false");
                }
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
            verifyCustomPassword(usernameToken, data);
        } else {
            verifyUnknownPassword(usernameToken, data);
        }
        return credential;
    }

    /**
     * Verify a UsernameToken containing a password of some unknown (but specified) password
     * type. It does this by querying a CallbackHandler instance to obtain a password for the
     * given username, and then comparing it against the received password.
     * This method currently uses the same LOG.c as the verifyPlaintextPassword case, but it in
     * a separate protected method to allow users to override the validation of the custom
     * password type specific case.
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyCustomPassword(UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        verifyPlaintextPassword(usernameToken, data);
    }

    /**
     * Verify a UsernameToken containing a plaintext password. It does this by querying a
     * CallbackHandler instance to obtain a password for the given username, and then comparing
     * it against the received password.
     * This method currently uses the same LOG.c as the verifyDigestPassword case, but it in
     * a separate protected method to allow users to override the validation of the plaintext
     * password specific case.
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyPlaintextPassword(UsernameToken usernameToken,
                                           RequestData data) throws WSSecurityException {
        verifyDigestPassword(usernameToken, data);
    }

    /**
     * Verify a UsernameToken containing a password digest. It does this by querying a
     * CallbackHandler instance to obtain a password for the given username, and then comparing
     * it against the received password.
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyDigestPassword(UsernameToken usernameToken,
                                        RequestData data) throws WSSecurityException {
        if (data.getCallbackHandler() == null) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, "noCallback");
        }

        String user = usernameToken.getName();
        String password = usernameToken.getPassword();
        String nonce = usernameToken.getNonce();
        String createdTime = usernameToken.getCreated();
        String pwType = usernameToken.getPasswordType();
        boolean passwordsAreEncoded = usernameToken.getPasswordsAreEncoded();

        WSPasswordCallback pwCb =
            new WSPasswordCallback(user, null, pwType, WSPasswordCallback.USERNAME_TOKEN);
        try {
            data.getCallbackHandler().handle(new Callback[]{pwCb});
        } catch (IOException | UnsupportedCallbackException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(e.getMessage(), e);
            }
            throw new WSSecurityException(
                WSSecurityException.ErrorCode.FAILED_AUTHENTICATION, e
            );
        }
        String origPassword = pwCb.getPassword();
        if (origPassword == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Callback supplied no password for: " + user);
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
        if (usernameToken.isHashed()) {
            String passDigest;
            if (passwordsAreEncoded) {
                passDigest = UsernameToken.doPasswordDigest(nonce, createdTime, 
                                                            Base64.getMimeDecoder().decode(origPassword));
            } else {
                passDigest = UsernameToken.doPasswordDigest(nonce, createdTime, origPassword);
            }
            if (!passDigest.equals(password)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
        } else {
            if (!origPassword.equals(password)) {
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
            }
        }
    }

    /**
     * Verify a UsernameToken containing no password. An exception is thrown unless the user
     * has explicitly allowed this use-case via WSHandlerConstants.ALLOW_USERNAMETOKEN_NOPASSWORD
     * @param usernameToken The UsernameToken instance to verify
     * @throws WSSecurityException on a failed authentication.
     */
    protected void verifyUnknownPassword(UsernameToken usernameToken,
                                         RequestData data) throws WSSecurityException {

        boolean allowUsernameTokenDerivedKeys = data.isAllowUsernameTokenNoPassword();
        if (!allowUsernameTokenDerivedKeys) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Authentication failed as the received UsernameToken does not "
                    + "contain any password element");
            }
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

}
