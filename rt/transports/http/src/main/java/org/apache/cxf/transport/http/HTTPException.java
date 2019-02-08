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
package org.apache.cxf.transport.http;

import java.io.IOException;
import java.net.URL;

public class HTTPException extends IOException {
    private static final long serialVersionUID = -6186825695409511895L;
    private int responseCode;
    private String responseMessage;
    private URL url;

    public HTTPException(int responseCode, String responseMessage, URL url) {
        super("HTTP response '" + responseCode + ": "
              + responseMessage + "' when communicating with " + url.toString());
        this.responseCode = responseCode;
        this.responseMessage = responseMessage;
        this.url = url;
    }

    public int getResponseCode() {
        return responseCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public URL getUrl() {
        return url;
    }

}
