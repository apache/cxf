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

/**
 * This holds some parameters to pass to the tests to avoid duplicating code.
 */
public final class TestParam {
    final String port;
    final boolean streaming;
    final String stsPort;

    public TestParam(String p, boolean b) {
        this(p, b, null);
    }

    public TestParam(String p, boolean b, String stsPort) {
        port = p;
        streaming = b;
        this.stsPort = stsPort;
    }

    public String toString() {
        return port + ':' + (streaming ? "streaming" : "dom") + ':' + stsPort;
    }

    public String getPort() {
        return port;
    }

    public boolean isStreaming() {
        return streaming;
    }

    public String getStsPort() {
        return stsPort;
    }

}
