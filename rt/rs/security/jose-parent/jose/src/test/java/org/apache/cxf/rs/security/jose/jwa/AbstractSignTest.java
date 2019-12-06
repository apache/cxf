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
package org.apache.cxf.rs.security.jose.jwa;

import org.apache.cxf.rs.security.jose.support.Serialization;

public abstract class AbstractSignTest extends AbstractJwaTest {

    protected final void test(String keyType, String signatureAlgorithm, Serialization serialization) {
        test(keyType, signatureAlgorithm, serialization, "Live long and prosper.");
    }

    protected final void test(String keyType, String signatureAlgorithm, Serialization serialization,
        String plainText) {
        test(keyType, signatureAlgorithm, serialization, plainText, "/jwk/priKeys.jwks");
    }

    protected final void test(String keyType, String signatureAlgorithm, Serialization serialization, String plainText,
        String jwksURI) {
        sign(keyType, signatureAlgorithm, serialization, plainText, loadResource(jwksURI));
    }

    protected abstract void sign(String keyType, String signatureAlgorithm, Serialization serialization,
        String plainText, String jwksJson);

}
