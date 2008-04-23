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


import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.CorbaTypeMap;

import org.omg.CORBA.Any;
import org.omg.CORBA.TypeCode;

public class CorbaAnyHandler extends CorbaObjectHandler {

    private Any value;
    private CorbaObjectHandler containedType;
    private CorbaTypeMap typeMap;
    
    public CorbaAnyHandler(QName anyName, 
                           QName anyIdlType, 
                           TypeCode anyTC, 
                           Object anyType) {
        super(anyName, anyIdlType, anyTC, anyType);
        
        value = null;
    }
    
    public Any getValue() {
        return value;
    }

    public void setAnyContainedType(CorbaObjectHandler obj) {
        containedType = obj;
    }

    public CorbaObjectHandler getAnyContainedType() {
        return containedType;
    }

    public void clear() {
        value = null;
    }

    public void setTypeMap(CorbaTypeMap tm) {
        typeMap = tm;
    }

    public CorbaTypeMap getTypeMap() {
        return typeMap;
    }
    
    public void setValue(Any v) throws CorbaBindingException {
        value = v;
    }
}
