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
package org.apache.cxf.systest.sts.common;

import java.io.File;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.ws.security.SecurityConstants;
import org.example.contract.doubleit.DoubleItPortType;

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
            File[] tmpFiles = new File(tmpDir).listFiles();
            if (tmpFiles != null) {
                for (File tmpFile : tmpFiles) {
                    if (tmpFile.exists() && (tmpFile.getName().startsWith("ws-security.nonce.cache")
                        || tmpFile.getName().startsWith("wss4j-nonce-cache")
                        || tmpFile.getName().startsWith("ws-security.timestamp.cache")
                        || tmpFile.getName().startsWith("wss4j-timestamp-cache"))) {
                        tmpFile.delete();
                    }
                }
            }
        }
    }

    public static void enableStreaming(DoubleItPortType port) {
        ((BindingProvider)port).getRequestContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
        ((BindingProvider)port).getResponseContext().put(
            SecurityConstants.ENABLE_STREAMING_SECURITY, "true"
        );
    }

}
