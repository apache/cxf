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
import org.apache.cxf.binding.corba.wsdl.Anonsequence;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.Sequence;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaSequenceListener extends AbstractCorbaTypeListener {

    private final CorbaSequenceHandler value;
    private final QName seqElementType;
    private final ORB orb;
    private final CorbaTypeMap typeMap;
    private CorbaTypeListener currentTypeListener;
    private ServiceInfo serviceInfo;
    private int depth;

    public CorbaSequenceListener(CorbaObjectHandler handler,
                                 CorbaTypeMap map,
                                 ORB orbRef, 
                                 ServiceInfo sInfo) {
        super(handler);
        value = (CorbaSequenceHandler) handler;
        orb = orbRef;
        typeMap = map;
        serviceInfo = sInfo;
        CorbaTypeImpl seqType = handler.getType();
        QName elementName;
        if (seqType instanceof Anonsequence) {
            Anonsequence anonSeqType = (Anonsequence) seqType;
            seqElementType = anonSeqType.getElemtype();
            elementName = anonSeqType.getElemname();
        } else {
            Sequence type = (Sequence) seqType;
            seqElementType = type.getElemtype();
            elementName = type.getElemname();
        }
        CorbaObjectHandler template = CorbaHandlerUtils.initializeObjectHandler(orb,
                                                                                elementName,
                                                                                seqElementType,
                                                                                typeMap,
                                                                                serviceInfo);        
        value.setTemplateElement(template);
    }

    public void processStartElement(QName name) {
        depth++;
        if (currentTypeListener == null) {
            currentElement = name;
            currentTypeListener =
                CorbaHandlerUtils.getTypeListener(name,
                                                  seqElementType,
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
            depth--;
            if (depth == 0 && currentElement.equals(name)) {
                currentTypeListener = null;
            }
        }
    }

    public void processCharacters(String text) {
        if (currentTypeListener == null) {
            // primitive sequence
            CorbaTypeListener primitiveListener = 
                CorbaHandlerUtils.getTypeListener(value.getName(),
                                                  seqElementType,
                                                  typeMap,
                                                  orb,
                                                  serviceInfo);
            primitiveListener.setNamespaceContext(ctx);
            value.addElement(primitiveListener.getCorbaObject());
            primitiveListener.processCharacters(text);
        } else {
            currentTypeListener.processCharacters(text);
        }
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
