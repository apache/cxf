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

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.util.ReflectionInvokationHandler.UnwrapParam;
import org.apache.cxf.common.util.ReflectionInvokationHandler.WrapReturn;

public class ASMHelper {
    protected static final Map<Class<?>, String> PRIMITIVE_MAP = new HashMap<Class<?>, String>();
    protected static final Map<Class<?>, String> NONPRIMITIVE_MAP = new HashMap<Class<?>, String>();
    
    protected static final Map<Class<?>, WeakReference<TypeHelperClassLoader>> LOADER_MAP 
        = new WeakIdentityHashMap<Class<?>, WeakReference<TypeHelperClassLoader>>();
    
    protected static boolean badASM;
    private static Class<?> cwClass;
    
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
    
    private static void tryClass(String s) {
        if (cwClass == null) {
            try {
                cwClass = ClassLoaderUtils.loadClass(s, ASMHelper.class);
            } catch (Throwable t) {
                //ignore
            }
        }
    }
    private static Class<?> getASMClassWriterClass() {
        //force this to make sure the proper OSGi import is generated
        return org.objectweb.asm.ClassWriter.class;
    }
    
    private static synchronized Class<?> getASMClass() throws ClassNotFoundException {
        if (cwClass == null) {
            //try the "real" asm first, then the others
            tryClass("org.objectweb.asm.ClassWriter"); 
            tryClass("org.apache.xbean.asm.ClassWriter"); 
            tryClass("org.springframework.asm.ClassWriter");
            if (cwClass == null) {
                cwClass = getASMClassWriterClass();
            }
        }
        return cwClass;
    }
    
    public static class Opcodes {
        //CHECKSTYLE:OFF
        //Will use reflection to set these based on the package name and such
        //so we don't want them "final" or the compiler will optimize them out 
        //to just "0" which we really don't want
        public static int ARETURN = 0;
        public static int ALOAD = 0;
        public static int IFNULL = 0;
        public static int CHECKCAST = 0;
        public static int INVOKEINTERFACE = 0;
        public static int GETFIELD = 0;
        public static int ASTORE = 0;
        public static int PUTFIELD = 0;
        public static int RETURN = 0;
        public static int INVOKESPECIAL = 0;
        public static int ACC_PUBLIC = 0;
        public static int ACC_FINAL = 0;
        public static int ACC_SUPER = 0;
        public static int ACC_PRIVATE = 0;
        public static int V1_5 = 0;
        public static int ACC_ABSTRACT = 0;
        public static int ACC_INTERFACE = 0;
        public static int ILOAD = 0;
        public static int IRETURN = 0;
        public static int NEW = 0;
        public static int DUP = 0;
        public static int ATHROW = 0;
        public static int INVOKEVIRTUAL = 0;
        public static int GOTO = 0;
        public static int POP = 0;
        public static int ACONST_NULL = 0;
        public static int IFNONNULL = 0;
        public static int SIPUSH = 0;
        public static int INVOKESTATIC = 0;
        //CHECKSTYLE:ON
        static {
            try {
                Class<?> cls = getASMClass();
                cls = ClassLoaderUtils.loadClass(cls.getPackage().getName() + ".Opcodes", cls);
                for (Field f1 : Opcodes.class.getDeclaredFields()) {
                    Field f = cls.getDeclaredField(f1.getName());
                    ReflectionUtil.setAccessible(f1).set(null, ReflectionUtil.setAccessible(f).get(null));
                }
            } catch (Throwable e) {
                //ignore
            }
        }
    }
    
    protected static String getMethodSignature(Method m) {
        StringBuilder buf = new StringBuilder("(");
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
    public static String getClassCode(java.lang.reflect.Type type) {
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
            } else {
                throw new IllegalArgumentException("Unable to determine type for: " + tv);
            }
        } else if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)type;
            StringBuilder a = new StringBuilder(getClassCode(pt.getRawType()));
            a.setLength(a.length() - 1);
            a.append('<');
            for (java.lang.reflect.Type t : pt.getActualTypeArguments()) {
                a.append(getClassCode(t));  
            }
            a.append(">;");
            return a.toString();
        } else if (type instanceof WildcardType) {
            WildcardType wt = (WildcardType)type;
            StringBuilder a = new StringBuilder();
            java.lang.reflect.Type[] lowBounds = wt.getLowerBounds();
            java.lang.reflect.Type[] upBounds = wt.getUpperBounds();
            for (java.lang.reflect.Type t : upBounds) {
                a.append("+");
                a.append(getClassCode(t));
            }
            for (java.lang.reflect.Type t : lowBounds) {
                a.append("-");
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
            try {
                // ASM 1.5.x/2.x
                Constructor<?> cons 
                    = cwClass.getConstructor(new Class<?>[] {Boolean.TYPE});
                
                try {
                    // got constructor, now check if it's 1.x which is very
                    // different from 2.x and 3.x
                    cwClass.getMethod("newConstInt", new Class<?>[] {Integer.TYPE});               
                    // newConstInt was removed in 2.x, if we get this far, we're
                    // using 1.5.x,
                    // set to null so we don't attempt to use it.
                    badASM = true;
                } catch (Throwable t) {
                    newCw = cons.newInstance(new Object[] {Boolean.TRUE});
                }
                
            } catch (Throwable e) {
                // ASM 3.x/4.x
                try {
                    Constructor<?> cons 
                        = cwClass.getConstructor(new Class<?>[] {Integer.TYPE});
                    int i = cwClass.getField("COMPUTE_MAXS").getInt(null);
                    i |= cwClass.getField("COMPUTE_FRAMES").getInt(null);
                    newCw = cons.newInstance(new Object[] {Integer.valueOf(i)});
                } catch (Throwable e1) {
                    // ignore
                }
            }
        }
        if (newCw != null) {
            return ReflectionInvokationHandler.createProxyWrapper(newCw, ClassWriter.class);
        }
        return null;
    }
    
    
    public Class<?> loadClass(String className, Class<?> clz , byte[] bytes) { 
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        synchronized (loader) {
            Class<?> cls = loader.lookupDefinedClass(className);
            if (cls == null) {
                return loader.defineClass(className, bytes);
            }
            return cls;
        }
    }
    public Class<?> findClass(String className, Class<?> clz) { 
        TypeHelperClassLoader loader = getTypeHelperClassLoader(clz);
        return loader.lookupDefinedClass(className);
    }
    
    private static synchronized TypeHelperClassLoader getTypeHelperClassLoader(Class<?> l) {
        WeakReference<TypeHelperClassLoader> ref = LOADER_MAP.get(l);
        TypeHelperClassLoader ret;
        if (ref == null || ref.get() == null) {
            ret = new TypeHelperClassLoader(l.getClassLoader());
            LOADER_MAP.put(l, new WeakReference<TypeHelperClassLoader>(ret));
        } else {
            ret = ref.get();
        }
        return ret;
    }
    
    public static class TypeHelperClassLoader extends ClassLoader {
        ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<String, Class<?>>();
        
        TypeHelperClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(name.replace('/', '.'));
        }
        
        public Class<?> defineClass(String name, byte bytes[]) {
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
            Class<?> tmpRet = defined.putIfAbsent(name.replace('/', '.'), ret);
            if (tmpRet != null) {
                ret = tmpRet;
            }
            return ret;
        }
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
    public interface ASMType {
        int getOpcode(int ireturn);
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
    
    public interface ClassWriter {
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String cls, boolean t);
        
        @WrapReturn(FieldVisitor.class)
        FieldVisitor visitField(int accPrivate, String fieldName, String classCode,
                                String fieldDescriptor, Object object);
        
        void visitEnd();
        byte[] toByteArray();
        
        @WrapReturn(MethodVisitor.class)        
        MethodVisitor visitMethod(int accPublic, String string, String string2, 
                                  String s3,
                                  String[] s4);
        void visit(int v15, int i, String newClassName, String object, String string, String[] object2);
        void visitSource(String arg0, String arg1);
    }
    
    public interface Label {
    }
    
    public interface FieldVisitor {
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String cls, boolean b);
        void visitEnd();
    }
    public interface MethodVisitor {
        void visitEnd();
        void visitLabel(@UnwrapParam(typeMethodName = "realType") Label l1);
        void visitMaxs(int i, int j);
        void visitLineNumber(int i, @UnwrapParam(typeMethodName = "realType") Label l0);
        void visitInsn(int return1);
        void visitVarInsn(int aload, int i);
        void visitCode();
        void visitLdcInsn(String sig);
        void visitLocalVariable(String string, 
                                String string2,
                                String string3, 
                                @UnwrapParam(typeMethodName = "realType") Label lBegin,
                                @UnwrapParam(typeMethodName = "realType") Label lEnd,
                                int i);
        void visitTypeInsn(int checkcast, String string);
        void visitMethodInsn(int invokevirtual, String periodToSlashes,
                             String name, String methodSignature);
        void visitIntInsn(int sipush, int x);
        void visitFieldInsn(int getfield, String periodToSlashes,
                            String string, String string2);
        void visitJumpInsn(int ifnonnull, @UnwrapParam(typeMethodName = "realType") Label nonNullLabel);
    }
    public interface AnnotationVisitor {
        void visit(String arg0, @UnwrapParam(typeMethodName = "realType") ASMType arg1);
        void visit(String arg0, Object arg1);
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitAnnotation(String arg0, String arg1);
        @WrapReturn(AnnotationVisitor.class)
        AnnotationVisitor visitArray(String arg0);
        void visitEnd();
        void visitEnum(String arg0, String arg1, String arg2);
    }
                                                            
    
}
