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

package org.apache.cxf.binding.corba.runtime;


import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.types.CorbaHandlerUtils;
import org.apache.cxf.binding.corba.types.CorbaTypeListener;
import org.apache.cxf.binding.corba.wsdl.RaisesType;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;

public class CorbaFaultStreamWriter extends CorbaStreamWriter {

    private RaisesType exType;

    public CorbaFaultStreamWriter(ORB orbRef,
                                  RaisesType raisesType,
                                  CorbaTypeMap map,
                                  ServiceInfo sinfo) {
        super(orbRef, map, sinfo);
        exType = raisesType;
        listeners = new CorbaTypeListener[1];
    }

    protected void setCurrentTypeListener(QName name) throws XMLStreamException {
        QName idlType = exType.getException();
        currentTypeListener = CorbaHandlerUtils.getTypeListener(name, idlType, typeMap, orb, serviceInfo);
        currentTypeListener.setNamespaceContext(ctx);
        listeners[0] = currentTypeListener;
    }

}
