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

package org.apache.cxf.ext.logging.event;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;

/**
 * Formats a log message showing the most important meta data
 */
public final class LogMessageFormatter {
    
    private LogMessageFormatter() {
    }
    
    public static String format(LogEvent event) {
        StringBuilder b = new StringBuilder();
        // Start from the next line to have the output well-aligned
        b.append(event.getType()).append('\n');
        
        write(b, "Address", event.getAddress());
        write(b, "HttpMethod", event.getHttpMethod());
        write(b, "Content-Type", event.getContentType());
        write(b, "ResponseCode", event.getResponseCode());
        write(b, "ExchangeId", event.getExchangeId());
        if (event.getServiceName() != null) {
            write(b, "ServiceName", localPart(event.getServiceName()));
            write(b, "PortName", localPart(event.getPortName()));
            write(b, "PortTypeName", localPart(event.getPortTypeName()));
        }
        if (event.getFullContentFile() != null) {
            write(b, "FullContentFile", event.getFullContentFile().getAbsolutePath());
        }
        write(b, "Headers", event.getHeaders().toString());
        if (!StringUtils.isEmpty(event.getPayload())) {
            write(b, "Payload", event.getPayload());
        }
        return b.toString();
    }
    protected static String localPart(QName name) {
        return name == null ? null : name.getLocalPart();
    }

    protected static void write(StringBuilder b, String key, String value) {
        if (value != null) {
            b.append("    ").append(key).append(": ").append(value).append('\n');
        }
    }
}
