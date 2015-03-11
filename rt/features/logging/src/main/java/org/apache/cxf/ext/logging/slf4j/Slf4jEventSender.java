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

public class Slf4jEventSender implements LogEventSender {

    @Override
    public void send(LogEvent event) {
        String cat = "org.apache.cxf.services." + event.getPortTypeName() + "." + event.getType();
        Logger log = LoggerFactory.getLogger(cat);
        Set<String> keys = new HashSet<String>(); 
        try {
            put(keys, "type", event.getType().toString());
            put(keys, "address", event.getAddress());
            put(keys, "content-type", event.getContentType());
            put(keys, "encoding", event.getEncoding());
            put(keys, "exchangeId", event.getExchangeId());
            put(keys, "httpMethod", event.getHttpMethod());
            put(keys, "messageId", event.getMessageId());
            put(keys, "responseCode", event.getResponseCode());
            put(keys, "serviceName", localPart(event.getServiceName()));
            put(keys, "portName", localPart(event.getPortName()));
            put(keys, "portTypeName", localPart(event.getPortTypeName()));
            if (event.getFullContentFile() != null) {
                put(keys, "fullContentFile", event.getFullContentFile().getAbsolutePath());
            }
            put(keys, "headers", event.getHeaders().toString());
            log.info(getLogMessage(event));
        } finally {
            for (String key : keys) {
                MDC.remove(key);
            }
        }
        
    }
    
    private String localPart(QName name) {
        return name == null ? null : name.getLocalPart();
    }

    private String getLogMessage(LogEvent event) {
        return event.getPayload();
    }
    
    private void put(Set<String> keys, String key, String value) {
        if (value != null) {
            MDC.put(key, value);
            keys.add(key);
        }
    }

}
