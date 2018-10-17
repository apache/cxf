package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.common.logging.LogUtils;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.util.*;
import java.util.logging.Logger;

/**
 * RS CXF Filter which extracts signature data from the context and sends it to the message verifier
 *
 * @author Fredrik Espedal
 */
@Provider
@PreMatching
@Priority(Priorities.AUTHENTICATION)
public class VerifySignatureFilter implements ContainerRequestFilter {
    private MessageVerifier messageVerifier;

    private boolean enabled;

    protected static final Logger LOG = LogUtils.getL7dLogger(VerifySignatureReaderInterceptor.class);


    public VerifySignatureFilter() {
        setEnabled(true);
        setMessageVerifier(new MessageVerifier(false));
    }

    @Override
    public void filter(ContainerRequestContext requestCtx) {
        if (!enabled) {
            LOG.info("Verify signature filter is disabled");
            return;
        }

        LOG.info("Starting filter message verification process");

        MultivaluedMap<String, String> responseHeaders = requestCtx.getHeaders();

        messageVerifier.verifyMessage(responseHeaders);
        LOG.info("Finished filter message verification process");
    }

    public void setMessageVerifier(MessageVerifier messageVerifier) {
        Objects.requireNonNull(messageVerifier);
        this.messageVerifier = messageVerifier;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
