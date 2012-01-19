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
import javax.xml.stream.events.Attribute;

import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaUnionEventProducer extends AbstractStartEndEventProducer {

    static final List<Attribute> IS_NIL_ATTRIBUTE_LIST = new ArrayList<Attribute>();
    static {
        XMLEventFactory factory = XMLEventFactory.newInstance();
        IS_NIL_ATTRIBUTE_LIST.add(factory.createAttribute(
                    new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"), "true"));
    }
    private final boolean isNil;

    public CorbaUnionEventProducer(CorbaObjectHandler h, ServiceInfo sInfo, ORB o) {
        CorbaUnionHandler handler = (CorbaUnionHandler) h;
        serviceInfo = sInfo;
        orb = o;
        name = handler.getName();        
        isNil = checkIsNil(handler);
        if (!isNil) {
            CorbaObjectHandler contents = handler.getValue();
            if (contents != null) {      
                Union unionType = (Union)handler.getType();
                if (unionType.isSetNillable() && unionType.isNillable()) {
                    CorbaTypeEventProducer contentEventProducer = 
                        CorbaHandlerUtils.getTypeEventProducer(contents, serviceInfo, orb);
                    currentEventProducer = new SkipStartEndEventProducer(contentEventProducer, name);
                } else {
                    List<CorbaObjectHandler> list = new ArrayList<CorbaObjectHandler>();
                    list.add(contents);
                    iterator = list.iterator();
                }
            } else if (handler.getSimpleName().equals(handler.getIdlType().getLocalPart() + "_f")) {
                state = states.length;
            }
        }
    }

    private boolean checkIsNil(CorbaUnionHandler handler) {
        boolean isItNil = false;
        Union unionType = (Union)handler.getType();

        if (unionType.isSetNillable() && unionType.isNillable()) {
            CorbaPrimitiveHandler descHandler = (CorbaPrimitiveHandler) handler.getDiscriminator();
            Boolean descValue = (Boolean) descHandler.getValue();
            if (!((Boolean)descValue).booleanValue()) {
                isItNil = true;
            }
        }
        return isItNil;
    }
    
    public List<Attribute> getAttributes() {
        List<Attribute> attributes = IS_NIL_ATTRIBUTE_LIST;
        if (!isNil) {
            attributes = super.getAttributes();
        }
        return attributes;
    }
}
