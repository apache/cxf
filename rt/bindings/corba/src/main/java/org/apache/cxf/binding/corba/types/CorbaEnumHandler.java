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

import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.omg.CORBA.TypeCode;

public class CorbaEnumHandler extends CorbaObjectHandler {

    private String value;
    private long index;
    
    public CorbaEnumHandler(QName enumName, QName enumIdlType, TypeCode enumTC, Object enumType) {
        super(enumName, enumIdlType, enumTC, enumType);
    }
    
    public String getValue() {
        return value;
    }
    
    public void setValue(String val) {
        value = val;
        
        Enum enumType = (Enum)this.type;
        List<Enumerator> enumerators = enumType.getEnumerator();
        index = -1;
        for (int i = 0; i < enumerators.size(); ++i) {
            Enumerator e = enumerators.get(i);
            if (e.getValue().equals(val)) {
                index = i;
                break;
            }
        }
    }
    
    public long getIndex() {
        return index;
    }

    public void clear() {
        value = null;
    }
}
