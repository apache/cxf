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

package org.apache.cxf.javascript.fortest;

import javax.activation.DataHandler;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlType;


/**
 *
 */
@XmlType(namespace = "uri:org.apache.cxf.javascript.testns")
public class MtoMParameterBeanWithDataHandler {
    private String ordinary;
    private DataHandler notXml10;

    public String getOrdinary() {
        return ordinary;
    }
    public void setOrdinary(String ordinary) {
        this.ordinary = ordinary;
    }

    @XmlMimeType("text/plain;charset=utf-8")
    public DataHandler getNotXml10() {
        return notXml10;
    }
    public void setNotXml10(DataHandler notXml10) {
        this.notXml10 = notXml10;
    }
}

