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
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public class CorbaPrimitiveHandler extends CorbaObjectHandler {

    private static final int UNSIGNED_MAX = 256; 
    private Object value;
    private boolean objectSet;
    private Any any;
    
    public CorbaPrimitiveHandler(QName primName, QName primIdlType, TypeCode primTC, Object primType) {
        super(primName, primIdlType, primTC, primType);
    }
    
    public Object getValue() {
        return value;
    }
    
    public void setIntoAny(Any val, CorbaStreamable stream, boolean output) {
        any = val;
        if (stream != null) {
            val.insert_Streamable(stream);
        }
        if (output && value != null) {
            switch (this.typeCode.kind().value()) {
            case TCKind._tk_boolean:
                any.insert_boolean((Boolean)value);
                break;
            case TCKind._tk_char:
                any.insert_char(((Character)value).charValue());
                break;
            case TCKind._tk_wchar:
                any.insert_wchar(((Character)value).charValue());
                break;
            case TCKind._tk_octet:
                any.insert_octet(((Byte)value).byteValue());
                break;
            case TCKind._tk_short:
                any.insert_short(((Short)value).shortValue());
                break;
            case TCKind._tk_ushort:
                any.insert_ushort((short)((Integer)value).intValue());
                break;
            case TCKind._tk_long:
                any.insert_long(((Integer)value).intValue());
                break;
            case TCKind._tk_longlong:
                any.insert_longlong(((Long)value).longValue());
                break;
            case TCKind._tk_ulong:
                any.insert_ulong((int)((java.math.BigInteger)value).longValue());
                break;
            case TCKind._tk_ulonglong:
                any.insert_ulonglong(((java.math.BigInteger)value).longValue());
                break;
            case TCKind._tk_float:
                any.insert_float((Float)value);
                break;
            case TCKind._tk_double:
                any.insert_double((Double)value);
                break;
            case TCKind._tk_string:
                any.insert_string((String)value);
                break;
            case TCKind._tk_wstring:
                any.insert_wstring((String)value);
                break;
            default:
                // Default: assume that whatever stored the data will also know how to convert it into what 
                // it needs.
            }
        }
    }

    public void setValue(Object obj) {
        objectSet = true;
        value = obj;
        if (any != null && value != null) {
            setIntoAny(any, null, true);
        }
    }

    public String getDataFromValue() {
        if (!objectSet && any != null) {
            return getDataFromAny();
        }
        String data = "";

        switch (this.typeCode.kind().value()) {

        case TCKind._tk_boolean:
            data = ((Boolean)value).toString();
            break;
        case TCKind._tk_char:
            char charValue = ((Character)value).charValue();
            // outside the normal range it will -256
            data = Byte.toString((byte)(charValue > Byte.MAX_VALUE 
                                                    ? charValue - UNSIGNED_MAX 
                                                    : charValue));
            break;
        case TCKind._tk_wchar:
            data = ((Character)value).toString();
            break;
        case TCKind._tk_octet:
            data = ((Byte)value).toString();
            break;
        case TCKind._tk_short:
            data = ((Short)value).toString();
            break;
        case TCKind._tk_ushort:
            data = ((Integer)value).toString();
            break;
        case TCKind._tk_long:
            data = ((Integer)value).toString();
            break;
        case TCKind._tk_longlong:
            data = ((Long)value).toString();
            break;
        case TCKind._tk_ulong:
        case TCKind._tk_ulonglong:
            data = ((java.math.BigInteger)value).toString();
            break;
        case TCKind._tk_float:
            if (((Float)value).isInfinite()) {
                data = "INF";
            } else {
                data = ((Float)value).toString();
            }
            break;
        case TCKind._tk_double:
            if (((Double)value).isInfinite()) {
                data = "INF";
            } else {
                data = ((Double)value).toString();
            }
            break;
        case TCKind._tk_string:
        case TCKind._tk_wstring:
            data = (String)value;
            break;
        default:
            // Default: assume that whatever stored the data will also know how to convert it into what 
            // it needs.
            data = value.toString();
        }
        return data;
    }
    
    public void setValueFromData(String data) {
        Object obj = null;
        switch (typeCode.kind().value()) {
        case TCKind._tk_boolean:
            obj = Boolean.valueOf(data);
            break;
        case TCKind._tk_char:
            // A char is mapped to a byte, we need it as a character
            Byte byteValue = new Byte(data);
            // for values < 0 + 256 
            // This means that we can directly write out the chars in the normal
            // range 0-127 even when using UTF-8
            obj = new Character((char)(byteValue.byteValue() < 0 
                                         ? byteValue.byteValue() + UNSIGNED_MAX
                                         : byteValue.byteValue()));
            break;
        case TCKind._tk_wchar:
            // A wide char is mapped to a string, we need it as a character
            obj = new Character(data.charAt(0));
            break;
        case TCKind._tk_octet:
            obj = new Short(data).byteValue();
            break;
        case TCKind._tk_short:
            obj = new Short(data);
            break;
        case TCKind._tk_ushort:
            obj = new Integer(data);
            break;
        case TCKind._tk_long:
            obj = new Integer(data);
            break;
        case TCKind._tk_longlong:
            obj = new Long(data);
            break;
        case TCKind._tk_ulong:
        case TCKind._tk_ulonglong:
            obj = new java.math.BigInteger(data);
            break;
        case TCKind._tk_float:
            if ("INF".equals(data)) {
                obj = Float.POSITIVE_INFINITY;
            } else if ("-INF".equals(data)) {
                obj = Float.NEGATIVE_INFINITY;
            } else {
                obj = new Float(data);
            }
            break;
        case TCKind._tk_double:
            if ("INF".equals(data)) {
                obj = Double.POSITIVE_INFINITY;
            } else if ("-INF".equals(data)) {
                obj = Double.NEGATIVE_INFINITY;
            } else {
                obj = new Double(data);
            }
            break;
        case TCKind._tk_string:
        case TCKind._tk_wstring:
            obj = data;
            break;
        default:
            // Default: just store the data we were given.  We'll expect that whatever stored the data
            // will also know how to convert it into what it needs.
            obj = data;
        }
        setValue(obj);
    }
    public String getDataFromAny() {
        String data = "";
        switch (this.typeCode.kind().value()) {
        case TCKind._tk_boolean:
            data = any.extract_boolean() ? "true" : "false";
            break;
        case TCKind._tk_char:
            char charValue = any.extract_char();
            // outside the normal range it will -256
            data = Byte.toString((byte)(charValue > Byte.MAX_VALUE 
                                                    ? charValue - UNSIGNED_MAX 
                                                    : charValue));
            break;
        case TCKind._tk_wchar:
            data = Character.toString(any.extract_wchar());
            break;
        case TCKind._tk_octet:
            data = Byte.toString(any.extract_octet());
            break;
        case TCKind._tk_short:
            data = Short.toString(any.extract_short());
            break;
        case TCKind._tk_ushort:
            data = Integer.toString(any.extract_ushort());
            break;
        case TCKind._tk_long:
            data = Integer.toString(any.extract_long());
            break;
        case TCKind._tk_longlong:
            data = Long.toString(any.extract_longlong());
            break;
        case TCKind._tk_ulong:
            data = Long.toString(any.extract_ulong());
            break;
        case TCKind._tk_ulonglong:
            data = java.math.BigInteger.valueOf(any.extract_ulonglong()).toString();
            break;
        case TCKind._tk_float:
            if (Float.isInfinite(any.extract_float())) {
                data = "INF";
            } else {
                data = Float.toString(any.extract_float());
            }
            break;
        case TCKind._tk_double:
            if (Double.isInfinite(any.extract_double())) {
                data = "INF";
            } else {
                data = Double.toString(any.extract_double());
            }
            break;
        case TCKind._tk_string:
            data = any.extract_string();
            break;
        case TCKind._tk_wstring:
            data = any.extract_wstring();
            break;
        default:
            //should not get here
            throw new RuntimeException("Unknown tc: " + this.typeCode);
        }
        return data;
    }
    
    public void clear() {
        value = null;
    }
}
