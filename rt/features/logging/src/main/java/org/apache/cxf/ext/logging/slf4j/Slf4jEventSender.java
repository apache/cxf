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
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.slf4j.event.Level;

public class Slf4jEventSender implements LogEventSender {

    private Level loggingLevel = Level.INFO;

    @Override
    public void send(LogEvent event) {
        Logger log = getLogger(event);
        Set<String> keys = new HashSet<>();
        try {
            fillMDC(event, keys);
            performLogging(log, MarkerFactory.getMarker(event.getServiceName() != null ? "SOAP" : "REST"),
                     getLogMessage(event));
        } finally {
            for (String key : keys) {
                MDC.remove(key);
            }
        }

    }

    protected Logger getLogger(final LogEvent event) {
        final String cat = "org.apache.cxf.services." + event.getPortTypeName().getLocalPart() + "." + event.getType();
        return LoggerFactory.getLogger(cat);
    }

    protected void fillMDC(final LogEvent event, final Set<String> keys) {
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
    }

    /**
     * Override this to easily change the logging level etc.
     */
    protected void performLogging(Logger log, Marker marker, String logMessage) {
        if (loggingLevel == Level.INFO) {
            log.info(marker, logMessage);
        } else if (loggingLevel == Level.DEBUG) {
            log.debug(marker, logMessage);
        } else if (loggingLevel == Level.ERROR) {
            log.error(marker, logMessage);
        } else if (loggingLevel == Level.TRACE) {
            log.trace(marker, logMessage);
        } else if (loggingLevel == Level.WARN) {
            log.warn(marker, logMessage);
        } else {
            log.info(marker, logMessage);
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

    public void setLoggingLevel(Level loggingLevel) {
        this.loggingLevel = loggingLevel;
    }

    public void setLoggingLevel(String loggingLevel) {
        this.loggingLevel = Level.valueOf(loggingLevel);
    }

}
