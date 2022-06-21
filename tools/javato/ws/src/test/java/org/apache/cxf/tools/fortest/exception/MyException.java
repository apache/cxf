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
package org.apache.cxf.tools.fortest.exception;

import jakarta.xml.bind.annotation.XmlType;

@jakarta.xml.ws.WebFault
@XmlType(namespace = "http://cxf.apache.org/test/HelloService",
         name = "MyException",
         propOrder = { "summary", "from", "id", "message" })
public class MyException extends SuperException {
    private static final long serialVersionUID = 8575109064272599936L;
    private String summary;
    private String from;

    public MyException(String message) {
        super(message);
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getSummary() {
        return summary;
    }

    public String getFrom() {
        return from;
    }

}
