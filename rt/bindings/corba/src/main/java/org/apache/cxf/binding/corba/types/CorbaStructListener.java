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

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.binding.corba.wsdl.Struct;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaStructListener extends AbstractCorbaTypeListener {

    private final CorbaTypeMap typeMap;
    private final ORB orb;
    private List<MemberType> structMembers;
    private int memberCount;
    private CorbaTypeListener currentTypeListener;
    private ServiceInfo serviceInfo;
    private int depth;

    public CorbaStructListener(CorbaObjectHandler handler,
                               CorbaTypeMap map,
                               ORB orbRef, ServiceInfo sInfo) {
        super(handler);
        orb = orbRef;
        typeMap = map;
        structMembers = ((Struct) handler.getType()).getMember();
        serviceInfo = sInfo;
    }

    public void processStartElement(QName name) {
        if (depth == 0 && (currentElement != null) && (!currentElement.equals(name))) {
            currentTypeListener = null;
        }
        depth++;
        if (currentTypeListener == null) {
            QName elName = name;
            MemberType member = structMembers.get(memberCount);
            boolean anonType = false;
            if (member.isSetAnonschematype() && member.isAnonschematype()) {
                anonType = true;
                elName = CorbaUtils.getEmptyQName();
                currentElement = null;
            } else {
                currentElement = name;
            }
            currentTypeListener =
                CorbaHandlerUtils.getTypeListener(elName,
                                                  member.getIdltype(),
                                                  typeMap,
                                                  orb,
                                                  serviceInfo);
            currentTypeListener.setNamespaceContext(ctx);
            ((CorbaStructHandler)handler).addMember(currentTypeListener.getCorbaObject());
            memberCount++;
            if (anonType) {
                currentTypeListener.getCorbaObject().setAnonymousType(true);
                currentTypeListener.processStartElement(name);
            }
        } else {
            currentTypeListener.processStartElement(name);
        }
    }

    public void processEndElement(QName name) {
        if (currentTypeListener != null) {
            currentTypeListener.processEndElement(name);
            depth--;
        }
    }

    public void processCharacters(String text) {
        currentTypeListener.processCharacters(text);
    }

    public void processWriteAttribute(String prefix, String namespaceURI, String localName, String value) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteAttribute(prefix, namespaceURI, localName, value);
        } else {
            if ("type".equals(localName)
                && "http://www.w3.org/2001/XMLSchema-instance".equals(namespaceURI)) {
                
                String pfx = value.substring(0, value.indexOf(":"));
                String ns = ctx.getNamespaceURI(pfx);
                QName qn = new QName(ns, 
                                     value.substring(value.indexOf(":") + 1));
                CorbaTypeListener l = CorbaHandlerUtils.getTypeListener(qn,
                                                  qn,
                                                  typeMap,
                                                  orb,
                                                  serviceInfo);
                this.handler = l.getCorbaObject();
                structMembers = ((Struct) handler.getType()).getMember();
            }
        }
    }

    public void processWriteNamespace(String prefix, String namespaceURI) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteNamespace(prefix, namespaceURI);
        }
    }
}
