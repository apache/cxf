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
package org.apache.cxf.jaxrs.fortest.jaxb.jaxbelement;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

@XmlRegistry
//CHECKSTYLE:OFF
public class ObjectFactory {

    private static final QName _ParamJAXBElement_QNAME = new QName("http://jaxbelement/10", "comment");


    public ObjectFactory() {
    }

    public ParamType createParamTypeTO() {
        return new ParamType();
    }


    @XmlElementDecl(namespace = "http://jaxbelement/10", name = "param")
    public ParamJAXBElement createParamJAXBElement(ParamType value) {
        return new ParamJAXBElement(value);
    }

    @XmlElementDecl(namespace = "http://jaxbelement/10", name = "comment")
    public JAXBElement<String> createRevocationRemark(String value) {
        return new JAXBElement<String>(_ParamJAXBElement_QNAME, String.class, null, value);
    }

}
//CHECKSTYLE:ON
