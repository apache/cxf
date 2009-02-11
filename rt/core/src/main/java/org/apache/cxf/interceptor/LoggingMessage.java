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
package org.apache.cxf.interceptor;

public final class LoggingMessage {

    private final String heading;
    private final StringBuilder address;
    private final StringBuilder contentType;
    private final StringBuilder encoding;
    private final StringBuilder header;
    private final StringBuilder message;
    private final StringBuilder payload;

    public LoggingMessage(String h) {
        heading = h;

        contentType = new StringBuilder();
        address = new StringBuilder();
        encoding = new StringBuilder();
        header = new StringBuilder();
        message = new StringBuilder();
        payload = new StringBuilder();
    }
    public StringBuilder getAddress() {
        return address;
    }

    public StringBuilder getEncoding() {
        return encoding;
    }

    public StringBuilder getHeader() {
        return header;
    }

    public StringBuilder getContentType() {
        return contentType;
    }

    public StringBuilder getMessage() {
        return message;
    }

    public StringBuilder getPayload() {
        return payload;
    }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(heading);
        if (address.length() > 0) {
            buffer.append("\nAddress: ");
            buffer.append(address);
        }
        buffer.append("\nEncoding: ");
        buffer.append(encoding);
        buffer.append("\nContent-Type: ");
        buffer.append(contentType);
        buffer.append("\nHeaders: ");
        buffer.append(header);
        if (message.length() > 0) {
            buffer.append("\nMessages: ");
            buffer.append(message);
        }
        buffer.append("\nPayload: ");
        buffer.append(payload);
        buffer.append("\n--------------------------------------");
        return buffer.toString();
    }
}