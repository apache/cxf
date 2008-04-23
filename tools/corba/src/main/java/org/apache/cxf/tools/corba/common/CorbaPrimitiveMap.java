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

package org.apache.cxf.tools.corba.common;

import java.util.HashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.CorbaTypeImpl;

public class CorbaPrimitiveMap extends PrimitiveMapBase {
    
    public CorbaPrimitiveMap() {
        corbaPrimitiveMap = new HashMap<String, QName>();        
        initialiseMap();
    }       

    private void initialiseMap() {
        
        corbaPrimitiveMap.put("string", CorbaConstants.NT_CORBA_STRING); 
        corbaPrimitiveMap.put("boolean", CorbaConstants.NT_CORBA_BOOLEAN);
        corbaPrimitiveMap.put("float", CorbaConstants.NT_CORBA_FLOAT);
        corbaPrimitiveMap.put("double", CorbaConstants.NT_CORBA_DOUBLE);       
        corbaPrimitiveMap.put("dateTime", CorbaConstants.NT_CORBA_DATETIME);
        corbaPrimitiveMap.put("date", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("time", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("gYearMonth", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("gYear", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("gMonthDay", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("gMonth", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("gDay", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("duration", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("anyURI", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("QName", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("normalizedString", CorbaConstants.NT_CORBA_STRING);                
        corbaPrimitiveMap.put("token", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("language", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("NMTOKEN", CorbaConstants.NT_CORBA_STRING);                   
        corbaPrimitiveMap.put("Name", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("NCName", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("ID", CorbaConstants.NT_CORBA_STRING);
        corbaPrimitiveMap.put("integer", CorbaConstants.NT_CORBA_LONGLONG);        
        corbaPrimitiveMap.put("short", CorbaConstants.NT_CORBA_SHORT);
        corbaPrimitiveMap.put("byte", CorbaConstants.NT_CORBA_CHAR);
        corbaPrimitiveMap.put("int", CorbaConstants.NT_CORBA_LONG);
        corbaPrimitiveMap.put("long", CorbaConstants.NT_CORBA_LONGLONG);
        corbaPrimitiveMap.put("nonPositiveInteger", CorbaConstants.NT_CORBA_LONGLONG);
        corbaPrimitiveMap.put("negativeInteger", CorbaConstants.NT_CORBA_LONGLONG);
        corbaPrimitiveMap.put("nonNegativeInteger", CorbaConstants.NT_CORBA_ULONGLONG);                
        corbaPrimitiveMap.put("positiveInteger", CorbaConstants.NT_CORBA_ULONGLONG);                
        corbaPrimitiveMap.put("unsignedInt", CorbaConstants.NT_CORBA_ULONG);
        corbaPrimitiveMap.put("unsignedLong", CorbaConstants.NT_CORBA_ULONGLONG);                
        corbaPrimitiveMap.put("unsignedShort", CorbaConstants.NT_CORBA_USHORT);
        corbaPrimitiveMap.put("unsignedByte", CorbaConstants.NT_CORBA_OCTET);
        corbaPrimitiveMap.put("anyType", CorbaConstants.NT_CORBA_ANY);
        
    }
    
    public Object get(QName key) {
        CorbaTypeImpl corbaTypeImpl = null;

        QName type = corbaPrimitiveMap.get(key.getLocalPart());
        if (type != null) {
            corbaTypeImpl = new CorbaTypeImpl();
            corbaTypeImpl.setQName(type);
            corbaTypeImpl.setType(key);
            corbaTypeImpl.setName(key.getLocalPart());
        }
        
        return corbaTypeImpl;        
    }


}


