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

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.wsdl.Anonarray;
import org.apache.cxf.binding.corba.wsdl.Array;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaArrayListener extends AbstractCorbaTypeListener {

    private final CorbaArrayHandler value;
    private final QName arrayElementType;
    private final ORB orb;
    private final CorbaTypeMap typeMap;
    private QName currentElName;
    private CorbaTypeListener currentTypeListener;
    private ServiceInfo serviceInfo;
    
    public CorbaArrayListener(CorbaObjectHandler handler,
                              CorbaTypeMap map,
                              ORB orbRef, ServiceInfo sInfo) {
        super(handler);
        value = (CorbaArrayHandler) handler;
        orb = orbRef;
        typeMap = map;
        serviceInfo = sInfo;
        Object arrayType = handler.getType();
        if (arrayType instanceof Anonarray) {
            Anonarray anonType = (Anonarray) arrayType;
            arrayElementType = anonType.getElemtype();
        } else {
            Array type = (Array) arrayType;
            arrayElementType = type.getElemtype();
        }
    }

    public void processStartElement(QName name) {
        if (currentTypeListener == null) {
            currentElName = name;
            currentTypeListener =
                CorbaHandlerUtils.getTypeListener(name,
                                                  arrayElementType,
                                                  typeMap,
                                                  orb,
                                                  serviceInfo);
            currentTypeListener.setNamespaceContext(ctx);
            value.addElement(currentTypeListener.getCorbaObject());
        } else {
            currentTypeListener.processStartElement(name);
        }
    }

    public void processEndElement(QName name) {
        if (currentTypeListener != null) {
            currentTypeListener.processEndElement(name);
            if (currentElName.equals(name)) {
                currentTypeListener = null;
            }
        }
    }

    public void processCharacters(String text) {
        currentTypeListener.processCharacters(text);
    }

    public void processWriteAttribute(String prefix, String namespaceURI, String localName, String val) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteAttribute(prefix, namespaceURI, localName, val);
        }
    }

    public void processWriteNamespace(String prefix, String namespaceURI) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteNamespace(prefix, namespaceURI);
        }
    }
}
