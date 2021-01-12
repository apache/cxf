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
package org.apache.cxf.jaxb;

import jakarta.xml.bind.annotation.XmlAccessOrder;
import jakarta.xml.bind.annotation.XmlAccessorOrder;
import jakarta.xml.bind.annotation.XmlAttribute;

@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class OrderException extends Exception {

    private static final long serialVersionUID = 1L;

    @XmlAttribute(name = "mappedField")
    private static final String MAPPED_FIELD = "MappedField";

    private transient int transientValue;

    private String info1;

    private String info2;

    private String aValue;

    private int intVal;

    private String detail;



    public OrderException(String message) {
        super(message);
    }


    public String getAValue() {
        return aValue;
    }

    public void setAValue(String value) {
        this.aValue = value;
    }

    public String getInfo1() {
        return info1;
    }

    public void setInfo1(String info1) {
        this.info1 = info1;
    }

    public String getInfo2() {
        return info2;
    }

    public void setInfo2(String info2) {
        this.info2 = info2;
    }


    public int getIntVal() {
        return intVal;
    }

    public void setIntVal(int intVal) {
        this.intVal = intVal;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }


    int getTransientValue() {
        return transientValue;
    }


    void setTransientValue(int transientValue) {
        this.transientValue = transientValue;
    }

    public String mappedField() {
        return MAPPED_FIELD;
    }


}
