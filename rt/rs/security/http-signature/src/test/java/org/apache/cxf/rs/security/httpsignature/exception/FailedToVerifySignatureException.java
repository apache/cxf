package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class FailedToVerifySignatureException extends SignatureException {
    public FailedToVerifySignatureException(String message) {
        super(message);
    }
}
