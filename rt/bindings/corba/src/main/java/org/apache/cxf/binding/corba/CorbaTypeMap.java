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
package org.apache.cxf.binding.corba;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;
import org.apache.cxf.binding.corba.wsdl.NamedType;
import org.omg.CORBA.TypeCode;


public class CorbaTypeMap {

    private String targetNamespace;
    private Map<String, CorbaTypeImpl> typeMap;
    private Map<QName, TypeCode> typeCodeMap;

    public CorbaTypeMap(String namespace) {
        targetNamespace = namespace;
        typeMap = new HashMap<String, CorbaTypeImpl>();
        typeCodeMap = new HashMap<QName, TypeCode>();
    }

    public void addType(String name, CorbaTypeImpl type) {
        typeMap.put(name, type);
    }

    public CorbaTypeImpl getType(String name) {
        assert name != null;

        return typeMap.get(name);
    }

    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void addTypeCode(QName name, TypeCode tc) {
        typeCodeMap.put(name, tc);
    }

    public TypeCode getTypeCode(QName name) {
        return typeCodeMap.get(name);
    }

    // This is used by the Any type when trying to re-construct the type stored inside a 
    // CORBA Any.
    public QName getIdlType(TypeCode tc) {
        String repId = null;
        try {
            repId = tc.id();
        } catch (org.omg.CORBA.TypeCodePackage.BadKind ex) {
            // No id has been set.
            return null;
        }

        if (repId == null) {
            return null;
        }

        Set<Map.Entry<String, CorbaTypeImpl>> mapSet = typeMap.entrySet();
        for (Iterator<Map.Entry<String, CorbaTypeImpl>> i = mapSet.iterator(); i.hasNext();) {
            Map.Entry<String, CorbaTypeImpl> entry = i.next();
            if (entry.getValue() instanceof NamedType) {
                NamedType n = (NamedType)entry.getValue();
                if (n.getRepositoryID().equals(repId)) {
                    return new QName(getTargetNamespace(), entry.getKey());
                }
            }
        }

        return null;
    }
}
