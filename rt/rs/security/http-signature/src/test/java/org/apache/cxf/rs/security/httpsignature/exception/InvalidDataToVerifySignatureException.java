package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class InvalidDataToVerifySignatureException extends SignatureException {
    public InvalidDataToVerifySignatureException(String message) {
        super(message);
    }
}
