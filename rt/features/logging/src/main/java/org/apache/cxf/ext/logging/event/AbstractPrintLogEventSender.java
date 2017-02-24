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
 *
 */
public abstract class AbstractPrintLogEventSender implements LogEventSender {
    
    protected StringBuilder prepareBuilder(StringBuilder b, LogEvent event) {
        b.append("\n");
        put(b, "Type", event.getType().toString());
        put(b, "Address", event.getAddress());
        put(b, "HttpMethod", event.getHttpMethod());
        put(b, "Content-Type", event.getContentType());
        put(b, "ResponseCode", event.getResponseCode());
        put(b, "ExchangeId", event.getExchangeId());
        put(b, "MessageId", event.getMessageId());
        if (event.getServiceName() != null) {
            put(b, "ServiceName", localPart(event.getServiceName()));
            put(b, "PortName", localPart(event.getPortName()));
            put(b, "PortTypeName", localPart(event.getPortTypeName()));
        }
        if (event.getFullContentFile() != null) {
            put(b, "FullContentFile", event.getFullContentFile().getAbsolutePath());
        }
        put(b, "Headers", event.getHeaders().toString());
        if (!StringUtils.isEmpty(event.getPayload())) {
            put(b, "Payload", event.getPayload());
        }
        return b;
    }
    protected String localPart(QName name) {
        return name == null ? null : name.getLocalPart();
    }

    protected void put(StringBuilder b, String key, String value) {
        if (value != null) {
            b.append("    ").append(key).append(": ").append(value).append("\n");
        }
    }
}
