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
package org.apache.cxf.rs.security.saml.sso;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.logging.Level;

import javax.security.auth.DestroyFailedException;
import javax.security.auth.callback.CallbackHandler;

import org.w3c.dom.Element;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.saml.DeflateEncoderDecoder;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.xml.security.algorithms.JCEMapper;
import org.opensaml.saml.saml2.core.AuthnRequest;

public class SamlRedirectBindingFilter extends AbstractServiceProviderFilter {

    @Override
    public void filter(ContainerRequestContext context) {
        Message m = JAXRSUtils.getCurrentMessage();
        if (checkSecurityContext(m)) {
            return;
        }
        try {
            SamlRequestInfo info = createSamlRequestInfo(m);
            String urlEncodedRequest =
                URLEncoder.encode(info.getSamlRequest(), StandardCharsets.UTF_8.name());

            UriBuilder ub = UriBuilder.fromUri(getIdpServiceAddress());

            ub.queryParam(SSOConstants.SAML_REQUEST, urlEncodedRequest);
            ub.queryParam(SSOConstants.RELAY_STATE, info.getRelayState());
            if (isSignRequest()) {
                signRequest(urlEncodedRequest, info.getRelayState(), ub);
            }

            String contextCookie = createCookie(SSOConstants.RELAY_STATE,
                                                info.getRelayState(),
                                                info.getWebAppContext(),
                                                info.getWebAppDomain());

            context.abortWith(Response.seeOther(ub.build())
                           .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store")
                           .header("Pragma", "no-cache")
                           .header(HttpHeaders.SET_COOKIE, contextCookie)
                           .build());
        } catch (Exception ex) {
            LOG.log(Level.FINE, ex.getMessage(), ex);
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    protected void signAuthnRequest(AuthnRequest authnRequest) throws Exception {
        // Do nothing as we sign the request in a different way for the redirect binding
    }

    protected String encodeAuthnRequest(Element authnRequest) throws IOException {
        String requestMessage = DOM2Writer.nodeToString(authnRequest);

        DeflateEncoderDecoder encoder = new DeflateEncoderDecoder();
        byte[] deflatedBytes = encoder.deflateToken(requestMessage.getBytes(StandardCharsets.UTF_8));

        return Base64Utility.encode(deflatedBytes);
    }

    /**
     * Sign a request according to the redirect binding spec for Web SSO
     */
    private void signRequest(
        String authnRequest,
        String relayState,
        UriBuilder ub
    ) throws Exception {
        Crypto crypto = getSignatureCrypto();
        if (crypto == null) {
            LOG.warning("No crypto instance of properties file configured for signature");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        String signatureUser = getSignatureUsername();
        if (signatureUser == null) {
            LOG.warning("No user configured for signature");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }
        CallbackHandler callbackHandler = getCallbackHandler();
        if (callbackHandler == null) {
            LOG.warning("No CallbackHandler configured to supply a password for signature");
            throw ExceptionUtils.toInternalServerErrorException(null, null);
        }

        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(signatureUser);
        X509Certificate[] issuerCerts = crypto.getX509Certificates(cryptoType);
        if (issuerCerts == null) {
            throw new Exception(
                "No issuer certs were found to sign the request using name: " + signatureUser
            );
        }

        String sigAlgo = getSignatureAlgorithm();
        String pubKeyAlgo = issuerCerts[0].getPublicKey().getAlgorithm();
        LOG.fine("automatic sig algo detection: " + pubKeyAlgo);
        if ("DSA".equalsIgnoreCase(pubKeyAlgo)) {
            sigAlgo = SSOConstants.DSA_SHA1;
        }

        LOG.fine("Using Signature algorithm " + sigAlgo);
        ub.queryParam(SSOConstants.SIG_ALG, URLEncoder.encode(sigAlgo, StandardCharsets.UTF_8.name()));

        // Get the password
        WSPasswordCallback[] cb = {new WSPasswordCallback(signatureUser, WSPasswordCallback.SIGNATURE)};
        callbackHandler.handle(cb);
        String password = cb[0].getPassword();

        // Get the private key
        PrivateKey privateKey = crypto.getPrivateKey(signatureUser, password);

        // Sign the request
        String jceSigAlgo = JCEMapper.translateURItoJCEID(sigAlgo);
        Signature signature = Signature.getInstance(jceSigAlgo);
        signature.initSign(privateKey);

        String requestToSign =
            SSOConstants.SAML_REQUEST + "=" + authnRequest + "&"
            + SSOConstants.RELAY_STATE + "=" + relayState + "&"
            + SSOConstants.SIG_ALG + "=" + URLEncoder.encode(sigAlgo, StandardCharsets.UTF_8.name());

        signature.update(requestToSign.getBytes(StandardCharsets.UTF_8));
        byte[] signBytes = signature.sign();

        String encodedSignature = Base64.getEncoder().encodeToString(signBytes);

        // Clean the private key from memory when we're done
        try {
            privateKey.destroy();
        } catch (DestroyFailedException ex) {
            // ignore
        }

        ub.queryParam(SSOConstants.SIGNATURE, URLEncoder.encode(encodedSignature, StandardCharsets.UTF_8.name()));

    }

}
