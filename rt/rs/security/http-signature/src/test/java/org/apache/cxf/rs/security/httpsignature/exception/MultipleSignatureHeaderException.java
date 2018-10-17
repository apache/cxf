package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class MultipleSignatureHeaderException extends SignatureException {
    public MultipleSignatureHeaderException(String message) {
        super(message);
    }
}
