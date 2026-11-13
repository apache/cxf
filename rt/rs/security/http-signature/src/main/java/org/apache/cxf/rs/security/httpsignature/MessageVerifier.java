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
package org.apache.cxf.rs.security.httpsignature;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.provider.AlgorithmProvider;
import org.apache.cxf.rs.security.httpsignature.provider.KeyProvider;
import org.apache.cxf.rs.security.httpsignature.provider.SecurityProvider;
import org.apache.cxf.rs.security.httpsignature.utils.DefaultSignatureConstants;
import org.apache.cxf.rs.security.httpsignature.utils.SignatureHeaderUtils;
import org.tomitribe.auth.signatures.Signature;

public class MessageVerifier {
    /**
     * The default maximum age in seconds of a signed "Date" header
     */
    public static final int DEFAULT_TIME_TO_LIVE = 300;
    /**
     * The default number of seconds a signed "Date" header may be in the future
     */
    public static final int DEFAULT_FUTURE_TIME_TO_LIVE = 60;

    private static final Logger LOG = LogUtils.getL7dLogger(MessageVerifier.class);

    private AlgorithmProvider algorithmProvider;
    private KeyProvider keyProvider;
    private SecurityProvider securityProvider;
    private final SignatureValidator signatureValidator;
    private final List<String> requiredHeaders;
    private boolean addDefaultRequiredHeaders = true;
    private Integer timeToLive;
    private Integer futureTimeToLive;

    public MessageVerifier(KeyProvider keyProvider) {
        this(keyProvider, (List<String>)null);
    }

    public MessageVerifier(KeyProvider keyProvider, List<String> requiredHeaders) {
        this(keyProvider,
            null,
            keyId -> DefaultSignatureConstants.SIGNING_ALGORITHM,
            requiredHeaders);
    }

    public MessageVerifier(KeyProvider keyProvider,
                           AlgorithmProvider algorithmProvider) {
        this(keyProvider, null, algorithmProvider, Collections.emptyList());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider) {
        this(keyProvider, securityProvider, algorithmProvider, Collections.emptyList());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider,
                           List<String> requiredHeaders) {
        this(keyProvider, securityProvider, algorithmProvider, requiredHeaders, new TomitribeSignatureValidator());
    }

    public MessageVerifier(KeyProvider keyProvider,
                           SecurityProvider securityProvider,
                           AlgorithmProvider algorithmProvider,
                           List<String> requiredHeaders,
                           SignatureValidator signatureValidator) {
        setkeyProvider(keyProvider);
        setSecurityProvider(securityProvider);
        setAlgorithmProvider(algorithmProvider);
        this.requiredHeaders = requiredHeaders == null ? Collections.emptyList() : requiredHeaders;
        this.signatureValidator = signatureValidator;
    }

    public final void setkeyProvider(KeyProvider provider) {
        this.keyProvider = Objects.requireNonNull(provider, "public key provider cannot be null");
    }

    public final void setSecurityProvider(SecurityProvider securityProvider) {

        this.securityProvider = securityProvider;
    }

    public final void setAlgorithmProvider(AlgorithmProvider algorithmProvider) {
        this.algorithmProvider = Objects.requireNonNull(algorithmProvider, "algorithm provider cannot be null");
    }

    public void verifyMessage(Map<String, List<String>> messageHeaders, String method,
                              String uri, Message m, byte[] messageBody) {
        SignatureHeaderUtils.inspectMessageHeaders(messageHeaders);
        inspectMissingSignatureHeader(messageHeaders);

        inspectMultipleSignatureHeaders(messageHeaders);

        // Calculate the headers that we require to be signed. Use the headers configured on this class if available,
        // falling back to the contextual property.
        List<String> signedHeaders = new ArrayList<>(requiredHeaders);
        if (requiredHeaders.isEmpty()
            && m.getContextualProperty(HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_IN_HEADERS) != null) {
            signedHeaders =
                CastUtils.cast(
                    (List<?>)m.getContextualProperty(HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_IN_HEADERS));
        }

        // Add the default required headers
        boolean requestor = MessageUtils.isRequestor(m);
        if (addDefaultRequiredHeaders) {
            if (!(requestor || signedHeaders.contains(HTTPSignatureConstants.REQUEST_TARGET))) {
                signedHeaders.add(HTTPSignatureConstants.REQUEST_TARGET);
            }

            if (!signedHeaders.contains("digest") && ((messageBody != null && messageBody.length > 0)
                    || ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)
                    || "PATCH".equalsIgnoreCase(method)))) {
                signedHeaders.add("digest");
            }
        }

        // Now dynamically check to see whether the "digest" header is required or not. It's not required
        // for the service case on a GET/HEAD request as there is no request, and also not required on
        // the client side unless it is an OK response that is not 204. It's still required in these cases
        // if there is a message body
        Integer status = (Integer)m.get(Message.RESPONSE_CODE);
        boolean noBodyExpected = (requestor && (status < 200 || status >= 300 || status == 204))
            || (!requestor && ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)));
        if (noBodyExpected && (messageBody == null || messageBody.length == 0)) {
            signedHeaders.remove("digest");
        }

        LOG.fine("Starting signature verification");
        signatureValidator.validate(messageHeaders,
                                    algorithmProvider,
                                    keyProvider,
                                    securityProvider,
                                    method,
                                    uri,
                                    signedHeaders);
        verifyDateHeader(messageHeaders, m);
        LOG.fine("Finished signature verification");
    }

    /**
     * Reject a message whose signed "Date" header is outside the configured time window, so that a captured
     * signed message can not be replayed once its time to live has passed. A "Date" header which is not
     * covered by the signature is ignored, as it can be changed along with the rest of the unsigned message.
     * Values set on this class take precedence over the contextual properties.
     */
    private void verifyDateHeader(Map<String, List<String>> messageHeaders, Message m) {
        int ttl = timeToLive != null ? timeToLive
            : getContextualInteger(m, HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_TIME_TO_LIVE, DEFAULT_TIME_TO_LIVE);
        int futureTtl = futureTimeToLive != null ? futureTimeToLive
            : getContextualInteger(m, HTTPSignatureConstants.RSSEC_HTTP_SIGNATURE_FUTURE_TIME_TO_LIVE,
                                   DEFAULT_FUTURE_TIME_TO_LIVE);
        // A time to live of 0 would reject every message, so 0 or less disables the age check. For the future
        // check 0 means no clock skew is allowed and only a negative value disables it.
        if (ttl <= 0 && futureTtl < 0) {
            return;
        }
        List<String> signedHeaders = Signature.fromString(messageHeaders.get("Signature").get(0)).getHeaders();
        if (!signedHeaders.contains("date")) {
            return;
        }
        // Check the same value that was signed, see SignatureHeaderUtils.mapHeaders
        List<String> dates = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : messageHeaders.entrySet()) {
            if ("date".equalsIgnoreCase(entry.getKey())) {
                dates.addAll(entry.getValue());
            }
        }
        if (dates.isEmpty()) {
            return;
        }
        if (dates.size() > 1) {
            throw new InvalidSignatureException("there are multiple signed Date header values");
        }
        final long dateTime;
        try {
            dateTime = HttpUtils.getHttpDateFormat().parse(dates.get(0)).getTime();
        } catch (ParseException ex) {
            throw new InvalidSignatureException("the signed Date header can not be parsed");
        }
        long now = System.currentTimeMillis();
        if (ttl > 0 && now - dateTime > TimeUnit.SECONDS.toMillis(ttl)) {
            throw new InvalidSignatureException("the signed Date header is older than the time to live");
        }
        if (futureTtl >= 0 && dateTime - now > TimeUnit.SECONDS.toMillis(futureTtl)) {
            throw new InvalidSignatureException("the signed Date header is in the future");
        }
    }

    private static int getContextualInteger(Message m, String key, int defaultValue) {
        Object value = m.getContextualProperty(key);
        if (value instanceof Number) {
            return ((Number)value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String)value);
            } catch (NumberFormatException ex) {
                LOG.warning("Incorrect integer value of " + value + " specified for: " + key);
            }
        }
        return defaultValue;
    }

    private void inspectMultipleSignatureHeaders(Map<String, List<String>> responseHeaders) {
        if (responseHeaders.get("Signature").size() > 1) {
            throw new MultipleSignatureHeaderException("there are multiple signature headers in request");
        }
    }

    private void inspectMissingSignatureHeader(Map<String, List<String>> responseHeaders) {
        if (!responseHeaders.containsKey("Signature")) {
            throw new MissingSignatureHeaderException("there is no signature header in request");
        }
    }

    public boolean isAddDefaultRequiredHeaders() {
        return addDefaultRequiredHeaders;
    }

    /**
     * Set whether we require some default headers to be signed, such as "digest" and "(request-target"),
     * depending on whether there is a request body or not, and whether we are the client or not
     */
    public void setAddDefaultRequiredHeaders(boolean addDefaultRequiredHeaders) {
        this.addDefaultRequiredHeaders = addDefaultRequiredHeaders;
    }

    /**
     * Return the configured maximum age in seconds of a signed "Date" header, or null if it was not set on
     * this class, in which case the contextual property or the default of 300 seconds is used.
     */
    public Integer getTimeToLive() {
        return timeToLive;
    }

    /**
     * Set the maximum age in seconds of a signed "Date" header. A message whose signed "Date" is older
     * than this is rejected. This takes precedence over the "rs.security.http.signature.time.to.live"
     * contextual property. The default is 300 seconds, a value of 0 or less disables the check, and null
     * falls back to the contextual property.
     */
    public void setTimeToLive(Integer timeToLive) {
        this.timeToLive = timeToLive;
    }

    /**
     * Return the configured number of seconds a signed "Date" header may be in the future, or null if it was
     * not set on this class, in which case the contextual property or the default of 60 seconds is used.
     */
    public Integer getFutureTimeToLive() {
        return futureTimeToLive;
    }

    /**
     * Set the number of seconds a signed "Date" header may be in the future, to allow for clock skew.
     * This takes precedence over the "rs.security.http.signature.future.time.to.live" contextual property.
     * The default is 60 seconds, a value of 0 allows no clock skew, a negative value disables the check, and
     * null falls back to the contextual property. With the check disabled, a message with a signed "Date" in
     * the future stays within the time to live until that date.
     */
    public void setFutureTimeToLive(Integer futureTimeToLive) {
        this.futureTimeToLive = futureTimeToLive;
    }

}
