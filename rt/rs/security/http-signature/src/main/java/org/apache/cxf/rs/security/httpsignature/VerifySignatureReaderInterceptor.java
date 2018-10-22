package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.common.logging.LogUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * RS CXF Reader Interceptor which extracts signature data from the message and sends it to the message verifier
 *
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class VerifySignatureReaderInterceptor implements ReaderInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(VerifySignatureReaderInterceptor.class);

    private MessageVerifier messageVerifier;

    private boolean enabled;

    public VerifySignatureReaderInterceptor() {
        setEnabled(true);
        setMessageVerifier(new MessageVerifier(true));
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException, WebApplicationException {
        if (!enabled) {
            LOG.fine("Verify signature reader interceptor is disabled");
            return context.proceed();
        }
        LOG.fine("Starting interceptor message verification process");

        Map<String, List<String>> responseHeaders = context.getHeaders();

        String messageBody = extractMessageBody(context);

        messageVerifier.verifyMessage(responseHeaders, messageBody);

        context.setInputStream(new ByteArrayInputStream(messageBody.getBytes()));
        LOG.fine("Finished interceptor message verification process");

        return context.proceed();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setMessageVerifier(MessageVerifier messageVerifier) {
        Objects.requireNonNull(messageVerifier);
        this.messageVerifier = messageVerifier;
    }

    public MessageVerifier getMessageVerifier() {
        return messageVerifier;
    }

    private String extractMessageBody(ReaderInterceptorContext context) {
        try (InputStream is = context.getInputStream()) {
            try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (IOException exception) {
            throw messageVerifier.getExceptionHandler()
                    .handle(new SignatureException("failed to validate the digest"),
                            SignatureExceptionType.DIGEST_FAILURE);
        }
    }

}
