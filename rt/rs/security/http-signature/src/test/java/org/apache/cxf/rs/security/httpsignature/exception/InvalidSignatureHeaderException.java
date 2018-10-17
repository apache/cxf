package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class InvalidSignatureHeaderException extends SignatureException {
    public InvalidSignatureHeaderException(String message) {
        super(message);
    }
}
