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
package org.apache.cxf.rs.security.jose.jwk;

import org.junit.Assert;
import org.junit.Test;

public class JwkUtilsTest extends Assert {
    private static final String RSA_KEY = "{"
      + "\"kty\": \"RSA\","
      + "\"n\": \"0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAt"
      +      "VT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn6"
      +      "4tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FD"
      +      "W2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n9"
      +      "1CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINH"
      +      "aQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw\","
      + "\"e\": \"AQAB\","
      + "\"alg\": \"RS256\","
      + "\"kid\": \"2011-04-29\""
      + "}\"";
    @Test
    public void testRsaKeyThumbprint() throws Exception {
        String thumbprint = JwkUtils.getThumbprint(RSA_KEY);
        assertEquals("NzbLsXh8uDCcd-6MNwXF4W_7noWXFZAfHkxZsRGC9Xs", thumbprint);
    }
    
}
