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
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.ws.commons.schema.constants.Constants;

public class XmlSchemaPrimitiveMap {
    private Map<QName, QName> xmlSchemaPrimitiveMap;
    
    public XmlSchemaPrimitiveMap() {
        xmlSchemaPrimitiveMap = new HashMap<QName, QName>();
        initializeMap();
    }
    
    private void initializeMap() {
        //<base_type_spec>
        // <floating_pt_type> - OMG Syntax and semantics - CORBA v3.0 
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_FLOAT, Constants.XSD_FLOAT); 
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_DOUBLE, Constants.XSD_DOUBLE);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_LONGDOUBLE, Constants.XSD_DOUBLE);
     
        // <integer_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_SHORT, Constants.XSD_SHORT);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_LONG, Constants.XSD_INT);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_LONGLONG, Constants.XSD_LONG);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_USHORT, Constants.XSD_UNSIGNEDSHORT);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_ULONG, Constants.XSD_UNSIGNEDINT);
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_ULONGLONG, Constants.XSD_UNSIGNEDLONG);
        
        // <char_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_CHAR, Constants.XSD_BYTE);
        
        // <wide_char_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_WCHAR, Constants.XSD_STRING);
        
        // <boolean_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_BOOLEAN, Constants.XSD_BOOLEAN);
        
        // <octet_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_OCTET, Constants.XSD_UNSIGNEDBYTE);
        
        // <any_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_ANY, Constants.XSD_ANYTYPE);
        
        // <object_type>
        
        // <value_base_type>
        
        //<template_type_spec>
        // <string_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_STRING, Constants.XSD_STRING);
        // <wide_string_type>
        xmlSchemaPrimitiveMap.put(CorbaConstants.NT_CORBA_WSTRING, Constants.XSD_STRING);
    }
    
    public QName get(QName key) {
        return xmlSchemaPrimitiveMap.get(key);
    }
}
