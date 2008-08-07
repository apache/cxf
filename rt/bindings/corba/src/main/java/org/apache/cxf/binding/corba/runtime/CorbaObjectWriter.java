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
package org.apache.cxf.binding.corba.runtime;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.types.CorbaAnyHandler;
import org.apache.cxf.binding.corba.types.CorbaArrayHandler;
import org.apache.cxf.binding.corba.types.CorbaEnumHandler;
import org.apache.cxf.binding.corba.types.CorbaExceptionHandler;
import org.apache.cxf.binding.corba.types.CorbaFixedHandler;
import org.apache.cxf.binding.corba.types.CorbaObjectHandler;
import org.apache.cxf.binding.corba.types.CorbaObjectReferenceHandler;
import org.apache.cxf.binding.corba.types.CorbaOctetSequenceHandler;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.types.CorbaSequenceHandler;
import org.apache.cxf.binding.corba.types.CorbaStructHandler;
import org.apache.cxf.binding.corba.types.CorbaUnionHandler;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Exception;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.omg.CORBA.Any;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.portable.OutputStream;

public class CorbaObjectWriter {

    private OutputStream stream;

    public CorbaObjectWriter(OutputStream outStream) {
        stream = outStream;
    }

    public void write(CorbaObjectHandler obj) {
        assert obj != null;
        switch (obj.getTypeCode().kind().value()) {
        case TCKind._tk_boolean:
            this.writeBoolean((Boolean)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_char:
            this.writeChar((Character)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_wchar:
            this.writeWChar((Character)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_octet:
            this.writeOctet((Byte)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_short:
            this.writeShort((Short)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_ushort:
            this.writeUShort((Integer)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_long:
            this.writeLong((Integer)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_ulong:
            this.writeULong((Long)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_longlong:
            this.writeLongLong((Long)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_ulonglong:
            this.writeULongLong((BigInteger)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_float:
            this.writeFloat((Float)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_double:
            this.writeDouble((Double)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_string:
            this.writeString((String)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_wstring:
            this.writeWString((String)((CorbaPrimitiveHandler)obj).getValue());
            break;
        case TCKind._tk_any:
            this.writeAny(obj);
            break;

        // Now for the complex types
        case TCKind._tk_array:
            this.writeArray(obj);
            break;
        case TCKind._tk_sequence:
            this.writeSequence(obj);
            break;
        case TCKind._tk_struct:
            this.writeStruct(obj);
            break;
        case TCKind._tk_enum:
            this.writeEnum(obj);
            break;
        case TCKind._tk_except:
            this.writeException(obj);
            break;
        case TCKind._tk_fixed:
            this.writeFixed(obj);
            break;
        case TCKind._tk_union:
            this.writeUnion(obj);
            break;
        case TCKind._tk_objref:
            this.writeObjectReference(obj);
            break;            
        default:
        // TODO: Provide Implementation. Do we throw an exception.
        }
    }

    // -- primitive types --
    public void writeBoolean(Boolean b) throws CorbaBindingException {
        if (b == null) {
            stream.write_boolean(false);
        } else {
            stream.write_boolean(b.booleanValue());
        }
    }

    public void writeChar(Character c) throws CorbaBindingException {
        if (c == null) {
            stream.write_char((char)0);
        } else {
            stream.write_char(c.charValue());
        }
    }

    public void writeWChar(Character c) throws CorbaBindingException {
        if (c == null) {
            stream.write_wchar((char)0);
        } else {
            stream.write_wchar(c.charValue());
        }
    }

    public void writeOctet(Byte b) throws CorbaBindingException {
        if (b == null) {
            stream.write_octet((byte)0);
        } else {
            stream.write_octet(b.byteValue());
        }
    }

    public void writeShort(Short s) throws CorbaBindingException {
        if (s == null) {
            stream.write_short((short)0);
        } else {
            stream.write_short(s.shortValue());
        }
    }

    public void writeUShort(Integer s) throws CorbaBindingException {
        if (s == null) {
            stream.write_ushort((short)0);
        } else {
            stream.write_ushort(s.shortValue());
        }
    }

    public void writeLong(Integer l) throws CorbaBindingException {
        if (l == null) {
            stream.write_long((int)0);
        } else {
            stream.write_long(l.intValue());
        }
    }

    public void writeULong(Long l) throws CorbaBindingException {
        if (l == null) {
            stream.write_ulong((int)0);
        } else {
            stream.write_ulong(l.intValue());
        }
    }

    public void writeLongLong(Long l) throws CorbaBindingException {
        if (l == null) {
            stream.write_longlong((long)0);
        } else {
            stream.write_longlong(l.longValue());
        }
    }

    public void writeULongLong(java.math.BigInteger l) throws CorbaBindingException {
        if (l == null) {
            stream.write_ulonglong((long)0);
        } else {
            stream.write_ulonglong(l.longValue());
        }
    }

    public void writeFloat(Float f) throws CorbaBindingException {
        if (f == null) {
            stream.write_float((float)0.0);
        } else {
            stream.write_float(f.floatValue());
        }
    }

    public void writeDouble(Double d) throws CorbaBindingException {
        if (d == null) {
            stream.write_double((double)0.0);
        } else {
            stream.write_double(d.doubleValue());
        }
    }

    public void writeString(String s) throws CorbaBindingException {
        if (s == null) {
            stream.write_string("");
        } else {
            stream.write_string(s);
        }
    }

    public void writeWString(String s) throws CorbaBindingException {
        if (s == null) {
            stream.write_wstring("");
        } else {
            stream.write_wstring(s);
        }
    }

    public void writeAny(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaAnyHandler anyHandler = (CorbaAnyHandler)obj;
        CorbaObjectHandler containedType = anyHandler.getAnyContainedType();
        Any a = anyHandler.getValue();

        // This is true if we have an empty any
        if (containedType != null) {
            a.type(containedType.getTypeCode());
            OutputStream os = a.create_output_stream();
            CorbaObjectWriter writer = new CorbaObjectWriter(os);
            writer.write(containedType);
            a.read_value(os.create_input_stream(), containedType.getTypeCode());
        }
        stream.write_any(a);
    }

    // -- complex types --
    public void writeEnum(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaEnumHandler enumHandler = (CorbaEnumHandler)obj;
        Enum enumType = (Enum)enumHandler.getType();
        String enumLabel = enumHandler.getValue();
        List<Enumerator> enumerators = enumType.getEnumerator();
        
        for (int i = 0; i < enumerators.size(); ++i) {
            if (enumerators.get(i).getValue().equals(enumLabel)) {
                stream.write_long(i);
                return;
            }
        }
        
        throw new CorbaBindingException("CorbaObjectWriter: unable to find enumeration label");
    }

    public void writeStruct(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaStructHandler structHandler = (CorbaStructHandler)obj;
        List<CorbaObjectHandler> structElements = structHandler.getMembers();

        for (int i = 0; i < structElements.size(); ++i) {
            this.write(structElements.get(i));
        }
    }

    public void writeException(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaExceptionHandler exHandler = (CorbaExceptionHandler)obj;
        Exception exType = (Exception)exHandler.getType();
        List<CorbaObjectHandler> exMembers = exHandler.getMembers();
        stream.write_string(exType.getRepositoryID());
        for (int i = 0; i < exMembers.size(); ++i) {
            this.write(exMembers.get(i));
        }
    }

    public void writeFixed(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaFixedHandler fixedHandler = (CorbaFixedHandler)obj;
        short scale = (short)fixedHandler.getScale();
        short fixed = (short)fixedHandler.getDigits();
        //the write_fixed method is a "late addition" and not all orbs implement it.
        //Some of them have a "write_fixed(BigDecimal, short, short)" method, we'll try that
        try {
            Method m = stream.getClass().getMethod("write_fixed", new Class[] {BigDecimal.class,
                                                                               Short.TYPE,
                                                                               Short.TYPE});
            m.invoke(stream, fixedHandler.getValue(), fixed, scale);
        } catch (Throwable e1) {
            stream.write_fixed(fixedHandler.getValue().movePointRight(scale));
        }
    }

    public void writeUnion(CorbaObjectHandler obj) throws CorbaBindingException {
        Union unionType = (Union) obj.getType();        
        List<Unionbranch> branches = unionType.getUnionbranch();
        if (branches.size() > 0) {             
            CorbaObjectHandler discriminator = ((CorbaUnionHandler)obj).getDiscriminator();
            this.write(discriminator);
            CorbaObjectHandler unionValue = ((CorbaUnionHandler)obj).getValue();
            if (unionValue != null) {
                this.write(unionValue);
            }
        }
    }

    public void writeArray(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaArrayHandler arrayHandler = (CorbaArrayHandler)obj;
        List<CorbaObjectHandler> arrayElements = arrayHandler.getElements();

        for (int i = 0; i < arrayElements.size(); ++i) {
            this.write(arrayElements.get(i));
        }
    }

    public void writeSequence(CorbaObjectHandler obj) throws CorbaBindingException {
        if (obj instanceof CorbaOctetSequenceHandler) {
            byte[] value = ((CorbaOctetSequenceHandler) obj).getValue();
            stream.write_ulong(value.length);
            stream.write_octet_array(value, 0, value.length);
        } else {
            CorbaSequenceHandler seqHandler = (CorbaSequenceHandler)obj;
            List<CorbaObjectHandler> seqElements = seqHandler.getElements();
            int length = seqElements.size();
            stream.write_ulong(length);
            for (int i = 0; i < length; ++i) {
                this.write(seqElements.get(i));
            }
        }
    }
    
    public void writeObjectReference(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaObjectReferenceHandler objHandler = (CorbaObjectReferenceHandler)obj;
        stream.write_Object(objHandler.getReference());
    }   
}
