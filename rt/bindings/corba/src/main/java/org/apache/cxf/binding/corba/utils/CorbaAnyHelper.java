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

package org.apache.cxf.binding.corba.utils;

import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public final class CorbaAnyHelper {

    private static final Map<QName, QName> SCHEMA_TO_IDL_TYPES = new HashMap<>();
    private static final Map<QName, QName> IDL_TO_SCHEMA_TYPES = new HashMap<>();

    private static Constructor<?> fixedAnyConstructor;

    private CorbaAnyHelper() {
        //utility class
    }

    public static Any createAny(ORB orb, Bus bus) {
        Any value = orb.create_any();
        if ("com.sun.corba.se.impl.corba.AnyImpl".equals(value.getClass().getName())) {
            value = createFixedAny(orb, value, bus);
        }
        return value;
    }

    public static boolean isPrimitiveSchemaType(QName schemaType) {
        return SCHEMA_TO_IDL_TYPES.get(schemaType) != null;
    }

    public static boolean isPrimitiveIdlType(QName idlType) {
        return IDL_TO_SCHEMA_TYPES.get(idlType) != null;
    }

    public static QName convertPrimitiveSchemaToIdlType(QName schemaType) {
        return SCHEMA_TO_IDL_TYPES.get(schemaType);
    }

    public static QName convertPrimitiveIdlToSchemaType(QName idlType) {
        return IDL_TO_SCHEMA_TYPES.get(idlType);
    }

    public static QName getPrimitiveIdlTypeFromTypeCode(TypeCode tc) {
        TCKind type = tc.kind();
        QName result = null;

        switch(type.value()) {
        case TCKind._tk_boolean:
            result = CorbaConstants.NT_CORBA_BOOLEAN;
            break;
        case TCKind._tk_char:
            result = CorbaConstants.NT_CORBA_CHAR;
            break;
        case TCKind._tk_wchar:
            result = CorbaConstants.NT_CORBA_WCHAR;
            break;
        case TCKind._tk_octet:
            result = CorbaConstants.NT_CORBA_OCTET;
            break;
        case TCKind._tk_short:
            result = CorbaConstants.NT_CORBA_SHORT;
            break;
        case TCKind._tk_ushort:
            result = CorbaConstants.NT_CORBA_USHORT;
            break;
        case TCKind._tk_long:
            result = CorbaConstants.NT_CORBA_LONG;
            break;
        case TCKind._tk_ulong:
            result = CorbaConstants.NT_CORBA_ULONG;
            break;
        case TCKind._tk_longlong:
            result = CorbaConstants.NT_CORBA_LONGLONG;
            break;
        case TCKind._tk_ulonglong:
            result = CorbaConstants.NT_CORBA_ULONGLONG;
            break;
        case TCKind._tk_float:
            result = CorbaConstants.NT_CORBA_FLOAT;
            break;
        case TCKind._tk_double:
            result = CorbaConstants.NT_CORBA_DOUBLE;
            break;
        case TCKind._tk_string:
            result = CorbaConstants.NT_CORBA_STRING;
            break;
        case TCKind._tk_wstring:
            result = CorbaConstants.NT_CORBA_WSTRING;
            break;
        default:
            result = null;
        }

        return result;
    }

    public static void insertPrimitiveIntoAny(Any a, CorbaPrimitiveHandler primitive)
        throws CorbaBindingException {
        assert primitive != null;
        switch (primitive.getTypeCode().kind().value()) {
        case TCKind._tk_boolean:
            a.insert_boolean((Boolean)primitive.getValue());
            break;
        case TCKind._tk_char:
            a.insert_char((Character)primitive.getValue());
            break;
        case TCKind._tk_wchar:
            a.insert_wchar((Character)primitive.getValue());
            break;
        case TCKind._tk_octet:
            a.insert_octet((Byte)primitive.getValue());
            break;
        case TCKind._tk_short:
            a.insert_short((Short)primitive.getValue());
            break;
        case TCKind._tk_ushort:
            a.insert_ushort(((Integer)primitive.getValue()).shortValue());
            break;
        case TCKind._tk_long:
            a.insert_long((Integer)primitive.getValue());
            break;
        case TCKind._tk_ulong:
            a.insert_ulong(((BigInteger)primitive.getValue()).intValue());
            break;
        case TCKind._tk_longlong:
            a.insert_longlong((Long)primitive.getValue());
            break;
        case TCKind._tk_ulonglong:
            a.insert_ulonglong(((BigInteger)primitive.getValue()).intValue());
            break;
        case TCKind._tk_float:
            a.insert_float((Float)primitive.getValue());
            break;
        case TCKind._tk_double:
            a.insert_double((Double)primitive.getValue());
            break;
        case TCKind._tk_string:
            a.insert_string((String)primitive.getValue());
            break;
        case TCKind._tk_wstring:
            a.insert_wstring((String)primitive.getValue());
            break;
        default:
            throw new CorbaBindingException("Unable to insert type into any.  Kind = "
                                            + primitive.getTypeCode().kind().value());
        }
    }

    public static void extractPrimitiveFromAny(Any a, CorbaPrimitiveHandler primitive) {
        assert primitive != null;
        switch (primitive.getTypeCode().kind().value()) {
        case TCKind._tk_boolean:
            primitive.setValue(Boolean.valueOf(a.extract_boolean()));
            break;
        case TCKind._tk_char:
            primitive.setValue(Character.valueOf(a.extract_char()));
            break;
        case TCKind._tk_wchar:
            primitive.setValue(Character.valueOf(a.extract_wchar()));
            break;
        case TCKind._tk_octet:
            primitive.setValue(Byte.valueOf(a.extract_octet()));
            break;
        case TCKind._tk_short:
            primitive.setValue(Short.valueOf(a.extract_short()));
            break;
        case TCKind._tk_ushort:
            primitive.setValue(Integer.valueOf(a.extract_ushort()));
            break;
        case TCKind._tk_long:
            primitive.setValue(Integer.valueOf(a.extract_long()));
            break;
        case TCKind._tk_ulong:
            primitive.setValue(BigInteger.valueOf(a.extract_ulong()));
            break;
        case TCKind._tk_longlong:
            primitive.setValue(Long.valueOf(a.extract_longlong()));
            break;
        case TCKind._tk_ulonglong:
            primitive.setValue(BigInteger.valueOf(a.extract_ulonglong()));
            break;
        case TCKind._tk_float:
            primitive.setValue(Float.valueOf(a.extract_float()));
            break;
        case TCKind._tk_double:
            primitive.setValue(Double.valueOf(a.extract_double()));
            break;
        case TCKind._tk_string:
            primitive.setValue(a.extract_string());
            break;
        case TCKind._tk_wstring:
            primitive.setValue(a.extract_wstring());
            break;
        default:
            throw new CorbaBindingException("Unable to extract type from any.  Kind = "
                                            + primitive.getTypeCode().kind().value());
        }
    }

    // NOTE: We have an issue when we get a schema type of String.  We don't know whether this means
    // that we have an IDL type of wchar, string, or wstring.  To be safe, we'll simply use a CORBA
    // string for the mapping.
    static {
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_BOOLEAN, CorbaConstants.NT_CORBA_BOOLEAN);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_BYTE, CorbaConstants.NT_CORBA_CHAR);
        //SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_STRING, CorbaConstants.NT_CORBA_WCHAR);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_UBYTE, CorbaConstants.NT_CORBA_OCTET);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_SHORT, CorbaConstants.NT_CORBA_SHORT);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_USHORT, CorbaConstants.NT_CORBA_USHORT);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_INT, CorbaConstants.NT_CORBA_LONG);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_UINT, CorbaConstants.NT_CORBA_ULONG);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_LONG, CorbaConstants.NT_CORBA_LONGLONG);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_ULONG, CorbaConstants.NT_CORBA_ULONGLONG);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_FLOAT, CorbaConstants.NT_CORBA_FLOAT);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_DOUBLE, CorbaConstants.NT_CORBA_DOUBLE);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_STRING, CorbaConstants.NT_CORBA_STRING);
        //SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_STRING, CorbaConstants.NT_CORBA_WSTRING);
        SCHEMA_TO_IDL_TYPES.put(W3CConstants.NT_SCHEMA_ANYTYPE, CorbaConstants.NT_CORBA_ANY);
    }

    static {
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_BOOLEAN, W3CConstants.NT_SCHEMA_BOOLEAN);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_CHAR, W3CConstants.NT_SCHEMA_BYTE);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_WCHAR, W3CConstants.NT_SCHEMA_STRING);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_OCTET, W3CConstants.NT_SCHEMA_UBYTE);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_SHORT, W3CConstants.NT_SCHEMA_SHORT);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_USHORT, W3CConstants.NT_SCHEMA_USHORT);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_LONG, W3CConstants.NT_SCHEMA_INT);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_ULONG, W3CConstants.NT_SCHEMA_UINT);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_LONGLONG, W3CConstants.NT_SCHEMA_LONG);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_ULONGLONG, W3CConstants.NT_SCHEMA_ULONG);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_FLOAT, W3CConstants.NT_SCHEMA_FLOAT);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_DOUBLE, W3CConstants.NT_SCHEMA_DOUBLE);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_STRING, W3CConstants.NT_SCHEMA_STRING);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_WSTRING, W3CConstants.NT_SCHEMA_STRING);
        IDL_TO_SCHEMA_TYPES.put(CorbaConstants.NT_CORBA_ANY, W3CConstants.NT_SCHEMA_ANYTYPE);
    }

    private static synchronized Any createFixedAny(ORB orb, Any any, Bus bus) {
        if (fixedAnyConstructor == null) {
            CorbaFixedAnyImplClassCreator corbaFixedAnyImplClassCreator =
                    bus.getExtension(CorbaFixedAnyImplClassCreator.class);
            Class<?> c = corbaFixedAnyImplClassCreator.createFixedAnyClass();
            try {
                fixedAnyConstructor = c.getConstructor(ORB.class, Any.class);
            } catch (Exception e) {
                //shouldn't happen since we generated that constructor
            }
        }
        try {
            return (Any)fixedAnyConstructor.newInstance(orb, any);
        } catch (Exception e) {
            return any;
        }
    }
}
