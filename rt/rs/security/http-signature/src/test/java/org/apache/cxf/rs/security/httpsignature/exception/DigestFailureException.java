package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class DigestFailureException extends SignatureException {
    public DigestFailureException(String message) {
        super(message);
    }
}
