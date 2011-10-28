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
package org.apache.cxf.management.interceptor;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.management.persistence.ExchangeData;
import org.apache.cxf.management.persistence.ExchangeDataDAO;
import org.apache.cxf.management.persistence.ExchangeDataFilter;
import org.apache.cxf.management.persistence.ExchangeDataProperty;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.OperationInfo;

public class PersistOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public static final String CXF_CONSOLE_ADDITIONAL_PROPERTY_PREFIX =
        "org.apache.cxf.management.interceptor.prefix";

    private ExchangeDataDAO exchangeDataDAO;

    private ExchangeDataFilter exchangeDataFilter;

    public PersistOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    class PersistOutInterceptorCallback implements CachedOutputStreamCallback {

        private final Message message;
        private final OutputStream origStream;
        private final ExchangeData exchange;

        public PersistOutInterceptorCallback(final Message msg, final OutputStream os,
                                             final ExchangeData ex) {
            this.message = msg;
            this.origStream = os;
            this.exchange = ex;
        }

        public void onClose(CachedOutputStream cos) {
            String id = (String)this.message.getExchange().get(LoggingMessage.ID_KEY);
            if (id == null) {
                id = LoggingMessage.nextId();
                this.message.getExchange().put(LoggingMessage.ID_KEY, id);
            }
            try {
                StringBuilder buffer = new StringBuilder();
                cos.writeCacheTo(buffer, cos.size());
                this.exchange.setResponseSize((int)cos.size());
                this.exchange.setResponse(buffer.toString());
            } catch (Exception ex) {
                // ignore
            }

            try {
                // empty out the cache
                cos.lockOutputStream();
                cos.resetOut(null, false);
            } catch (Exception ex) {
                // ignore
            }
            this.message.setContent(OutputStream.class, this.origStream);

            if (PersistOutInterceptor.this.exchangeDataFilter == null
                || PersistOutInterceptor.this.exchangeDataFilter.shouldPersist(this.exchange)) {
                try {
                    PersistOutInterceptor.this.exchangeDataDAO.save(this.exchange);
                } catch (Throwable e) {

                    e.printStackTrace();
                }
            }
        }

        public void onFlush(CachedOutputStream cos) {

        }
    }

    private static void addProperty(ExchangeData exchange, String key, String value) {

        ExchangeDataProperty exchangeProperty = new ExchangeDataProperty();
        exchangeProperty.setExchangeData(exchange);
        exchangeProperty.setName(key);
        exchangeProperty.setValue(value);

        if (exchange.getProperties() == null) {
            exchange.setProperties(new ArrayList<ExchangeDataProperty>());
        }
        exchange.getProperties().add(exchangeProperty);
    }

    private void addPropertiesFrom(ExchangeData exchange, Message message) {
        for (Map.Entry<String, Object> entry : message.entrySet()) {
            if (entry.getKey().equals(org.apache.cxf.message.Message.ENCODING)) {
                exchange.setEncoding((String)entry.getValue());
            } else if (entry.getKey().equals(org.apache.cxf.message.Message.REQUEST_URI)) {
                exchange.setUri((String)entry.getValue());
            } else if (entry.getKey().equals(org.apache.cxf.message.Message.PROTOCOL_HEADERS)) {

                if (entry.getValue() instanceof Map) {
                    List userAgents = (List)((Map)entry.getValue()).get("user-agent");
                    if (userAgents != null && !userAgents.isEmpty()) {
                        exchange.setUserAgent(userAgents.get(0).toString());
                    }
                }
                if (entry.getValue() != null) {
                    addProperty(exchange, entry.getKey(), entry.getValue().toString());
                }

            } else if (entry.getKey().startsWith("org.apache.cxf.message.Message.")
                && (entry.getValue() instanceof String || entry.getValue() instanceof Integer || entry
                    .getValue() instanceof Boolean)) {
                addProperty(exchange, entry.getKey(), entry.getValue().toString());

            } else if (entry.getKey().startsWith(CXF_CONSOLE_ADDITIONAL_PROPERTY_PREFIX)) {
                addProperty(exchange, entry.getKey().substring(
                                                               CXF_CONSOLE_ADDITIONAL_PROPERTY_PREFIX
                                                               .length()), entry.getValue().toString());

            }
        }
    }

    public void handleMessage(Message message) throws Fault {

        ExchangeData exchangeData = message.getExchange().getInMessage().getContent(ExchangeData.class);
        if (exchangeData != null) {

            final OutputStream os = message.getContent(OutputStream.class);
            if (os == null) {
                return;
            }

            try {

                Service service = message.getExchange().get(Service.class);

                String serviceName = String.valueOf(service.getName());
                OperationInfo opInfo = message.getExchange().get(OperationInfo.class);
                String operationName = opInfo == null ? null : opInfo.getName().getLocalPart();

                if (operationName == null) {
                    Object nameProperty = message.getExchange().get("org.apache.cxf.resource.operation.name");
                    if (nameProperty != null) {
                        operationName = "\"" + nameProperty.toString() + "\"";
                    }
                }

                exchangeData.setServiceName(serviceName);
                exchangeData.setOperation(operationName);

                // add all additional properties

                addPropertiesFrom(exchangeData, message.getExchange().getInMessage());
                addPropertiesFrom(exchangeData, message);

            } catch (Exception e) {
                // TODO: handle exception
                e.printStackTrace();

            }

            // Write the output while caching it for the log message
            final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
            message.setContent(OutputStream.class, newOut);
            newOut.registerCallback(new PersistOutInterceptorCallback(message, os, exchangeData));

            exchangeData.setOutDate(new Date());

            if (message.getContent(Exception.class) != null) {
                exchangeData.setStatus("ERROR");

                Exception exception = message.getContent(Exception.class);
                StringWriter stringWriter = new StringWriter();
                if (exception.getCause() != null) {
                    exchangeData.setExceptionType(exception.getCause().getClass().getName());
                    exception.getCause().printStackTrace(new PrintWriter(stringWriter));
                } else {
                    exchangeData.setExceptionType(exception.getClass().getName());
                    exception.printStackTrace(new PrintWriter(stringWriter));
                }
                exchangeData.setStackTrace(stringWriter.toString());

            } else {
                exchangeData.setStatus("OK");
            }

        }
    }

    public void setExchangeDataDAO(ExchangeDataDAO exchangeDataDAO) {
        this.exchangeDataDAO = exchangeDataDAO;
    }

    public void setExchangeDataFilter(ExchangeDataFilter exchangeDataFilter) {
        this.exchangeDataFilter = exchangeDataFilter;
    }

}
