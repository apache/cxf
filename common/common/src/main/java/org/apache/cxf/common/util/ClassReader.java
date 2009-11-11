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

package org.apache.cxf.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the class file reader for obtaining the parameter names for declared
 * methods in a class. The class must have debugging attributes for us to obtain
 * this information.
 * <p>
 * This does not work for inherited methods. To obtain parameter names for
 * inherited methods, you must use a paramReader for the class that originally
 * declared the method.
 * <p>
 * don't get tricky, it's the bare minimum. Instances of this class are not
 * threadsafe -- don't share them.
 * <p>
 * 
 * @author Edwin Smith, Macromedia
 */
public class ClassReader extends ByteArrayInputStream {
    // constants values that appear in java class files,
    // from jvm spec 2nd ed, section 4.4, pp 103
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHOD_REF = 11;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_UTF_8 = 1;
    /**
     * the constant pool. constant pool indices in the class file directly index
     * into this array. The value stored in this array is the position in the
     * class file where that constant begins.
     */
    private int[] cpoolIndex;
    private Object[] cpool;

    private Map<String, Method> attrMethods;

    protected ClassReader(byte buf[], Map<String, Method> attrMethods) {
        super(buf);

        this.attrMethods = attrMethods;
    }
    
    /**
     * load the bytecode for a given class, by using the class's defining
     * classloader and assuming that for a class named P.C, the bytecodes are in
     * a resource named /P/C.class.
     * 
     * @param c the class of interest
     * @return a byte array containing the bytecode
     * @throws IOException
     */
    protected static byte[] getBytes(Class c) throws IOException {
        InputStream fin = c.getResourceAsStream('/' + c.getName().replace('.', '/') + ".class");
        if (fin == null) {
            throw new IOException();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int actual;
            do {
                actual = fin.read(buf);
                if (actual > 0) {
                    out.write(buf, 0, actual);
                }
            } while (actual > 0);
            return out.toByteArray();
        } finally {
            fin.close();
        }
    }

    static String classDescriptorToName(String desc) {
        return desc.replace('/', '.');
    }

    protected static Map<String, Method> findAttributeReaders(Class c) {
        Map<String, Method> map = new HashMap<String, Method>();
        Method[] methods = c.getMethods();

        for (int i = 0; i < methods.length; i++) {
            String name = methods[i].getName();
            if (name.startsWith("read") && methods[i].getReturnType() == void.class) {
                map.put(name.substring(4), methods[i]);
            }
        }

        return map;
    }

    protected static String getSignature(Member method, Class[] paramTypes) {
        // compute the method descriptor

        StringBuilder b = new StringBuilder((method instanceof Method) ? method.getName() : "<init>");
        b.append('(');

        for (int i = 0; i < paramTypes.length; i++) {
            addDescriptor(b, paramTypes[i]);
        }

        b.append(')');
        if (method instanceof Method) {
            addDescriptor(b, ((Method)method).getReturnType());
        } else if (method instanceof Constructor) {
            addDescriptor(b, void.class);
        }

        return b.toString();
    }

    private static void addDescriptor(StringBuilder b, Class c) {
        if (c.isPrimitive()) {
            if (c == void.class) {
                b.append('V');
            } else if (c == int.class) {
                b.append('I');
            } else if (c == boolean.class) {
                b.append('Z');
            } else if (c == byte.class) {
                b.append('B');
            } else if (c == short.class) {
                b.append('S');
            } else if (c == long.class) {
                b.append('J');
            } else if (c == char.class) {
                b.append('C');
            } else if (c == float.class) {
                b.append('F');
            } else if (c == double.class) {
                b.append('D');
            }
        } else if (c.isArray()) {
            b.append('[');
            addDescriptor(b, c.getComponentType());
        } else {
            b.append('L').append(c.getName().replace('.', '/')).append(';');
        }
    }

    /**
     * @return the next unsigned 16 bit value
     */
    protected final int readShort() {
        return (read() << 8) | read();
    }

    /**
     * @return the next signed 32 bit value
     */
    protected final int readInt() {
        return (read() << 24) | (read() << 16) | (read() << 8) | read();
    }

    /**
     * skip n bytes in the input stream.
     */
    protected void skipFully(int n) throws IOException {
        while (n > 0) {
            int c = (int)skip(n);
            if (c <= 0) {
                throw new EOFException();
            }

            n -= c;
        }
    }

    protected final Member resolveMethod(int index) throws IOException, ClassNotFoundException,
        NoSuchMethodException {
        int oldPos = pos;
        try {
            Member m = (Member)cpool[index];
            if (m == null) {
                pos = cpoolIndex[index];
                Class owner = resolveClass(readShort());
                NameAndType nt = resolveNameAndType(readShort());
                String signature = nt.name + nt.type;
                if ("<init>".equals(nt.name)) {
                    Constructor[] ctors = owner.getConstructors();
                    for (int i = 0; i < ctors.length; i++) {
                        String sig = getSignature(ctors[i], ctors[i].getParameterTypes());
                        if (sig.equals(signature)) {
                            cpool[index] = ctors[i];
                            m = ctors[i];
                            return m;
                        }
                    }
                } else {
                    Method[] methods = owner.getDeclaredMethods();
                    for (int i = 0; i < methods.length; i++) {
                        String sig = getSignature(methods[i], methods[i].getParameterTypes());
                        if (sig.equals(signature)) {
                            cpool[index] = methods[i];
                            m = methods[i];
                            return m;
                        }
                    }
                }
                throw new NoSuchMethodException(signature);
            }
            return m;
        } finally {
            pos = oldPos;
        }

    }

    protected final Field resolveField(int i) throws IOException, ClassNotFoundException,
        NoSuchFieldException {
        int oldPos = pos;
        try {
            Field f = (Field)cpool[i];
            if (f == null) {
                pos = cpoolIndex[i];
                Class owner = resolveClass(readShort());
                NameAndType nt = resolveNameAndType(readShort());
                cpool[i] = owner.getDeclaredField(nt.name);
                f = owner.getDeclaredField(nt.name);
            }
            return f;
        } finally {
            pos = oldPos;
        }
    }

    protected final NameAndType resolveNameAndType(int i) throws IOException {
        int oldPos = pos;
        try {
            NameAndType nt = (NameAndType)cpool[i];
            if (nt == null) {
                pos = cpoolIndex[i];
                String name = resolveUtf8(readShort());
                String type = resolveUtf8(readShort());
                cpool[i] = new NameAndType(name, type);
                nt = new NameAndType(name, type);
            }
            return nt;
        } finally {
            pos = oldPos;
        }
    }

    protected final Class resolveClass(int i) throws IOException, ClassNotFoundException {
        int oldPos = pos;
        try {
            Class c = (Class)cpool[i];
            if (c == null) {
                pos = cpoolIndex[i];
                String name = resolveUtf8(readShort());
                cpool[i] = Class.forName(classDescriptorToName(name));
                c = Class.forName(classDescriptorToName(name));
            }
            return c;
        } finally {
            pos = oldPos;
        }
    }

    protected final String resolveUtf8(int i) throws IOException {
        int oldPos = pos;
        try {
            String s = (String)cpool[i];
            if (s == null) {
                pos = cpoolIndex[i];
                int len = readShort();
                skipFully(len);
                cpool[i] = new String(buf, pos - len, len, "utf-8");
                s = new String(buf, pos - len, len, "utf-8");
            }
            return s;
        } finally {
            pos = oldPos;
        }
    }

    @SuppressWarnings("fallthrough")
    protected final void readCpool() throws IOException {
        int count = readShort(); // cpool count
        cpoolIndex = new int[count];
        cpool = new Object[count];
        for (int i = 1; i < count; i++) {
            int c = read();
            cpoolIndex[i] = super.pos;
            // constant pool tag
            switch (c) {
            case CONSTANT_FIELDREF:
            case CONSTANT_METHODREF:
            case CONSTANT_INTERFACE_METHOD_REF:
            case CONSTANT_NAME_AND_TYPE:

                readShort(); // class index or (12) name index
                // fall through

            case CONSTANT_CLASS:
            case CONSTANT_STRING:

                readShort(); // string index or class index
                break;

            case CONSTANT_LONG:
            case CONSTANT_DOUBLE:

                readInt(); // hi-value

                // see jvm spec section 4.4.5 - double and long cpool
                // entries occupy two "slots" in the cpool table.
                i++;
                // fall through

            case CONSTANT_INTEGER:
            case CONSTANT_FLOAT:

                readInt(); // value
                break;

            case CONSTANT_UTF_8:

                int len = readShort();
                skipFully(len);
                break;

            default:
                // corrupt class file
                throw new IllegalStateException();
            }
        }
    }

    protected final void skipAttributes() throws IOException {
        int count = readShort();
        for (int i = 0; i < count; i++) {
            readShort(); // name index
            skipFully(readInt());
        }
    }

    /**
     * read an attributes array. the elements of a class file that can contain
     * attributes are: fields, methods, the class itself, and some other types
     * of attributes.
     */
    protected final void readAttributes() throws IOException {
        int count = readShort();
        for (int i = 0; i < count; i++) {
            int nameIndex = readShort(); // name index
            int attrLen = readInt();
            int curPos = pos;

            String attrName = resolveUtf8(nameIndex);

            Method m = attrMethods.get(attrName);

            if (m != null) {
                try {
                    m.invoke(this, new Object[] {});
                } catch (IllegalAccessException e) {
                    pos = curPos;
                    skipFully(attrLen);
                } catch (InvocationTargetException e) {
                    try {
                        throw e.getTargetException();
                    } catch (Error ex) {
                        throw ex;
                    } catch (RuntimeException ex) {
                        throw ex;
                    } catch (IOException ex) {
                        throw ex;
                    } catch (Throwable ex) {
                        pos = curPos;
                        skipFully(attrLen);
                    }
                }
            } else {
                // don't care what attribute this is
                skipFully(attrLen);
            }
        }
    }

    /**
     * read a code attribute
     * 
     * @throws IOException
     */
    public void readCode() throws IOException {
        readShort(); // max stack
        readShort(); // max locals
        skipFully(readInt()); // code
        skipFully(8 * readShort()); // exception table

        // read the code attributes (recursive). This is where
        // we will find the LocalVariableTable attribute.
        readAttributes();
    }

    private static class NameAndType {
        String name;
        String type;

        public NameAndType(String name, String type) {
            this.name = name;
            this.type = type;
        }
    }
}
