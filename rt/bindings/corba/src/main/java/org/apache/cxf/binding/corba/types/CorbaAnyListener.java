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
import org.apache.cxf.binding.corba.utils.CorbaAnyHelper;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;

public class CorbaAnyListener extends AbstractCorbaTypeListener {

    private final CorbaTypeMap typeMap;
    private final ORB orb;
    private ServiceInfo serviceInfo;
    private CorbaTypeListener currentTypeListener;
    private QName containedType;

    public CorbaAnyListener(CorbaObjectHandler h,
                            CorbaTypeMap map,
                            ORB orbRef,
                            ServiceInfo info) {
        super(h);
        orb = orbRef;
        typeMap = map;
        serviceInfo = info;
    }

    public void processStartElement(QName name) {
        if (currentTypeListener == null) {
            currentElement = name;
            QName idlType = convertSchemaToIdlType(containedType);
            currentTypeListener = CorbaHandlerUtils.getTypeListener(name,
                                                                    idlType,
                                                                    typeMap,
                                                                    orb,
                                                                    serviceInfo);

            currentTypeListener.setNamespaceContext(ctx);
            CorbaAnyHandler anyHandler = (CorbaAnyHandler)handler;
            // We need an any during the write.  Since we don't have the orb in the writer, create
            // the any here and use it later.
            anyHandler.setValue(orb.create_any());
            anyHandler.setAnyContainedType(currentTypeListener.getCorbaObject());

            currentTypeListener.processStartElement(name);
        } else {
            currentTypeListener.processStartElement(name);
        }
    }

    public void processEndElement(QName name) {
        if (currentTypeListener != null) {
            currentTypeListener.processEndElement(name);
            CorbaAnyHandler anyHandler = (CorbaAnyHandler)handler;
            anyHandler.setAnyContainedType(currentTypeListener.getCorbaObject());
        }
    }

    public void processCharacters(String text) {
        if (currentTypeListener == null) {
            // This is the case with a primitive type.  Since there aren't any requests to
            // startElement and endElement, we need to do everything in here.
            QName idlType = convertSchemaToIdlType(containedType);
            CorbaTypeListener primitiveListener = CorbaHandlerUtils.getTypeListener(idlType,
                                                                                    idlType,
                                                                                    typeMap,
                                                                                    orb,
                                                                                    serviceInfo);
            primitiveListener.setNamespaceContext(ctx);
            primitiveListener.processCharacters(text);

            CorbaObjectHandler obj = primitiveListener.getCorbaObject();

            Any a = orb.create_any();
            
            CorbaAnyHandler anyHandler = (CorbaAnyHandler)handler;
            anyHandler.setValue(a);
            anyHandler.setAnyContainedType(obj);
        } else {
            currentTypeListener.processCharacters(text);
        }
    }

    public void processWriteAttribute(String prefix, String namespaceURI, String localName, String value) {
        if ("type".equals(localName) && W3CConstants.NU_SCHEMA_XSI.equals(namespaceURI)) {
            int index = value.lastIndexOf(':');
            if (index != -1) {
                String pfx = value.substring(0, index);
                String ns = ctx.getNamespaceURI(pfx);
                containedType = new QName(ns, value.substring(index + 1), pfx);
            } else {
                containedType = new QName(value);
            }
        }
    }


    private QName convertSchemaToIdlType(QName schemaType) {
        QName idlType = null;
        if (CorbaAnyHelper.isPrimitiveSchemaType(schemaType)) {
            idlType = CorbaAnyHelper.convertPrimitiveSchemaToIdlType(schemaType);
        } else {
            // The localpart of the schema QName should match the localpart of the type in the CORBA
            // typemap.
            idlType = new QName(typeMap.getTargetNamespace(), schemaType.getLocalPart());
        }
        
        return idlType;
    }
}
