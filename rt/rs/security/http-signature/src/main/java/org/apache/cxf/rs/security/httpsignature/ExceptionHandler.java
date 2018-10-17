package org.apache.cxf.rs.security.httpsignature;

@FunctionalInterface
public interface ExceptionHandler {
    RuntimeException handle(SignatureException exception, SignatureExceptionType type);
}
