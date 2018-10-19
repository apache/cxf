package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.rs.security.httpsignature.exception.*;

public class MockExceptionHandler implements ExceptionHandler {
    @Override
    public RuntimeException handle(SignatureException exception, SignatureExceptionType type) {
        switch (type){
            case INVALID_SIGNATURE_HEADER:
                return new InvalidSignatureHeaderException(exception.getMessage());
            case MISSING_SIGNATURE_HEADER:
                return new MissingSignatureHeaderException(exception.getMessage());
            case FAILED_TO_VERIFY_SIGNATURE:
                return new FailedToVerifySignatureException(exception.getMessage());
            case INVALID_DATA_TO_VERIFY_SIGNATURE:
                return new InvalidDataToVerifySignatureException(exception.getMessage());
            case DIGEST_FAILURE:
                return new DigestFailureException(exception.getMessage());
            case DIFFERENT_DIGESTS:
                return new DifferentDigestsException(exception.getMessage());
            case MULTIPLE_SIGNATURE_HEADERS:
                return new MultipleSignatureHeaderException(exception.getMessage());
            case DIFFERENT_ALGORITHMS:
                return new DifferentAlgorithmsException(exception.getMessage());
            default:
                return new SignatureException("Unknown exception type");
        }
    }
}
