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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;

import org.apache.cxf.binding.corba.runtime.CorbaObjectReader;
import org.apache.cxf.binding.corba.utils.CorbaAnyHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;

public class CorbaAnyEventProducer extends AbstractStartEndEventProducer {

    private static final String ANY_TYPE_PREFIX = "anytypens";

    private CorbaAnyHandler handler;

    private List<Attribute> attributes;
    private List<Namespace> namespaces;

    private CorbaObjectHandler containedType;

    public CorbaAnyEventProducer(CorbaObjectHandler h, ServiceInfo info, ORB orbRef) {
        handler = (CorbaAnyHandler)h;
        name = handler.getName();
        orb = orbRef;
        serviceInfo = info;

        containedType = getAnyContainedType(handler.getValue());
        handler.setAnyContainedType(containedType);

        if (containedType != null) {
            QName containedSchemaType = convertIdlToSchemaType(containedType);

            XMLEventFactory factory = XMLEventFactory.newInstance();

            attributes = new ArrayList<Attribute>();
            attributes.add(factory.createAttribute(new QName(W3CConstants.NU_SCHEMA_XSI, "type"), 
                                                   ANY_TYPE_PREFIX + ":" 
                                                   + containedSchemaType.getLocalPart())); 
            namespaces = new ArrayList<Namespace>();
            namespaces.add(factory.createNamespace(ANY_TYPE_PREFIX,
                                                   containedSchemaType.getNamespaceURI()));
        }

        CorbaTypeEventProducer containedProducer = 
            CorbaHandlerUtils.getTypeEventProducer(containedType, serviceInfo, orb);
        if (containedProducer instanceof AbstractStartEndEventProducer) {
            iterator = ((AbstractStartEndEventProducer)containedProducer).getNestedTypes();
        } else {
            List<CorbaTypeEventProducer> prods = new ArrayList<CorbaTypeEventProducer>();
            CorbaSimpleAnyContainedTypeEventProducer simpleProducer = 
                new CorbaSimpleAnyContainedTypeEventProducer(containedProducer.getText());
            prods.add(simpleProducer);
            producers = prods.iterator();
        }
    }

    public List<Attribute> getAttributes() {
        // We should only add attributes if we are asked for the any types attributes.  This will be
        // the case when the current event producer is null.  Otherwise, we are handling the
        // contained type and we CAN'T have the anys attributes used (It causes big problems)
        if (currentEventProducer == null) {
            return attributes;
        } else {
            return null;
        }
    }

    public List<Namespace> getNamespaces() {
        return namespaces;
    }

    private CorbaObjectHandler getAnyContainedType(Any a) {
        CorbaObjectHandler result = null;

        TypeCode tc = a.type();
        QName containedName = new QName("AnyContainedType");
        QName idlType = null;
        if (CorbaUtils.isPrimitiveTypeCode(tc)) {
            idlType = CorbaAnyHelper.getPrimitiveIdlTypeFromTypeCode(tc);
            result = new CorbaPrimitiveHandler(containedName, idlType, tc, null);
        } else if (tc.kind().value() == TCKind._tk_any) {
            idlType = CorbaConstants.NT_CORBA_ANY;
            result = new CorbaAnyHandler(containedName, idlType, tc, null);
            ((CorbaAnyHandler)result).setTypeMap(handler.getTypeMap());
        } else {
            idlType = handler.getTypeMap().getIdlType(tc);
            result = CorbaHandlerUtils.initializeObjectHandler(orb, containedName, idlType,
                                                               handler.getTypeMap(), serviceInfo);
        }

        InputStream is = a.create_input_stream();
        CorbaObjectReader reader = new CorbaObjectReader(is);
        reader.read(result);

        return result;
    }

    private QName convertIdlToSchemaType(CorbaObjectHandler obj) {
        QName idlType = obj.getIdlType();
        QName result = null;
        if (CorbaAnyHelper.isPrimitiveIdlType(idlType)) {
            result = CorbaAnyHelper.convertPrimitiveIdlToSchemaType(obj.getIdlType());
        } else {
            CorbaTypeImpl impl = obj.getType();
            result = impl.getType();
        }

        return result;
    }


    class CorbaSimpleAnyContainedTypeEventProducer implements CorbaTypeEventProducer {
        int state;
        int[] states = {XMLStreamReader.CHARACTERS};
        String value;

        public CorbaSimpleAnyContainedTypeEventProducer(String text) {
            value = text;
            state = 0;
        }

        public String getLocalName() {
            return null;
        }

        public String getText() {
            return value;
        }

        public int next() {
            return states[state++];
        }

        public QName getName() {
            return null;
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
}
