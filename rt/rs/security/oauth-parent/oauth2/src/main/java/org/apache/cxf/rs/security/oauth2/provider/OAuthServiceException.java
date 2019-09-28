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

package org.apache.cxf.rs.security.oauth2.provider;

import org.apache.cxf.rs.security.oauth2.common.OAuthError;
import org.apache.cxf.rs.security.oauth2.utils.OAuthConstants;


/**
 * Encapsulates OAuth-related problems
 */
public class OAuthServiceException extends RuntimeException {

    private static final long serialVersionUID = 343738539234766320L;
    private OAuthError error;

    public OAuthServiceException() {
        super(OAuthConstants.SERVER_ERROR);
    }

    public OAuthServiceException(String message) {
        super(message);
    }

    public OAuthServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public OAuthServiceException(Throwable cause) {
        super(OAuthConstants.SERVER_ERROR, cause);
    }

    public OAuthServiceException(OAuthError error) {
        this.error = error;
    }

    public OAuthServiceException(OAuthError error, Throwable cause) {
        super(cause);
        this.error = error;
    }

    public OAuthError getError() {
        return error;
    }

    @Override
    public String getMessage() {
        return error == null ? super.getMessage() : error.toString();
    }
}
