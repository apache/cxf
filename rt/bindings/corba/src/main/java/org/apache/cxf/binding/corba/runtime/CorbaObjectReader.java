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


import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

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
import org.apache.cxf.binding.corba.wsdl.CaseType;
import org.apache.cxf.binding.corba.wsdl.Enum;
import org.apache.cxf.binding.corba.wsdl.Enumerator;
import org.apache.cxf.binding.corba.wsdl.Union;
import org.apache.cxf.binding.corba.wsdl.Unionbranch;
import org.apache.cxf.common.logging.LogUtils;
import org.omg.CORBA.Any;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.CORBA.TCKind;
import org.omg.CORBA.TypeCode;
import org.omg.CORBA.portable.InputStream;

public class CorbaObjectReader {

    private static final Logger LOG = LogUtils.getL7dLogger(CorbaObjectReader.class);

    private InputStream stream;

    public CorbaObjectReader(InputStream inStream) {
        stream = inStream;
    }

    public void read(CorbaObjectHandler obj) throws CorbaBindingException {
        switch (obj.getTypeCode().kind().value()) {
        case TCKind._tk_boolean:
            ((CorbaPrimitiveHandler)obj).setValue(this.readBoolean());
            break;
        case TCKind._tk_char:
            Character charValue = this.readChar();
            ((CorbaPrimitiveHandler)obj).setValue(charValue);
            break;
        case TCKind._tk_wchar:
            ((CorbaPrimitiveHandler)obj).setValue(this.readWChar());
            break;
        case TCKind._tk_octet:
            Byte octetValue = this.readOctet();
            ((CorbaPrimitiveHandler)obj).setValue(octetValue);
            break;
        case TCKind._tk_short:
            ((CorbaPrimitiveHandler)obj).setValue(this.readShort());
            break;
        case TCKind._tk_ushort:
            ((CorbaPrimitiveHandler)obj).setValue(this.readUShort());
            break;
        case TCKind._tk_long:
            ((CorbaPrimitiveHandler)obj).setValue(this.readLong());
            break;
        case TCKind._tk_ulong:
            ((CorbaPrimitiveHandler)obj).setValue(this.readULong());
            break;
        case TCKind._tk_longlong:
            ((CorbaPrimitiveHandler)obj).setValue(this.readLongLong());
            break;
        case TCKind._tk_ulonglong:
            ((CorbaPrimitiveHandler)obj).setValue(this.readULongLong());
            break;
        case TCKind._tk_float:
            ((CorbaPrimitiveHandler)obj).setValue(this.readFloat());
            break;
        case TCKind._tk_double:
            ((CorbaPrimitiveHandler)obj).setValue(this.readDouble());
            break;
        case TCKind._tk_string:
            ((CorbaPrimitiveHandler)obj).setValue(this.readString());
            break;
        case TCKind._tk_wstring:
            ((CorbaPrimitiveHandler)obj).setValue(this.readWString());
            break;
        case TCKind._tk_any:
            ((CorbaAnyHandler)obj).setValue(this.readAny());
            break;
        
        // Now for the complex types
        case TCKind._tk_array:
            this.readArray(obj);
            break;
        case TCKind._tk_sequence:
            this.readSequence(obj);
            break;
        case TCKind._tk_struct:
            this.readStruct(obj);
            break;
        case TCKind._tk_enum:
            this.readEnum(obj);
            break;
        case TCKind._tk_except:
            this.readException(obj);
            break;
        case TCKind._tk_fixed:
            this.readFixed(obj);
            break;
        case TCKind._tk_union:
            this.readUnion(obj);
            break;
        case TCKind._tk_objref:
            this.readObjectReference(obj);
            break;            
        default:
        // TODO: Provide Implementation. Do we throw an exception.
        }
    }

    // -- primitive types --
    public Boolean readBoolean() throws CorbaBindingException {
        try {
            return stream.read_boolean();
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read boolean");
            throw new CorbaBindingException("CorbaObjectReader: readBoolean MARSHAL exception", ex);
        }
    }

    public Character readChar() throws CorbaBindingException {
        try {
            return new Character(stream.read_char());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read character");
            throw new CorbaBindingException("CorbaObjectReader: readChar MARSHAL exception", ex);
        }
    }

    public Character readWChar() throws CorbaBindingException {
        try {
            return new Character(stream.read_wchar());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read wide character");
            throw new CorbaBindingException("CorbaObjectReader: readWChar MARSHAL exception", ex);
        }
    }

    public Byte readOctet() throws CorbaBindingException {
        try {
            return new Byte(stream.read_octet());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read octet");
            throw new CorbaBindingException("CorbaObjectReader: readOctet MARSHAL exception", ex);
        }
    }

    public Short readShort() throws CorbaBindingException {
        try {
            return new Short(stream.read_short());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read short");
            throw new CorbaBindingException("CorbaObjectReader: readShort MARSHAL exception", ex);
        }
    }

    public Integer readUShort() throws CorbaBindingException {
        try {
            int result = stream.read_ushort();
            if (result < 0) {
                result = (result - Short.MIN_VALUE) - Short.MIN_VALUE;
            }
            return result;
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read unsigned short");
            throw new CorbaBindingException("CorbaObjectReader: readUShort MARSHAL exception", ex);
        }
    }

    public Integer readLong() throws CorbaBindingException {
        Integer result = new Integer(stream.read_long());
        try {
            return result;
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read long");
            throw new CorbaBindingException("CorbaObjectReader: readLong MARSHAL exception", ex);
        }
    }

    public long readULong() throws CorbaBindingException {
        try {
            long l = stream.read_ulong();
            l &= 0xffffffffL;
            return l;
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read unsigned long");
            throw new CorbaBindingException("CorbaObjectReader: readULong MARSHAL exception", ex);
        }
    }

    public Long readLongLong() throws CorbaBindingException {
        try {
            return new Long(stream.read_longlong());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read long long");
            throw new CorbaBindingException("CorbaObjectReader: readLongLong MARSHAL exception", ex);
        }
    }

    private BigInteger convertLongToULong(long l) {
        if (l < 0) {
            long l2 = l & 0x7FFFFFFFFFFFFFL;
            BigInteger i = BigInteger.valueOf(l2);
            BigInteger i2 = BigInteger.valueOf(0);
            i2.setBit(63);
            i = i.or(i2);
            return i;
        }
        return BigInteger.valueOf(l);
    }
    public BigInteger readULongLong() throws CorbaBindingException {
        try {
            return convertLongToULong(stream.read_ulonglong());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read unsigned long long");
            throw new CorbaBindingException("CorbaObjectReader: readULongLong MARSHAL exception", ex);
        }
    }

    public Float readFloat() throws CorbaBindingException {
        try {
            return new Float(stream.read_float());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read float");
            throw new CorbaBindingException("CorbaObjectReader: readFloat MARSHAL exception", ex);
        }
    }

    public Double readDouble() throws CorbaBindingException {
        try {
            return new Double(stream.read_double());
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read double");
            throw new CorbaBindingException("CorbaObjectReader: readDouble MARSHAL exception", ex);
        }
    }

    public String readString() throws CorbaBindingException {
        try {
            return stream.read_string();
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read string");
            throw new CorbaBindingException("CorbaObjectReader: readString MARSHAL exception", ex);
        }
    }

    public String readWString() throws CorbaBindingException {
        try {
            return stream.read_wstring();
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read wide string");
            throw new CorbaBindingException("CorbaObjectReader: readWString MARSHAL exception", ex);
        }
    }

    public Any readAny() throws CorbaBindingException {
        try {
            return stream.read_any();
        } catch (org.omg.CORBA.MARSHAL ex) {
            LOG.log(Level.SEVERE, "CorbaObjectReader: could not read any");
            throw new CorbaBindingException("CorbaObjectReader: readAny MARSHAL exception", ex);
        }
    }

    // -- complex types --
    public void readEnum(CorbaObjectHandler obj) throws CorbaBindingException {
        int enumIndex = stream.read_long();
        Enum enumType = (Enum) obj.getType();
        List<Enumerator> enumerators = enumType.getEnumerator();

        CorbaEnumHandler enumObj = (CorbaEnumHandler)obj;
        enumObj.setValue(enumerators.get(enumIndex).getValue());
    }

    public void readStruct(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaStructHandler structObj = (CorbaStructHandler)obj;
        List<CorbaObjectHandler> structMembers = structObj.getMembers();
        for (int i = 0; i < structMembers.size(); ++i) {
            this.read(structMembers.get(i));
        }
    }

    public void readException(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaExceptionHandler exceptObj = (CorbaExceptionHandler)obj;
        List<CorbaObjectHandler> exceptElements = exceptObj.getMembers();

        String exceptId = stream.read_string();
        exceptObj.setId(exceptId);

        for (int i = 0; i < exceptElements.size(); ++i) {
            this.read(exceptElements.get(i));
        }
    }

    public void readFixed(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaFixedHandler fixedHandler = (CorbaFixedHandler)obj;
        long scale = fixedHandler.getScale();
        try {
            java.math.BigDecimal fixedValue = stream.read_fixed().movePointLeft((int)scale);
            fixedHandler.setValue(fixedValue);
        } catch (NO_IMPLEMENT ex) {
            //the read_fixed method is a "late addition" and not all orbs implement it.
            //Some of them have a "read_fixed(TypeCode)" method, we'll try that
            Method m = null;
            try {
                m = stream.getClass().getMethod("read_fixed", new Class[] {TypeCode.class});
                BigDecimal fixedValue = (BigDecimal)m.invoke(stream, new Object[] {obj.getTypeCode()});
                fixedHandler.setValue(fixedValue);
            } catch (Throwable e1) {
                throw ex;
            }
        }
    }

    public void readEnumDiscriminator(CorbaUnionHandler unionHandler, CorbaEnumHandler disc)
        throws CorbaBindingException {
        int enumIndex = stream.read_long();
        Enum enumType = (Enum) disc.getType();
        List<Enumerator> enumerators = enumType.getEnumerator();
        if (enumIndex == Integer.MAX_VALUE) {
            enumIndex = unionHandler.getDefaultIndex();
        }
        disc.setValue(enumerators.get(enumIndex).getValue());
    }

    public void readUnion(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaUnionHandler unionHandler = (CorbaUnionHandler)obj;
        Union unionType = (Union)unionHandler.getType();
        List<Unionbranch> branches = unionType.getUnionbranch();
        CorbaObjectHandler discriminator = unionHandler.getDiscriminator();
        if (branches.size() > 0) {
            String discLabel = null;
            if (discriminator.getTypeCodeKind().value() == TCKind._tk_enum) {
                CorbaEnumHandler disc = (CorbaEnumHandler) discriminator;
                readEnumDiscriminator(unionHandler, disc);
                discLabel = disc.getValue();
            } else {
                read(discriminator);
                discLabel = ((CorbaPrimitiveHandler)discriminator).getDataFromValue(); 
            }
            // Now find the label in the union to get the right case
            Unionbranch defaultBranch = null;
            boolean caseFound = false;
            for (Iterator<Unionbranch> branchIter = branches.iterator(); branchIter.hasNext();) {
                Unionbranch branch = branchIter.next();
                if (branch.isSetDefault() && branch.isDefault()) {
                    defaultBranch = branch;
                }
                List<CaseType> cases = branch.getCase();
                for (Iterator<CaseType> caseIter = cases.iterator(); caseIter.hasNext();) {
                    CaseType c = caseIter.next();
                    if (c.getLabel().equalsIgnoreCase(discLabel)) {
                        CorbaObjectHandler branchObj = unionHandler.getBranchByName(branch.getName());
                        this.read(branchObj);
                        unionHandler.setValue(branch.getName(), branchObj);
                        caseFound = true;
                        break;
                    }
                }
                if (caseFound) {
                    break;
                }
            }
            
            // If we never find a case that matches the value of the discriminiator, then we must have
            // found the default case.
            if (!caseFound && defaultBranch != null) {
                CorbaObjectHandler branchObj = unionHandler.getBranchByName(defaultBranch.getName());
                this.read(branchObj);
                unionHandler.setValue(defaultBranch.getName(), branchObj);
            }
        }
    }

    //CHECKSTYLE:OFF  -  processing the typecodes in a switch makes this method fairly long/complex
    public void readArray(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaArrayHandler arrayObj = (CorbaArrayHandler)obj;
        List<CorbaObjectHandler> arrayElements = arrayObj.getElements();

        Object val = null;
        int arraySize = arrayElements.size();
        if (arraySize > 0) {
            switch(arrayElements.get(0).getTypeCodeKind().value()) {
            case TCKind._tk_boolean: {
                boolean[] values = new boolean[arraySize];
                stream.read_boolean_array(values, 0, arraySize); 
                val = values;
                break;
            }
            case TCKind._tk_char: {
                char[] values = new char[arraySize];
                stream.read_char_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_wchar: {
                char[] values = new char[arraySize];
                stream.read_wchar_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_octet: {
                byte[] values = new byte[arraySize];
                stream.read_octet_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_short: {
                short[] values = new short[arraySize];
                stream.read_short_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_ushort: {
                short[] values = new short[arraySize];
                stream.read_ushort_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_long: {
                int[] values = new int[arraySize];
                stream.read_long_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_ulong: {
                int[] values = new int[arraySize];
                stream.read_ulong_array(values, 0, arraySize);
                long[] v2 = new long[arraySize];
                for (int x = 0; x < arraySize; x++) {
                    v2[x] = values[x];
                    v2[x] &= 0xFFFFFFFFL;
                }
                val = v2;
                break;
            }
            case TCKind._tk_longlong: {
                long[] values = new long[arraySize];
                stream.read_longlong_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_ulonglong: {
                long[] values = new long[arraySize];
                stream.read_ulonglong_array(values, 0, arraySize);
                BigInteger[] v2 = new BigInteger[arraySize];
                for (int x = 0; x < arraySize; x++) {
                    v2[x] = convertLongToULong(values[x]);
                }
                val = v2;
                break;
            }
            case TCKind._tk_float: {
                float[] values = new float[arraySize];
                stream.read_float_array(values, 0, arraySize);
                val = values;
                break;
            }
            case TCKind._tk_double: {
                double[] values = new double[arraySize];
                stream.read_double_array(values, 0, arraySize);
                val = values;
                break;
            }
            default:
                for (int i = 0; i < arrayElements.size(); ++i) {
                    this.read(arrayElements.get(i));
                }
            }
            if (val != null) {
                int sz = Array.getLength(val);
                for (int i = 0; i < sz; i++) {
                    ((CorbaPrimitiveHandler)arrayElements.get(i)).setValue(Array.get(val, i));
                }
            }
        }
    }
    //CHECKSTYLE:ON

    public void readSequence(CorbaObjectHandler obj) throws CorbaBindingException {
        if (obj instanceof CorbaOctetSequenceHandler) {
            int length = stream.read_ulong();
            byte[] value = new byte[length];
            stream.read_octet_array(value, 0, length);
            ((CorbaOctetSequenceHandler) obj).setValue(value);
        } else {
            CorbaSequenceHandler sequenceObj = (CorbaSequenceHandler)obj;
            List<CorbaObjectHandler> seqElements = sequenceObj.getElements();
            int length = stream.read_ulong();
            List<CorbaObjectHandler> elements = new ArrayList<CorbaObjectHandler>(length);
            
            // Simply checking the bound won't handle our recursive types.  We need to check for the
            // existance of template, which will be present for all unbounded sequences and for bound
            // sequences with recursive type elements.  Use the template element to construct each
            // object that is in the input stream.
            if (sequenceObj.getTemplateElement() != null) {
                sequenceObj.clear();
                CorbaObjectHandler template = sequenceObj.getTemplateElement();
                for (int i = 0; i < length; ++i) {
                    CorbaObjectHandler seqElement;
                    if (i < seqElements.size()) {
                        seqElement = seqElements.get(i);
                    } else {
                        seqElement = initializeCorbaObjectHandler(template);
                    }
                    read(seqElement);
                    elements.add(seqElement);
                }
                sequenceObj.setElements(elements);
            } else {
                // We have a bounded sequence and the object should already be pre-built
                for (int i = 0; i < length; ++i) {
                    read(seqElements.get(i));
                }
            }
        }
    }

    public void readObjectReference(CorbaObjectHandler obj) throws CorbaBindingException {
        CorbaObjectReferenceHandler objRefObj = (CorbaObjectReferenceHandler)obj;
        org.omg.CORBA.Object objRef = stream.read_Object();
        objRefObj.setReference(objRef);
    }

    private CorbaObjectHandler initializeCorbaObjectHandler(CorbaObjectHandler template) {
        Constructor templateConstructor = template.getClass().getDeclaredConstructors()[0];
        Object[] params = new Object[4];
        
        // Check to see if the template type is a recursive type.  If so, it means that it is part
        // of a sequence and needs to have the name "item" in order
        if (template.isRecursive()) {
            // Revisit: Is this always the case?
            params[0] = new QName("item");
        } else {
            params[0] = template.getName();
        }
        params[1] = template.getIdlType();
        params[2] = template.getTypeCode();
        params[3] = template.getType();
        
        CorbaObjectHandler handler = null;
        try {
            handler = (CorbaObjectHandler) templateConstructor.newInstance(params);
            // To construct an any, we also need to set a typemap.  This should be available through
            // the template object.
            if (template instanceof CorbaAnyHandler) {
                ((CorbaAnyHandler)handler).setTypeMap(((CorbaAnyHandler)template).getTypeMap());
            }
        } catch (java.lang.Exception ex) {
            throw new CorbaBindingException("Unable to instantiate sequence element", ex);
        }


        if (template instanceof CorbaSequenceHandler) {
            CorbaSequenceHandler templateSeq = (CorbaSequenceHandler) template;
            ((CorbaSequenceHandler)handler).
                setTemplateElement(templateSeq.getTemplateElement());
        } else if (template instanceof CorbaStructHandler) {
            CorbaStructHandler templateStruct = (CorbaStructHandler) template;
            CorbaStructHandler struct = (CorbaStructHandler) handler;
            struct.setRecursive(template.isRecursive());
            List<CorbaObjectHandler> members = templateStruct.getMembers();
            for (int i = 0; i < members.size(); i++) {
                CorbaObjectHandler member = initializeCorbaObjectHandler(members.get(i));
                struct.addMember(member);
            }           
        } else if (template instanceof CorbaArrayHandler) {
            CorbaArrayHandler templateArray = (CorbaArrayHandler) template;
            CorbaArrayHandler array = (CorbaArrayHandler) handler;
            List<CorbaObjectHandler> elements = templateArray.getElements();
            for (int i = 0; i < elements.size(); i++) {
                CorbaObjectHandler element = initializeCorbaObjectHandler(elements.get(i));
                array.addElement(element);
            }
        } else if (template instanceof CorbaUnionHandler) {
            CorbaUnionHandler templateUnion = (CorbaUnionHandler) template;
            CorbaUnionHandler union = (CorbaUnionHandler) handler;
            union.setRecursive(template.isRecursive());
            union.setDiscriminator(initializeCorbaObjectHandler(templateUnion.getDiscriminator()));
            List<CorbaObjectHandler> cases = templateUnion.getCases();
            for (int i = 0; i < cases.size(); i++) {
                union.addCase(initializeCorbaObjectHandler(cases.get(i)));
            }
        }

        return handler;

    }

}
