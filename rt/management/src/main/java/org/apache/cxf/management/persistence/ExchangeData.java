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
package org.apache.cxf.management.persistence;

import java.util.Date;
import java.util.List;

public class ExchangeData {

    private String encoding;

    private String exceptionType;

    private Integer id;

    private Date inDate;

    private String operation;

    private Date outDate;

    private List<ExchangeDataProperty> properties;

    private String request;

    private Integer requestSize;

    private String response;

    private Integer responseSize;

    private String serviceName;

    private String stackTrace;

    private String status;

    private String uri;

    private String userAgent;

    public String getEncoding() {
        return this.encoding;
    }

    public String getExceptionType() {
        return this.exceptionType;
    }

    public Integer getId() {
        return this.id;
    }

    public Date getInDate() {
        return this.inDate;
    }

    public String getOperation() {
        return this.operation;
    }

    public Date getOutDate() {
        return this.outDate;
    }

    public List<ExchangeDataProperty> getProperties() {
        return this.properties;
    }

    public String getRequest() {
        return this.request;
    }

    public Integer getRequestSize() {
        return this.requestSize;
    }

    public String getResponse() {
        return this.response;
    }

    public Integer getResponseSize() {
        return this.responseSize;
    }

    public String getServiceName() {
        return this.serviceName;
    }

    public String getStackTrace() {
        return this.stackTrace;
    }

    public String getStatus() {
        return this.status;
    }

    public String getUri() {
        return this.uri;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setExceptionType(String exceptionType) {
        this.exceptionType = exceptionType;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setInDate(Date inDate) {
        this.inDate = inDate;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setOutDate(Date outDate) {
        this.outDate = outDate;
    }

    public void setProperties(List<ExchangeDataProperty> properties) {
        this.properties = properties;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setRequestSize(Integer requestSize) {
        this.requestSize = requestSize;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void setResponseSize(Integer responseSize) {
        this.responseSize = responseSize;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setStackTrace(String stackTrace) {
        this.stackTrace = stackTrace;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

}
