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
package org.apache.cxf.systest.jaxb.service;

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "fault")
@XmlType(name = "", propOrder = {
    "message", "data"
}, namespace = "")
public class PropertyOrderException extends Exception implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name = "message", required = true, nillable = true)
    private String message;

    @XmlElement(name = "data", required = true, nillable = true)
    private ErrorData data;

    public PropertyOrderException() {
        this.data = new ErrorData();
        this.message = null;
    }

    public PropertyOrderException(ErrorData data) {
        this.data = data;
        this.message = null;
    }

    public PropertyOrderException(String message) {
        this.data = null;
        this.message = message;
    }

    public String getMessage() {
        return this.message;
    }

    public ErrorData getData() {
        return data;
    }

    public void setData(ErrorData data) {
        this.data = data;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
