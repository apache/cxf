package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class MissingSignatureHeaderException extends SignatureException {
    public MissingSignatureHeaderException(String message) {
        super(message);
    }
}
