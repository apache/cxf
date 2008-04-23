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
package org.apache.cxf.binding.corba.types;

import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

public class CorbaPrimitiveTypeEventProducer implements CorbaTypeEventProducer {

    int state;
    int[] states = {XMLStreamReader.START_ELEMENT, XMLStreamReader.CHARACTERS, XMLStreamReader.END_ELEMENT};
    final CorbaPrimitiveHandler handler;
    final QName name;

    public CorbaPrimitiveTypeEventProducer(CorbaObjectHandler h) {
        handler = (CorbaPrimitiveHandler) h;
        name = handler.getName();
    }

    public String getLocalName() {        
        return handler.getSimpleName();
    }

    public String getText() {
        return handler.getDataFromValue();
    }

    public int next() {
        return states[state++];
    }

    public QName getName() {
        return name;
    }

    public boolean hasNext() {
        return state < states.length;
    }

    public List<Attribute> getAttributes() {
        return null;
    }

    public List<Namespace> getNamespaces() {
        return null;
    }
}
