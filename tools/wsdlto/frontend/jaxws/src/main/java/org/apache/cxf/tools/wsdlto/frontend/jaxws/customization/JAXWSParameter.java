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
package org.apache.cxf.tools.wsdlto.frontend.jaxws.customization;

import javax.xml.namespace.QName;

public class JAXWSParameter {
    private String name;
    private String part;
    private QName eleName;
    private String messageName;

    public JAXWSParameter(String msgName, String part, QName elementName, String name) {
        this.messageName = msgName;
        this.part = part;
        this.eleName = elementName;
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPart(String part) {
        this.part = part;
    }

    public String getPart() {
        return part;
    }

    public void setElementName(QName elementName) {
        this.eleName = elementName;
    }

    public QName getElementName() {
        return eleName;
    }

    public String getMessageName() {
        return this.messageName;
    }

    public void setMessageName(String msgName) {
        this.messageName = msgName;
    }



}
