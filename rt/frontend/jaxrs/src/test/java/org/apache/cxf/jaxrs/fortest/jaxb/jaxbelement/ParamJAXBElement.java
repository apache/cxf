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
import javax.xml.namespace.QName;

@SuppressWarnings({
    "unchecked", "rawtypes"
})
//CHECKSTYLE:OFF
public class ParamJAXBElement extends JAXBElement<ParamType> {

    private static final long serialVersionUID = 4994571526736505284L;
    protected final static QName NAME = new QName("http://jaxbelement/10", "param");
    
    public ParamJAXBElement(ParamType value) {
        super(NAME, ((Class) ParamType.class), null, value);
    }

    public ParamJAXBElement() {
        super(NAME, ((Class) ParamType.class), null, null);
    }
//CHECKSTYLE:ON
}

