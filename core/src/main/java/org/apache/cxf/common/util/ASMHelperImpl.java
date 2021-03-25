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

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.common.classloader.ClassLoaderUtils;


public class ASMHelperImpl implements ASMHelper {
    protected static final Map<Class<?>, String> PRIMITIVE_MAP = new HashMap<>();
    protected static final Map<Class<?>, String> NONPRIMITIVE_MAP = new HashMap<>();
    protected static final Map<Class<?>, Integer> PRIMITIVE_ZERO_MAP = new HashMap<>();

    protected boolean badASM;
    private Class<?> cwClass;

    public ASMHelperImpl() {

    }

    static {
        PRIMITIVE_MAP.put(Byte.TYPE, "B");
        PRIMITIVE_MAP.put(Boolean.TYPE, "Z");
        PRIMITIVE_MAP.put(Long.TYPE, "J");
        PRIMITIVE_MAP.put(Integer.TYPE, "I");
        PRIMITIVE_MAP.put(Short.TYPE, "S");
        PRIMITIVE_MAP.put(Character.TYPE, "C");
        PRIMITIVE_MAP.put(Float.TYPE, "F");
        PRIMITIVE_MAP.put(Double.TYPE, "D");

        NONPRIMITIVE_MAP.put(Byte.TYPE, Byte.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Boolean.TYPE, Boolean.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Long.TYPE, Long.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Integer.TYPE, Integer.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Short.TYPE, Short.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Character.TYPE, Character.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Float.TYPE, Float.class.getName().replaceAll("\\.", "/"));
        NONPRIMITIVE_MAP.put(Double.TYPE, Double.class.getName().replaceAll("\\.", "/"));
    }

    private void tryClass(String s) {
        if (cwClass == null) {
            try {
                Class<?> c2 = ClassLoaderUtils.loadClass(s, ASMHelperImpl.class);

                //old versions don't have this, but we need it
                Class<?> cls = ClassLoaderUtils.loadClass(c2.getPackage().getName() + ".MethodVisitor", c2);
                cls.getMethod("visitFrame", Integer.TYPE, Integer.TYPE,
                        Object[].class,  Integer.TYPE, Object[].class);
                cwClass = c2;
            } catch (Throwable t) {
                //ignore
            }
        }
    }
    private Class<?> getASMClassWriterClass() {
        //force this to make sure the proper OSGi import is generated
        return org.objectweb.asm.ClassWriter.class;
    }

    public synchronized Class<?> getASMClass() throws ClassNotFoundException {
        if (cwClass == null) {
            //try the "real" asm first, then the others
            tryClass("org.objectweb.asm.ClassWriter");
            tryClass("org.apache.xbean.asm9.ClassWriter");
            tryClass("org.apache.xbean.asm8.ClassWriter");
            tryClass("org.apache.xbean.asm7.ClassWriter");
            tryClass("org.apache.xbean.asm5.ClassWriter");
            tryClass("org.apache.xbean.asm6.ClassWriter");
            tryClass("org.apache.xbean.asm4.ClassWriter");
            tryClass("org.apache.xbean.asm.ClassWriter");
            tryClass("org.springframework.asm.ClassWriter");
            if (cwClass == null) {
                cwClass = getASMClassWriterClass();
            }
        }
        return cwClass;
    }
    public OpcodesProxy getOpCodes() {
        OpcodesProxy ops = new OpcodesProxy(this);
        PRIMITIVE_ZERO_MAP.put(Byte.TYPE, ops.ICONST_0);
        PRIMITIVE_ZERO_MAP.put(Boolean.TYPE, ops.ICONST_0);
        PRIMITIVE_ZERO_MAP.put(Long.TYPE, ops.LCONST_0);
        PRIMITIVE_ZERO_MAP.put(Integer.TYPE, ops.ICONST_0);
        PRIMITIVE_ZERO_MAP.put(Short.TYPE, ops.ICONST_0);
        PRIMITIVE_ZERO_MAP.put(Character.TYPE, ops.ICONST_0);
        PRIMITIVE_ZERO_MAP.put(Float.TYPE, ops.FCONST_0);
        PRIMITIVE_ZERO_MAP.put(Double.TYPE, ops.DCONST_0);
        return ops;
    }
    public void setBadASM(boolean b) {
        badASM = b;
    }

    public String getMethodSignature(Method m) {
        StringBuilder buf = new StringBuilder("(");
        for (Class<?> cl : m.getParameterTypes()) {
            buf.append(getClassCode(cl));
        }
        buf.append(')');
        buf.append(getClassCode(m.getReturnType()));

        return buf.toString();
    }

    @Override
    public String getNonPrimitive(Class<?> tp) {
        return NONPRIMITIVE_MAP.get(tp);
    }
    @Override
    public String getPrimitive(Class<?> tp) {
        return PRIMITIVE_MAP.get(tp);
    }




    public String getClassCode(Class<?> cl) {
        if (cl == Void.TYPE) {
            return "V";
        }
        if (cl.isPrimitive()) {
            return PRIMITIVE_MAP.get(cl);
        }
        if (cl.isArray()) {
            return "[" + getClassCode(cl.getComponentType());
        }
        return "L" + StringUtils.periodToSlashes(cl.getName()) + ";";
    }
    public String getClassCode(java.lang.reflect.Type type) {
        if (type instanceof Class) {
            return getClassCode((Class<?>)type);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType at = (GenericArrayType)type;
            return "[" + getClassCode(at.getGenericComponentType());
        } else if (type instanceof TypeVariable) {
            TypeVariable<?> tv = (TypeVariable<?>)type;
            java.lang.reflect.Type[] bounds = tv.getBounds();
            if (bounds != null && bounds.length == 1) {
                return getClassCode(bounds[0]);
            }
            throw new IllegalArgumentException("Unable to determine type for: " + tv);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            StringBuilder a = new StringBuilder(getClassCode(pt.getRawType()));
            if (!pt.getRawType().equals(Enum.class)) {
                a.setLength(a.length() - 1);
                a.append('<');

                for (java.lang.reflect.Type t : pt.getActualTypeArguments()) {
                    a.append(getClassCode(t));
                }
                a.append(">;");
            }
            return a.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType)type;
            StringBuilder a = new StringBuilder();
            java.lang.reflect.Type[] lowBounds = wt.getLowerBounds();
            java.lang.reflect.Type[] upBounds = wt.getUpperBounds();
            for (java.lang.reflect.Type t : upBounds) {
                a.append('+');
                a.append(getClassCode(t));
            }
            for (java.lang.reflect.Type t : lowBounds) {
                a.append('-');
                a.append(getClassCode(t));
            }
            return a.toString();
        }
        return null;
    }

    public ClassWriter createClassWriter() {
        Object newCw = null;
        if (!badASM) {
            if (cwClass == null) {
                try {
                    cwClass = getASMClass();
                } catch (Throwable error) {
                    badASM = true;
                    throw new RuntimeException("No ASM ClassWriterFound", error);
                }
            }
            // ASM >= 3.x (since cxf is java 8 min we don't care of asm 1/2)
            try {
                Constructor<?> cons = cwClass.getConstructor(Integer.TYPE);
                int i = cwClass.getField("COMPUTE_MAXS").getInt(null);
                i |= cwClass.getField("COMPUTE_FRAMES").getInt(null);
                newCw = cons.newInstance(Integer.valueOf(i));
            } catch (Throwable e1) {
                // ignore
            }
        }
        if (newCw != null) {
            return ReflectionInvokationHandler.createProxyWrapper(newCw, ClassWriter.class);
        }
        return null;
    }


    public ASMType getType(final String type) {
        try {
            final Class<?> cls = ClassLoaderUtils.loadClass(cwClass.getPackage().getName() + ".Type", cwClass);
            final Method m = cls.getMethod("getType", String.class);
            final Method m2 = cls.getMethod("getOpcode", Integer.TYPE);
            @SuppressWarnings("unused")
            ASMType t = new ASMType() {
                Object tp = ReflectionUtil.setAccessible(m).invoke(null, type);
                public Object getValue() {
                    return tp;
                }
                public Class<?> realType() {
                    return cls;
                }
                public int getOpcode(int ireturn) {
                    try {
                        return (Integer)ReflectionUtil.setAccessible(m2).invoke(tp, ireturn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };
            return t;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public Label createLabel() {
        try {
            final Class<?> cls = ClassLoaderUtils.loadClass(cwClass.getPackage().getName() + ".Label",
                    cwClass);
            @SuppressWarnings("unused")
            Label l = new Label() {
                Object l = cls.newInstance();
                public Object getValue() {
                    return l;
                }
                public Class<?> realType() {
                    return cls;
                }
            };
            return l;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
