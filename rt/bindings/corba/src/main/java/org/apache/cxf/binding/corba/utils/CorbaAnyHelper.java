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

import org.apache.cxf.binding.corba.CorbaBindingException;
import org.apache.cxf.binding.corba.types.CorbaPrimitiveHandler;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.W3CConstants;
import org.apache.cxf.common.util.ASMHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.omg.CORBA.Any;
import org.omg.CORBA.ORB;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;

public final class CorbaAnyHelper {
   
    private static final Map<QName, QName> SCHEMA_TO_IDL_TYPES = new HashMap<QName, QName>();
    private static final Map<QName, QName> IDL_TO_SCHEMA_TYPES = new HashMap<QName, QName>();
    
    private static Constructor fixedAnyConstructor;
    
    private CorbaAnyHelper() {
        //utility class
    }
    
    public static Any createAny(ORB orb) {
        Any value = orb.create_any();
        if ("com.sun.corba.se.impl.corba.AnyImpl".equals(value.getClass().getName())) {
            value = createFixedAny(orb, value);
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
            primitive.setValue(Integer.valueOf((int)a.extract_ushort()));
            break;
        case TCKind._tk_long:
            primitive.setValue(Integer.valueOf(a.extract_long()));
            break;
        case TCKind._tk_ulong:
            primitive.setValue(BigInteger.valueOf((long)a.extract_ulong()));
            break;
        case TCKind._tk_longlong:
            primitive.setValue(Long.valueOf(a.extract_longlong()));
            break;
        case TCKind._tk_ulonglong:
            primitive.setValue(BigInteger.valueOf((long)a.extract_ulonglong()));
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
    
    private static Any createFixedAny(ORB orb, Any any) {
        createFixedAnyConstructor();
        try {
            return (Any)fixedAnyConstructor.newInstance(orb, any);
        } catch (Exception e) {
            return any;
        }
    }
    private static synchronized void createFixedAnyConstructor() {
        if (fixedAnyConstructor != null) {
            return;
        }
        
        ASMHelper helper = new ASMHelper();
        ClassWriter cw = helper.createClassWriter();
        FieldVisitor fv;

        cw.visit(Opcodes.V1_5, Opcodes.ACC_PUBLIC + Opcodes.ACC_SUPER,
                 "org/apache/cxf/binding/corba/utils/FixedAnyImpl", 
                 null, "com/sun/corba/se/impl/corba/AnyImpl", null);

        cw.visitSource("FixedAnyImpl.java", null);

        fv = cw.visitField(0, "obj", "Lorg/omg/CORBA/portable/Streamable;", null, null);
        fv.visitEnd();
        addFixedAnyConstructor(cw);
        addInsertOverride(cw);
        addExtractOverride(cw);
        addReadOverride(cw);
        addWriteOverride(cw);
        
        cw.visitEnd();

        byte[] b = cw.toByteArray();
        Class<?> c = helper.loadClass("org.apache.cxf.binding.corba.utils.FixedAnyImpl", 
                                      CorbaAnyHelper.class, b);
        try {
            fixedAnyConstructor = c.getConstructor(ORB.class, Any.class);
        } catch (Exception e) {
            //shouldn't happen since we generated that constructor
        }
    }

    private static void addReadOverride(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "read_value", 
                            "(Lorg/omg/CORBA/portable/InputStream;Lorg/omg/CORBA/TypeCode;)V", 
                            null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(54, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(55, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl", 
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/omg/CORBA/portable/Streamable", 
                           "_read", "(Lorg/omg/CORBA/portable/InputStream;)V");
        Label l3 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l3);
        mv.visitLabel(l1);
        mv.visitLineNumber(57, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl", 
                           "read_value", 
                           "(Lorg/omg/CORBA/portable/InputStream;Lorg/omg/CORBA/TypeCode;)V");
        mv.visitLabel(l3);
        mv.visitLineNumber(59, l3);
        mv.visitInsn(Opcodes.RETURN);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                              null, l0, l4, 0);
        mv.visitLocalVariable("is", "Lorg/omg/CORBA/portable/InputStream;", null, l0, l4, 1);
        mv.visitLocalVariable("t", "Lorg/omg/CORBA/TypeCode;", null, l0, l4, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
    }
        
    private static void addWriteOverride(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "write_value", 
                            "(Lorg/omg/CORBA/portable/OutputStream;)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(61, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(62, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "org/omg/CORBA/portable/Streamable",
                           "_write", "(Lorg/omg/CORBA/portable/OutputStream;)V");
        Label l3 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, l3);
        mv.visitLabel(l1);
        mv.visitLineNumber(64, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl",
                           "write_value", "(Lorg/omg/CORBA/portable/OutputStream;)V");
        mv.visitLabel(l3);
        mv.visitLineNumber(66, l3);
        mv.visitInsn(Opcodes.RETURN);
        Label l4 = new Label();
        mv.visitLabel(l4);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                              null, l0, l4, 0);
        mv.visitLocalVariable("os", "Lorg/omg/CORBA/portable/OutputStream;", null, l0, l4, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
        
    }

    private static void addExtractOverride(ClassWriter cw) {
        // TODO Auto-generated method stub
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "extract_Streamable",
                            "()Lorg/omg/CORBA/portable/Streamable;", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(47, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl", 
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        Label l1 = new Label();
        mv.visitJumpInsn(Opcodes.IFNULL, l1);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(48, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitFieldInsn(Opcodes.GETFIELD, "org/apache/cxf/binding/corba/utils/FixedAnyImpl",
                          "obj", "Lorg/omg/CORBA/portable/Streamable;");
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitLabel(l1);
        mv.visitLineNumber(50, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "com/sun/corba/se/impl/corba/AnyImpl",
                           "extract_Streamable", "()Lorg/omg/CORBA/portable/Streamable;");
        mv.visitInsn(Opcodes.ARETURN);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;", null, l0, l3, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        
    }

    private static void addInsertOverride(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                            "insert_Streamable", 
                            "(Lorg/omg/CORBA/portable/Streamable;)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(43, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, 
                           "com/sun/corba/se/impl/corba/AnyImpl", 
                           "insert_Streamable", 
                           "(Lorg/omg/CORBA/portable/Streamable;)V");
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(44, l1);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(Opcodes.PUTFIELD, 
                          "org/apache/cxf/binding/corba/utils/FixedAnyImpl", "obj", 
                          "Lorg/omg/CORBA/portable/Streamable;");
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(45, l2);
        mv.visitInsn(Opcodes.RETURN);
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                              null, l0, l3, 0);
        mv.visitLocalVariable("s", "Lorg/omg/CORBA/portable/Streamable;", null, l0, l3, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();
    }

    private static void addFixedAnyConstructor(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Lorg/omg/CORBA/ORB;)V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(36, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/sun/corba/se/spi/orb/ORB");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           "com/sun/corba/se/impl/corba/AnyImpl",
                           "<init>", "(Lcom/sun/corba/se/spi/orb/ORB;)V");
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(37, l1);
        mv.visitInsn(Opcodes.RETURN);
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this",
                              "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;", 
                              null, l0, l2, 0);
        mv.visitLocalVariable("orb", "Lorg/omg/CORBA/ORB;", null, l0, l2, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        
        mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>",
                            "(Lorg/omg/CORBA/ORB;Lorg/omg/CORBA/Any;)V",
                            null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(39, l0);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, "com/sun/corba/se/spi/orb/ORB");
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           "com/sun/corba/se/impl/corba/AnyImpl",
                           "<init>",
                           "(Lcom/sun/corba/se/spi/orb/ORB;Lorg/omg/CORBA/Any;)V");
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(40, l1);
        mv.visitInsn(Opcodes.RETURN);
        l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLocalVariable("this", "Lorg/apache/cxf/binding/corba/utils/FixedAnyImpl;",
                              null, l0, l2, 0);
        mv.visitLocalVariable("orb", "Lorg/omg/CORBA/ORB;", null, l0, l2, 1);
        mv.visitLocalVariable("any", "Lorg/omg/CORBA/Any;", null, l0, l2, 2);
        mv.visitMaxs(3, 3);
        mv.visitEnd();
        
    }
}
