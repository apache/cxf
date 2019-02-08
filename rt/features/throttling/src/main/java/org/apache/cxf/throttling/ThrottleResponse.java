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

package org.apache.cxf.throttling;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class ThrottleResponse {
    protected long delay;
    protected Map<String, String> responseHeaders = new HashMap<>();
    protected int responseCode = -1;
    protected String errorMessage;

    public ThrottleResponse() {

    }

    public ThrottleResponse(int responceCode) {
        this.responseCode = responceCode;
    }

    public ThrottleResponse(int responceCode, long delay) {
        this(responceCode);
        this.delay = delay;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
    /**
     * Add headers to the response.  Typically, this would be things like X-RateLimit-Limit headers
     * @return
     */
    public ThrottleResponse addResponseHeader(String header, String value) {
        responseHeaders.put(header, value);
        return this;
    }

    public int getResponseCode() {
        return responseCode;
    }
    public String getErrorMessage() {
        return errorMessage;
    }

    public ThrottleResponse setResponseCode(int rc) {
        return setResponseCode(rc, null);
    }
    public ThrottleResponse setResponseCode(int rc, String msg) {
        this.responseCode = rc;
        this.errorMessage = msg;
        return this;
    }

    /**
     * Delay processing for specified milliseconds.
     * Should be "small" to prevent the client from timing out unless the client request is
     * aborted with the HTTP error code.
     * @return
     */
    public long getDelay() {
        return delay;
    }

    public ThrottleResponse setDelay(long d) {
        this.delay = d;
        return this;
    }
}
