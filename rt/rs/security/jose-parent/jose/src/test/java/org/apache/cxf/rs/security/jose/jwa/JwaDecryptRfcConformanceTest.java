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

import org.junit.Test;

public abstract class JwaDecryptRfcConformanceTest extends AbstractDecryptTest {

    @Test
    public void testOctA128GcmJweCompact() throws Exception {
        test("/jwe/oct.128.a128kw.a128gcm.compact.jwe");
    }

    @Test
    public void testOctA128GcmJweJsonFlattened() throws Exception {
        test("/jwe/oct.128.a128kw.a128gcm.json.flattened.jwe");
    }

    @Test
    public void testOctA128GcmJweJson() throws Exception {
        test("/jwe/oct.128.a128kw.a128gcm.json.jwe");
    }

    @Test
    public void testRsaOaepA128GcmJweCompact() throws Exception {
        test("/jwe/rsa.2048.rsa-oaep.a128gcm.compact.jwe");
    }

    @Test
    public void testRsaOaepA128GcmJweJsonFlattened() throws Exception {
        test("/jwe/rsa.2048.rsa-oaep.a128gcm.json.flattened.jwe");
    }

    @Test
    public void testRsaOaepA128GcmJweJson() throws Exception {
        test("/jwe/rsa.2048.rsa-oaep.a128gcm.json.jwe");
    }

    @Test
    public void testEcdhDirectA128GcmJweCompact() throws Exception {
        test("/jwe/ec.p-256.ecdh-es.a128gcm.compact.jwe");
    }

    @Test
    public void testEcdhDirectA128GcmJweJsonFlattened() throws Exception {
        test("/jwe/ec.p-256.ecdh-es.a128gcm.json.flattened.jwe");
    }

    @Test
    public void testEcdhDirectA128GcmJweJson() throws Exception {
        test("/jwe/ec.p-256.ecdh-es.a128gcm.json.jwe");
    }

    @Test
    public void testEcdhA128KwA128GcmJweCompact() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128gcm.compact.jwe");
    }

    @Test
    public void testEcdhA128KwA128GcmJweJsonFlattened() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128gcm.json.flattened.jwe");
    }

    @Test
    public void testEcdhA128KwA128GcmJweJson() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128gcm.json.jwe");
    }

    @Test
    public void testEcdhA128KwA128CbcJweCompact() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128cbc-hs256.compact.jwe");
    }

    @Test
    public void testEcdhA128KwA128CbcJweJsonFlattened() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128cbc-hs256.json.flattened.jwe");
    }

    @Test
    public void testEcdhA128KwA128CbcJweJson() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a128kw.a128cbc-hs256.json.jwe");
    }

    @Test
    public void testRsa15A128GcmCompact() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128gcm.compact.jwe");
    }

    @Test
    public void testRsa15A128GcmJsonFlattened() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128gcm.json.flattened.jwe");
    }

    @Test
    public void testRsa15A128GcmJson() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128gcm.json.jwe");
    }

    @Test
    public void testRsa15A128CbcJweCompact() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128cbc-hs256.compact.jwe");
    }

    @Test
    public void testRsa15A128CbcJweJsonFlattened() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128cbc-hs256.json.flattened.jwe");
    }

    @Test
    public void testRsa15A128CbcJweJson() throws Exception {
        test("/jwe/rsa.2048.rsa1_5.a128cbc-hs256.json.jwe");
    }

    @Test
    public void testOctA128CbcJweCompact() throws Exception {
        test("/jwe/oct.128.a128kw.a128cbc-hs256.compact.jwe");
    }

    @Test
    public void testOctA128CbcJweJsonFlattened() throws Exception {
        test("/jwe/oct.128.a128kw.a128cbc-hs256.json.flattened.jwe");
    }

    @Test
    public void testOctA128CbcJweJson() throws Exception {
        test("/jwe/oct.128.a128kw.a128cbc-hs256.json.jwe");
    }

    @Test
    public void testEcdhA256wA128GcmJweJson() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a256kw.a128gcm.json.jwe");
    }

    @Test
    public void testEcdhA256KwA128CbcJweHs256() throws Exception {
        test("/jwe/ec.p-256.ecdh-es+a256kw.a128cbc-hs256.json.jwe");
    }

}
