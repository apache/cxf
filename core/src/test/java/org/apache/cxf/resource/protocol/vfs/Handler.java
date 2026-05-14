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
package org.apache.cxf.resource.protocol.vfs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;

public class Handler extends URLStreamHandler {
    private static final byte[] CONTENT = "vfs-test-content".getBytes(StandardCharsets.UTF_8);

    @Override
    protected URLConnection openConnection(URL url) {
        return new URLConnection(url) {
            @Override
            public void connect() {
                // No-op: test URL handler has no external endpoint.
            }

            @Override
            public InputStream getInputStream() throws IOException {
                connect();
                return new ByteArrayInputStream(CONTENT);
            }
        };
    }
}
