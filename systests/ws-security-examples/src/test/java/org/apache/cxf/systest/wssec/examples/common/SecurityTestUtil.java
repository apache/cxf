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
package org.apache.cxf.systest.wssec.examples.common;

import java.io.File;

/**
 * A utility class for security tests
 */
public final class SecurityTestUtil {
    
    private SecurityTestUtil() {
        // complete
    }
    
    public static void cleanup() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        if (tmpDir != null) {
            File nonceFile = new File(tmpDir + File.separator + "ws-security.nonce.cache.instance.data");
            if (nonceFile.exists()) {
                nonceFile.delete();
            }
            File tsFile = new File(tmpDir + File.separator + "ws-security.timestamp.cache.instance.data");
            if (tsFile.exists()) {
                tsFile.delete();
            }
        }
    }
    
}
