/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.rs.security.httpsignature;

import org.apache.cxf.rs.security.httpsignature.exception.DifferentAlgorithmsException;
import org.apache.cxf.rs.security.httpsignature.exception.DifferentDigestsException;
import org.apache.cxf.rs.security.httpsignature.exception.DigestFailureException;
import org.apache.cxf.rs.security.httpsignature.exception.FailedToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidDataToVerifySignatureException;
import org.apache.cxf.rs.security.httpsignature.exception.InvalidSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingDigestException;
import org.apache.cxf.rs.security.httpsignature.exception.MissingSignatureHeaderException;
import org.apache.cxf.rs.security.httpsignature.exception.MultipleSignatureHeaderException;

public class MockExceptionHandler implements ExceptionHandler {
    @Override
    public RuntimeException handle(SignatureException exception, SignatureExceptionType type) {
        switch (type) {
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
        case MISSING_DIGEST:
            return new MissingDigestException(exception.getMessage());
        case MULTIPLE_SIGNATURE_HEADERS:
            return new MultipleSignatureHeaderException(exception.getMessage());
        case DIFFERENT_ALGORITHMS:
            return new DifferentAlgorithmsException(exception.getMessage());
        default:
            return new SignatureException("Unknown exception type");
        }
    }
}
