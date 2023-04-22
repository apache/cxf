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
package org.apache.cxf.rs.security.oauth2.utils;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.security.auth.x500.X500Principal;

import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.common.util.Base64UrlUtility;
import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.rs.security.jose.common.JoseConstants;
import org.apache.cxf.rs.security.jose.jwa.AlgorithmUtils;
import org.apache.cxf.rs.security.jose.jwa.ContentAlgorithm;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.cxf.rs.security.jose.jwe.JweDecryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweEncryptionProvider;
import org.apache.cxf.rs.security.jose.jwe.JweUtils;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureProvider;
import org.apache.cxf.rs.security.jose.jws.JwsSignatureVerifier;
import org.apache.cxf.rs.security.jose.jws.JwsUtils;
import org.apache.cxf.rs.security.oauth2.common.AuthenticationMethod;
import org.apache.cxf.rs.security.oauth2.common.Client;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oauth2.common.OAuthPermission;
import org.apache.cxf.rs.security.oauth2.common.ServerAccessToken;
import org.apache.cxf.rs.security.oauth2.common.UserSubject;
import org.apache.cxf.rs.security.oauth2.provider.OAuthServiceException;
import org.apache.cxf.rt.security.crypto.CryptoUtils;
import org.apache.cxf.rt.security.crypto.MessageDigestUtils;
import org.apache.cxf.security.LoginSecurityContext;
import org.apache.cxf.security.SecurityContext;
import org.apache.cxf.security.transport.TLSSessionInfo;

import static java.util.stream.Collectors.toList;

/**
 * Various utility methods
 */
public final class OAuthUtils {

    private OAuthUtils() {
    }

    public static byte[] createCertificateThumbprint(X509Certificate cert) throws Exception {
        return MessageDigestUtils.createDigest(cert.getEncoded(), MessageDigestUtils.ALGO_SHA_256);
    }

    public static void setCertificateThumbprintConfirmation(MessageContext mc, X509Certificate cert) {
        try {
            byte[] thumbprint = createCertificateThumbprint(cert);
            String encodedThumbprint = Base64UrlUtility.encode(thumbprint);
            mc.put(JoseConstants.HEADER_X509_THUMBPRINT_SHA256, encodedThumbprint);
        } catch (Exception ex) {
            throw new OAuthServiceException(ex);
        }
    }

    public static boolean compareCertificateThumbprints(X509Certificate cert, String encodedThumbprint) {
        try {
            byte[] thumbprint = createCertificateThumbprint(cert);
            byte[] currentThumbprint = Base64UrlUtility.decode(encodedThumbprint);
            return MessageDigest.isEqual(thumbprint, currentThumbprint);
        } catch (Exception ex) {
            return false;
        }
    }


    public static boolean compareTlsCertificates(TLSSessionInfo tlsInfo,
                                          List<String> base64EncodedCerts) {
        Certificate[] clientCerts = tlsInfo.getPeerCertificates();
        if (clientCerts.length == base64EncodedCerts.size()) {
            try {
                for (int i = 0; i < clientCerts.length; i++) {
                    X509Certificate x509Cert = (X509Certificate)clientCerts[i];
                    byte[] encodedKey = x509Cert.getEncoded();
                    byte[] clientKey = Base64Utility.decode(base64EncodedCerts.get(i));
                    if (!Arrays.equals(encodedKey, clientKey)) {
                        return false;
                    }
                }
                return true;
            } catch (Exception ex) {
                // throw exception later
            }
        }
        return false;
    }

    public static boolean isMutualTls(jakarta.ws.rs.core.SecurityContext sc, TLSSessionInfo tlsSessionInfo) {
        // Pure 2-way TLS authentication
        return tlsSessionInfo != null
            && StringUtils.isEmpty(sc.getAuthenticationScheme())
            && getRootTLSCertificate(tlsSessionInfo) != null;
    }

    public static String getSubjectDnFromTLSCertificates(X509Certificate cert) {
        X500Principal x509Principal = cert.getSubjectX500Principal();
        return x509Principal.getName();
    }

    public static String getIssuerDnFromTLSCertificates(X509Certificate cert) {
        X500Principal x509Principal = cert.getIssuerX500Principal();
        return x509Principal.getName();
    }

    public static X509Certificate getRootTLSCertificate(TLSSessionInfo tlsInfo) {
        Certificate[] clientCerts = tlsInfo.getPeerCertificates();
        if (clientCerts != null && clientCerts.length > 0) {
            return (X509Certificate)clientCerts[0];
        }
        return null;
    }

    public static void injectContextIntoOAuthProvider(MessageContext context, Object provider) {
        Method dataProviderContextMethod = null;
        try {
            dataProviderContextMethod = provider.getClass().getMethod("setMessageContext",
                                                                          new Class[]{MessageContext.class});
        } catch (Throwable t) {
            // ignore
        }
        if (dataProviderContextMethod != null) {
            try {
                dataProviderContextMethod.invoke(provider, new Object[]{context});
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    public static String setSessionToken(MessageContext mc) {
        return setSessionToken(mc, 0);
    }
    public static String setSessionToken(MessageContext mc, int maxInactiveInterval) {
        return setSessionToken(mc, generateRandomTokenKey(), maxInactiveInterval);
    }
    public static String setSessionToken(MessageContext mc, String sessionToken) {
        return setSessionToken(mc, sessionToken, 0);
    }
    public static String setSessionToken(MessageContext mc, String sessionToken, int maxInactiveInterval) {
        return setSessionToken(mc, sessionToken, null, maxInactiveInterval);
    }
    public static String setSessionToken(MessageContext mc, String sessionToken,
                                                String attribute, int maxInactiveInterval) {
        HttpSession session = mc.getHttpServletRequest().getSession();
        if (maxInactiveInterval > 0) {
            session.setMaxInactiveInterval(maxInactiveInterval);
        }
        String theAttribute = attribute == null ? OAuthConstants.SESSION_AUTHENTICITY_TOKEN : attribute;
        session.setAttribute(theAttribute, sessionToken);
        return sessionToken;
    }

    public static String getSessionToken(MessageContext mc) {
        return getSessionToken(mc, null);
    }
    public static String getSessionToken(MessageContext mc, String attribute) {
        return getSessionToken(mc, attribute, true);
    }
    public static String getSessionToken(MessageContext mc, String attribute, boolean remove) {
        HttpSession session = mc.getHttpServletRequest().getSession();
        String theAttribute = attribute == null ? OAuthConstants.SESSION_AUTHENTICITY_TOKEN : attribute;
        String sessionToken = (String)session.getAttribute(theAttribute);
        if (sessionToken != null && remove) {
            session.removeAttribute(theAttribute);
        }
        return sessionToken;
    }
    public static UserSubject createSubject(MessageContext mc, SecurityContext sc) {
        UserSubject subject = mc.getContent(UserSubject.class);
        if (subject != null) {
            return subject;
        }
        return OAuthUtils.createSubject(sc);
    }
    public static UserSubject createSubject(SecurityContext securityContext) {
        List<String> roleNames = Collections.emptyList();
        if (securityContext instanceof LoginSecurityContext) {
            roleNames = ((LoginSecurityContext) securityContext).getUserRoles().stream().map(Principal::getName)
                .collect(toList());
        }
        UserSubject subject = new UserSubject(securityContext.getUserPrincipal().getName(), roleNames);
        Message m = JAXRSUtils.getCurrentMessage();
        if (m != null && m.get(AuthenticationMethod.class) != null) {
            subject.setAuthenticationMethod(m.get(AuthenticationMethod.class));
        }
        return subject;
    }

    public static String convertPermissionsToScope(List<OAuthPermission> perms) {
        StringBuilder sb = new StringBuilder();
        for (OAuthPermission perm : perms) {
            if (perm.isInvisibleToClient() || perm.getPermission() == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(perm.getPermission());
        }
        return sb.toString();
    }

    public static List<String> convertPermissionsToScopeList(List<OAuthPermission> perms) {
        return perms.stream().map(OAuthPermission::getPermission).collect(toList());
    }

    public static boolean isGrantSupportedForClient(Client client,
                                                    boolean canSupportPublicClients,
                                                    String grantType) {
        if (grantType == null || !client.isConfidential() && !canSupportPublicClients) {
            return false;
        }
        List<String> allowedGrants = client.getAllowedGrantTypes();
        return allowedGrants.isEmpty() || allowedGrants.contains(grantType);
    }

    public static List<String> parseScope(String requestedScope) {
        if (requestedScope != null) {
            return Arrays.stream(requestedScope.split(" ")).filter(StringUtils.notEmpty()).collect(toList());
        } else {
            return Collections.emptyList();
        }
    }

    public static String generateRandomTokenKey() throws OAuthServiceException {
        return generateRandomTokenKey(32);
    }
    public static String generateRandomTokenKey(int byteSize) {
        if (byteSize < 16) {
            throw new OAuthServiceException();
        }
        return StringUtils.toHexString(CryptoUtils.generateSecureRandomBytes(byteSize));
    }

    public static long getIssuedAt() {
        return System.currentTimeMillis() / 1000L;
    }

    public static boolean isExpired(Long issuedAt, Long lifetime) {
        // At some point -1 was used to indicate an unlimited lifetime
        // with 0 being introduced instead at a later stage.
        // In theory there still could be a code around initializing the tokens with -1.
        // Treating -1 and 0 the same way is reasonable and it also makes it easier to
        // deal with the token introspection responses with no issuedAt time reported
        return lifetime == null
            || lifetime < -1
            || lifetime > 0L && issuedAt + lifetime < System.currentTimeMillis() / 1000L;
    }

    public static boolean validateAudience(String providedAudience,
                                           List<String> allowedAudiences) {
        return providedAudience == null
            || validateAudiences(Collections.singletonList(providedAudience), allowedAudiences);
    }
    public static boolean validateAudiences(List<String> providedAudiences,
                                            List<String> allowedAudiences) {
        return StringUtils.isEmpty(providedAudiences)
               && StringUtils.isEmpty(allowedAudiences)
               || allowedAudiences.containsAll(providedAudiences);
    }

    public static boolean checkRequestURI(String servletPath, String uri) {
        boolean wildcard = uri.endsWith("*");
        String theURI = wildcard ? uri.substring(0, uri.length() - 1) : uri;
        try {
            URITemplate template = new URITemplate(theURI);
            MultivaluedMap<String, String> map = new MetadataMap<>();
            if (template.match(servletPath, map)) {
                String finalGroup = map.getFirst(URITemplate.FINAL_MATCH_GROUP);
                if (wildcard || StringUtils.isEmpty(finalGroup) || "/".equals(finalGroup)) {
                    return true;
                }
            }
        } catch (Exception ex) {
            // ignore
        }
        return false;
    }

    public static List<String> getRequestedScopes(Client client,
                                                  String scopeParameter,
                                                  boolean useAllClientScopes,
                                                  boolean partialMatchScopeValidation) {
        return getRequestedScopes(client, scopeParameter, useAllClientScopes, partialMatchScopeValidation, true);
    }

    public static List<String> getRequestedScopes(Client client,
                                                  String scopeParameter,
                                                  boolean useAllClientScopes,
                                                  boolean partialMatchScopeValidation,
                                                  boolean defaultToRegisteredScopes) {
        List<String> requestScopes = parseScope(scopeParameter);
        List<String> registeredScopes = client.getRegisteredScopes();
        if (requestScopes.isEmpty()) {
            if (defaultToRegisteredScopes) {
                return registeredScopes;
            }
            return requestScopes;
        }
        if (!validateScopes(requestScopes, registeredScopes, partialMatchScopeValidation)) {
            throw new OAuthServiceException("Unexpected scope");
        }
        if (useAllClientScopes) {
            for (String registeredScope : registeredScopes) {
                if (!requestScopes.contains(registeredScope)) {
                    requestScopes.add(registeredScope);
                }
            }
        }

        return requestScopes;
    }

    public static boolean validateScopes(List<String> requestScopes, List<String> registeredScopes,
                                         boolean partialMatchScopeValidation) {
        if (!registeredScopes.isEmpty()) {
            // if it is a strict validation then pre-registered scopes have to contains all
            // the current request scopes
            if (!partialMatchScopeValidation) {
                return registeredScopes.containsAll(requestScopes);
            }
            for (String requestScope : requestScopes) {
                boolean match = false;
                for (String registeredScope : registeredScopes) {
                    if (requestScope.startsWith(registeredScope)) {
                        match = true;
                        break;
                    }
                }
                if (!match) {
                    return false;
                }
            }
        }
        return true;
    }

    public static ClientAccessToken toClientAccessToken(ServerAccessToken serverToken, boolean supportOptionalParams) {
        String tokenKey =
            serverToken.getEncodedToken() != null ? serverToken.getEncodedToken() : serverToken.getTokenKey();
        ClientAccessToken clientToken = new ClientAccessToken(serverToken.getTokenType(),
                                                              tokenKey);
        clientToken.setRefreshToken(serverToken.getRefreshToken());
        if (supportOptionalParams) {
            clientToken.setExpiresIn(serverToken.getExpiresIn());
            List<OAuthPermission> perms = serverToken.getScopes();
            String scopeString = OAuthUtils.convertPermissionsToScope(perms);
            if (!StringUtils.isEmpty(scopeString)) {
                clientToken.setApprovedScope(scopeString);
            }
            clientToken.setParameters(new HashMap<String, String>(serverToken.getParameters()));
        }
        return clientToken;
    }

    public static JwsSignatureProvider getClientSecretSignatureProvider(String clientSecret) {
        Properties sigProps = JwsUtils.loadSignatureOutProperties(false);
        return JwsUtils.getHmacSignatureProvider(clientSecret,
                                                 getClientSecretSignatureAlgorithm(sigProps));
    }
    public static JwsSignatureVerifier getClientSecretSignatureVerifier(String clientSecret) {
        Properties sigProps = JwsUtils.loadSignatureOutProperties(false);
        return JwsUtils.getHmacSignatureVerifier(clientSecret,
                                                 getClientSecretSignatureAlgorithm(sigProps));
    }

    public static JweDecryptionProvider getClientSecretDecryptionProvider(String clientSecret) {
        Properties props = JweUtils.loadEncryptionInProperties(false);
        byte[] key = StringUtils.toBytesUTF8(clientSecret);
        return JweUtils.getDirectKeyJweDecryption(key, getClientSecretContentAlgorithm(props));
    }

    public static JweEncryptionProvider getClientSecretEncryptionProvider(String clientSecret) {
        Properties props = JweUtils.loadEncryptionInProperties(false);
        byte[] key = StringUtils.toBytesUTF8(clientSecret);
        return JweUtils.getDirectKeyJweEncryption(key, getClientSecretContentAlgorithm(props));
    }

    private static ContentAlgorithm getClientSecretContentAlgorithm(Properties props) {
        String ctAlgoProp = props.getProperty(OAuthConstants.CLIENT_SECRET_CONTENT_ENCRYPTION_ALGORITHM);
        if (ctAlgoProp == null) {
            ctAlgoProp = props.getProperty(JoseConstants.RSSEC_ENCRYPTION_CONTENT_ALGORITHM);
        }
        ContentAlgorithm ctAlgo = ContentAlgorithm.getAlgorithm(ctAlgoProp);
        ctAlgo = ctAlgo != null ? ctAlgo : ContentAlgorithm.A128GCM;
        return ctAlgo;
    }

    public static SignatureAlgorithm getClientSecretSignatureAlgorithm(Properties sigProps) {

        String clientSecretSigProp = sigProps.getProperty(OAuthConstants.CLIENT_SECRET_SIGNATURE_ALGORITHM);
        if (clientSecretSigProp == null) {
            String sigProp = sigProps.getProperty(JoseConstants.RSSEC_SIGNATURE_ALGORITHM);
            if (AlgorithmUtils.isHmacSign(sigProp)) {
                clientSecretSigProp = sigProp;
            }
        }
        SignatureAlgorithm sigAlgo = SignatureAlgorithm.getAlgorithm(clientSecretSigProp);
        sigAlgo = sigAlgo != null ? sigAlgo : SignatureAlgorithm.HS256;
        if (!AlgorithmUtils.isHmacSign(sigAlgo)) {
            // Must be HS-based for the symmetric signature
            throw new OAuthServiceException(OAuthConstants.SERVER_ERROR);
        }
        return sigAlgo;
    }

    public static String convertListOfScopesToString(List<String> registeredScopes) {
        return String.join(" ", registeredScopes);
    }
}
