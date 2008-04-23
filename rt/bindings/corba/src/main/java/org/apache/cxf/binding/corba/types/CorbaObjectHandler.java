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

import org.apache.cxf.binding.corba.CorbaStreamable;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;

import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

// This class serves as a base for all other specific object type handlers and 
// provides basic functionality that is common for all objects.
public class CorbaObjectHandler {

    protected QName name;
    protected QName idlType;
    protected TypeCode typeCode;
    protected CorbaTypeImpl type;
    protected boolean isAnon;
    protected boolean isRecursive;

    public CorbaObjectHandler() {
    }
    
    public CorbaObjectHandler(QName objName, QName objIdlType, TypeCode objTC, Object objType) {
        name = objName;
        idlType = objIdlType;
        typeCode = objTC;
        type = (CorbaTypeImpl)objType;
    }
    
    public QName getName() {
        return name;
    }
    
    public String getSimpleName() {
        return name.getLocalPart();
    }
    
    public QName getIdlType() {
        return idlType;
    }
    
    public TypeCode getTypeCode() {
        return typeCode;
    }
    
    public TCKind getTypeCodeKind() {
        return typeCode.kind();
    }
    
    public CorbaTypeImpl getType() {
        return type;
    }

    public void setAnonymousType(boolean anon) {
        isAnon = anon;
    }

    public boolean isAnonymousType() {
        return isAnon;
    }

    public void setRecursive(boolean rec) {
        isRecursive = rec;
    }

    public boolean isRecursive() {
        return isRecursive;
    }

    public void clear() {        
    }
    
    public void setIntoAny(Any value, CorbaStreamable stream, boolean output) {
        value.insert_Streamable(stream);
    }
}
