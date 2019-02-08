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
package org.apache.cxf.xkms.handlers;

public enum Applications {
    /**
     * Certificate Subject Name
     */
    PKIX("urn:ietf:rfc:2459"),
    /**
     * DNS address of http server
     */
    TLS_HTTPS("urn:ietf:rfc:2818"),
    /**
     * Service Name
     */
    SERVICE_NAME("urn:apache:cxf:service:name"),
    /**
     * Service Endpoint
     */
    SERVICE_ENDPOINT("urn:apache:cxf:service:endpoint"),
    /**
     * Certificate Issuer
     */
    ISSUER("urn:x509:issuer"),
    /**
     * Certificate Serial Number
     */
    SERIAL("urn:x509:serial"),
    /**
     * SMTP email address of subject
     */
    PGP("urn:ietf:rfc:2440");

    private String uri;

    Applications(String uri) {
        this.uri = uri;
    }

    public String getUri() {
        return this.uri;
    }

    public static Applications fromUri(String uri) {
        for (Applications app : Applications.values()) {
            if (app.getUri().equals(uri)) {
                return app;
            }
        }
        return null;
    }

}
