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

import javax.xml.bind.DatatypeConverter;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.omg.CORBA.TypeCode;

public class CorbaOctetSequenceHandler extends CorbaObjectHandler {

    private byte[] value;
    private boolean isBase64Octets;
    
    public CorbaOctetSequenceHandler(QName primName,
                                     QName primIdlType,
                                     TypeCode primTC,
                                     Object primType) {
        super(primName, primIdlType, primTC, primType);
        isBase64Octets = getType().getType().equals(W3CConstants.NT_SCHEMA_BASE64);
    }
    
    public byte[] getValue() {
        return value;
    }    
    
    public void setValue(byte[] obj) {
        value = obj;
    }

    public String getDataFromValue() {
        String result;
        if (isBase64Octets) {
            result = new String(DatatypeConverter.printBase64Binary(value));
        } else {
            result = new String(DatatypeConverter.printHexBinary(value));
        }
        return result;
    }
    
    public void setValueFromData(String data) {
        try {
            if (isBase64Octets) {
                value = DatatypeConverter.parseBase64Binary(data);
            } else {
                value = DatatypeConverter.parseHexBinary(data);
            }
        } catch (Exception ex) {
            throw new CorbaBindingException("Not able to parse the octet sequence", ex);
        }
    }
    
    public void clear() {
        value = null;
    }
}
