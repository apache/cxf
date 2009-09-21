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
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.ClassWriter;

public class ASMHelper {
    protected static final Map<Class<?>, String> PRIMITIVE_MAP = new HashMap<Class<?>, String>();
    protected static final Map<Class<?>, String> NONPRIMITIVE_MAP = new HashMap<Class<?>, String>();
    
    protected static final Map<Class<?>, TypeHelperClassLoader> LOADER_MAP 
        = new WeakIdentityHashMap<Class<?>, TypeHelperClassLoader>();
    
    protected static boolean oldASM;
    
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
    
    protected static String getMethodSignature(Method m) {
        StringBuffer buf = new StringBuffer("(");
        for (Class<?> cl : m.getParameterTypes()) {
            buf.append(getClassCode(cl));
        }
        buf.append(")");
        buf.append(getClassCode(m.getReturnType()));
        
        return buf.toString();
    }
    
    protected static String periodToSlashes(String s) {
        char ch[] = s.toCharArray();
        for (int x = 0; x < ch.length; x++) {
            if (ch[x] == '.') {
                ch[x] = '/';
            }
        }
        return new String(ch);
    }
    
    
    public static String getClassCode(Class<?> cl) {
        if (cl == Void.TYPE) {
            return "V";
        }
        if (cl.isPrimitive()) {
            return PRIMITIVE_MAP.get(cl);
        }
        if (cl.isArray()) {
            return "[" + getClassCode(cl.getComponentType());
        }
        return "L" + periodToSlashes(cl.getName()) + ";";
    }
    public static String getClassCode(Type type) {
        if (type instanceof Class) {
            return getClassCode((Class)type);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType at = (GenericArrayType)type;
            return "[" + getClassCode(at.getGenericComponentType());
        } else if (type instanceof TypeVariable) {
            TypeVariable tv = (TypeVariable)type;
            Type[] bounds = tv.getBounds();
            if (bounds != null && bounds.length == 1) {
                return getClassCode(bounds[0]);
            } else {
                throw new IllegalArgumentException("Unable to determine type for: " + tv);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            StringBuilder a = new StringBuilder(getClassCode(pt.getRawType()));
            a.setLength(a.length() - 1);
            a.append('<');
            for (Type t : pt.getActualTypeArguments()) {
                a.append(getClassCode(t));  
            }
            a.append(">;");
            return a.toString();
        }
        return null;
    }
    
    
    public ClassWriter createClassWriter() {
        ClassWriter newCw = null;
        if (!oldASM) {
            Class<ClassWriter> cls;
            try {
                cls = ClassWriter.class;
            } catch (NoClassDefFoundError error) {
                throw new RuntimeException("No ASM ClassWriterFound", error);
            }
            try {
                // ASM 1.5.x/2.x
                Constructor<ClassWriter> cons = cls.getConstructor(new Class<?>[] {Boolean.TYPE});
                
                try {
                    // got constructor, now check if it's 1.x which is very
                    // different from 2.x and 3.x
                    cls.getMethod("newConstInt", new Class<?>[] {Integer.TYPE});               
                    // newConstInt was removed in 2.x, if we get this far, we're
                    // using 1.5.x,
                    // set to null so we don't attempt to use it.
                    newCw = null;    
                    oldASM = true;
                } catch (Throwable t) {
                    newCw = cons.newInstance(new Object[] {Boolean.TRUE});
                }
                
            } catch (Throwable e) {
                // ASM 3.x
                try {
                    Constructor<ClassWriter> cons = cls.getConstructor(new Class<?>[] {Integer.TYPE});
                    int i = cls.getField("COMPUTE_MAXS").getInt(null);
                    i |= cls.getField("COMPUTE_FRAMES").getInt(null);
                    newCw = cons.newInstance(new Object[] {Integer.valueOf(i)});
                } catch (Throwable e1) {
                    // ignore
                }
                
            }
        }
        return newCw;
    }
    
    
    public Class<?> loadClass(String className, Class clz , byte[] bytes) { 
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        return loader.defineClass(className, bytes);
    }
    public Class<?> findClass(String className, Class clz) { 
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        return loader.lookupDefinedClass(className);
    }
    
    private static synchronized TypeHelperClassLoader getTypeHelperClassLoader(Class<?> l) {
        TypeHelperClassLoader ret = LOADER_MAP.get(l);
        if (ret == null) {
            ret = new TypeHelperClassLoader(l.getClassLoader());
            LOADER_MAP.put(l, ret);
        }
        return ret;
    }
    
    public static class TypeHelperClassLoader extends ClassLoader {
        Map<String, Class<?>> defined = new ConcurrentHashMap<String, Class<?>>();
        
        TypeHelperClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(name.replace('/', '.'));
        }
        
        public synchronized Class<?> defineClass(String name, byte bytes[]) {
            Class<?> ret = defined.get(name.replace('/', '.'));
            if (ret != null) {
                return ret;
            }
            if (name.endsWith("package-info")) {
                Package p = super.getPackage(name.substring(0, name.length() - 13));
                if (p == null) {
                    definePackage(name.substring(0, name.length() - 13).replace('/', '.'),
                                    null,
                                    null,
                                    null, 
                                    null,
                                    null,
                                    null,
                                    null);
                }
            }
            
            ret = super.defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
            defined.put(name.replace('/', '.'), ret);
            return ret;
        }
    }
    
}
