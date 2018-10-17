package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class DifferentDigestsException extends SignatureException {
    public DifferentDigestsException(String message) {
        super(message);
    }
}
