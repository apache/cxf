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
import org.apache.cxf.binding.corba.wsdl.Exception;
import org.apache.cxf.binding.corba.wsdl.MemberType;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaExceptionListener extends AbstractCorbaTypeListener {

    private final CorbaTypeMap typeMap;
    private final ORB orb;
    private final List<MemberType> exMembers;
    private int memberCount;
    private CorbaTypeListener currentTypeListener;
    private QName memberElement;
    private ServiceInfo sInfo;


    public CorbaExceptionListener(CorbaObjectHandler handler,
                                  CorbaTypeMap map,
                                  ORB orbRef, 
                                  ServiceInfo serviceInfo) {
        super(handler);
        orb = orbRef;
        typeMap = map;
        sInfo = serviceInfo;
        exMembers = ((Exception) handler.getType()).getMember();
    }

    public void processStartElement(QName name) {
        //REVISIT, assume only elements not attrs
        if (currentTypeListener == null) {
            memberElement = name;
            currentTypeListener =
                CorbaHandlerUtils.getTypeListener(name,
                                                  exMembers.get(memberCount).getIdltype(),
                                                  typeMap,
                                                  orb, 
                                                  sInfo);
            currentTypeListener.setNamespaceContext(ctx);
            ((CorbaExceptionHandler)handler).addMember(currentTypeListener.getCorbaObject());
            memberCount++;
        } else {
            currentTypeListener.processStartElement(name);
        }
    }

    public void processEndElement(QName name) {
        if (currentTypeListener != null) {
            currentTypeListener.processEndElement(name);
        }
        if (memberElement.equals(name)) {
            currentTypeListener = null;
        }
    }

    public void processCharacters(String text) {
        currentTypeListener.processCharacters(text);
    }

    public void processWriteAttribute(String prefix, String namespaceURI, String localName, String value) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteAttribute(prefix, namespaceURI, localName, value);
        }
    }

    public void processWriteNamespace(String prefix, String namespaceURI) {
        if (currentTypeListener != null) {
            currentTypeListener.processWriteNamespace(prefix, namespaceURI);
        }
    }

}
