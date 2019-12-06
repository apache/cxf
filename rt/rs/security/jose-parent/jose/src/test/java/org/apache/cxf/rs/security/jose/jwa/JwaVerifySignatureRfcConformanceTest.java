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

public abstract class JwaVerifySignatureRfcConformanceTest extends AbstractVerifySignatureTest {

    @Test
    public void testRsaRs256JwsCompact() throws Exception {
        test("/jws/rsa.2048.rs256.compact.jws");
    }

    @Test
    public void testRsaRs256JwsJsonFlattened() throws Exception {
        test("/jws/rsa.2048.rs256.json.flattened.jws");
    }

    @Test
    public void testRsaRs256JwsJson() throws Exception {
        test("/jws/rsa.2048.rs256.json.jws");
    }

    @Test
    public void testEcEs256JwsCompact() throws Exception {
        test("/jws/ec.p-256.es256.compact.jws");
    }

    @Test
    public void testEcEs256JwsJsonFlattened() throws Exception {
        test("/jws/ec.p-256.es256.json.flattened.jws");
    }

    @Test
    public void testEcEs256JwsJson() throws Exception {
        test("/jws/ec.p-256.es256.json.jws");
    }


}
