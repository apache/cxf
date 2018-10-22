package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class MissingDigestException extends SignatureException {
    public MissingDigestException(String message) {
        super(message);
    }
}
