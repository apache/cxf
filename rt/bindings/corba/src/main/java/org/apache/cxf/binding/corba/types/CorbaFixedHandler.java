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

import java.math.BigDecimal;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.corba.wsdl.Anonfixed;
import org.apache.cxf.binding.corba.wsdl.Fixed;

import org.omg.CORBA.TypeCode;

public final class CorbaFixedHandler extends CorbaObjectHandler {

    private final long digits;
    private final long scale;
    private BigDecimal value;
    
    public CorbaFixedHandler(QName fixedName, QName fixedIdlType, TypeCode fixedTC, Object fixedType) {
        super(fixedName, fixedIdlType, fixedTC, fixedType);
        
        if (fixedType instanceof Fixed) {
            digits = ((Fixed)fixedType).getDigits();
            scale = ((Fixed)fixedType).getScale();
        } else if (fixedType instanceof Anonfixed) {
            digits = ((Anonfixed)fixedType).getDigits();
            scale = ((Anonfixed)fixedType).getScale();
        } else {
            // This should never happen
            digits = 0;
            scale = 0;
        }
    }
    
    public long getDigits() {
        return digits;
    }
    
    public long getScale() {
        return scale;
    }
    
    public BigDecimal getValue() {
        return value;
    }
    
    public String getValueData() {
        return value.toString();
    }
    
    public void setValue(BigDecimal val) {
        value = val;
    }
    
    public void setValueFromData(String data) {
        value = new BigDecimal(data);
    }

    public void clear() {
        value = null;
    }  
}
