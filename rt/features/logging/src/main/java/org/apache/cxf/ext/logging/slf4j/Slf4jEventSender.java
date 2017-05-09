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
package org.apache.cxf.ext.logging.slf4j;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.ext.logging.event.LogEvent;
import org.apache.cxf.ext.logging.event.LogEventSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.slf4j.MarkerFactory;

public class Slf4jEventSender implements LogEventSender {

    @Override
    public void send(LogEvent event) {
        String cat = "org.apache.cxf.services." + event.getPortTypeName().getLocalPart() + "." + event.getType();
        Logger log = LoggerFactory.getLogger(cat);
        Set<String> keys = new HashSet<>();
        try {
            put(keys, "Type", event.getType().toString());
            put(keys, "Address", event.getAddress());
            put(keys, "HttpMethod", event.getHttpMethod());
            put(keys, "Content-Type", event.getContentType());
            put(keys, "ResponseCode", event.getResponseCode());
            put(keys, "ExchangeId", event.getExchangeId());
            put(keys, "MessageId", event.getMessageId());
            if (event.getServiceName() != null) {
                put(keys, "ServiceName", localPart(event.getServiceName()));
                put(keys, "PortName", localPart(event.getPortName()));
                put(keys, "PortTypeName", localPart(event.getPortTypeName()));
            }
            if (event.getFullContentFile() != null) {
                put(keys, "FullContentFile", event.getFullContentFile().getAbsolutePath());
            }
            put(keys, "Headers", event.getHeaders().toString());
            log.info(MarkerFactory.getMarker(event.getServiceName() != null ? "SOAP" : "REST"), 
                     getLogMessage(event));
        } finally {
            for (String key : keys) {
                MDC.remove(key);
            }
        }

    }

    private String localPart(QName name) {
        return name == null ? null : name.getLocalPart();
    }

    protected String getLogMessage(LogEvent event) {
        return event.getPayload();
    }

    private void put(Set<String> keys, String key, String value) {
        if (value != null) {
            MDC.put(key, value);
            keys.add(key);
        }
    }

}
