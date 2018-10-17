package org.apache.cxf.rs.security.httpsignature.exception;

import org.apache.cxf.rs.security.httpsignature.SignatureException;

public class DifferentAlgorithmsException extends SignatureException {
    public DifferentAlgorithmsException(String message) {
        super(message);
    }
}
