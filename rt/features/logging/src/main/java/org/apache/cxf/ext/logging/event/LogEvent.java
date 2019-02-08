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

import java.io.File;
import java.util.Map;

import javax.xml.namespace.QName;

public final class LogEvent {
    public static final String KEY_EXCHANGE_ID = "exchangeId";
    private String messageId;
    private String exchangeId;
    private EventType type;
    private String address;
    private String contentType;
    private String encoding;
    private String httpMethod;
    private String responseCode;
    private String principal;
    private QName serviceName; // Only for SOAP
    private QName portName;
    private QName portTypeName;
    private String operationName;
    private Map<String, String> headers;
    private boolean binaryContent;
    private boolean multipartContent;
    private String payload;
    private boolean truncated;
    private File fullContentFile;


    public LogEvent() {
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String id) {
        this.messageId = id;
    }

    public String getExchangeId() {
        return exchangeId;
    }

    public void setExchangeId(String exchangeId) {
        this.exchangeId = exchangeId;
    }

    public EventType getType() {
        return type;
    }

    public void setType(EventType type) {
        this.type = type;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getEncoding() {
        return encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public QName getServiceName() {
        return serviceName;
    }

    public void setServiceName(QName serviceName) {
        this.serviceName = serviceName;
    }

    public QName getPortName() {
        return portName;
    }

    public void setPortName(QName portName) {
        this.portName = portName;
    }

    public QName getPortTypeName() {
        return portTypeName;
    }

    public void setPortTypeName(QName portTypeName) {
        this.portTypeName = portTypeName;
    }

    public String getOperationName() {
        return operationName;
    }

    public void setOperationName(String operationName) {
        this.operationName = operationName;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public boolean isBinaryContent() {
        return binaryContent;
    }

    public void setBinaryContent(boolean binaryContent) {
        this.binaryContent = binaryContent;
    }

    public boolean isMultipartContent() {
        return multipartContent;
    }

    public void setMultipartContent(boolean multipartContent) {
        this.multipartContent = multipartContent;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public boolean isTruncated() {
        return truncated;
    }

    public void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    public File getFullContentFile() {
        return fullContentFile;
    }

    public void setFullContentFile(File fullContentFile) {
        this.fullContentFile = fullContentFile;
    }


}
