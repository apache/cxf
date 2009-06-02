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
package org.apache.cxf.jaxb;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.databinding.WrapperHelper;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

final class WrapperHelperCompiler extends ASMHelper {
  
    
    final Class<?> wrapperType;
    final Method setMethods[];
    final Method getMethods[];
    final Method jaxbMethods[];
    final Field fields[];
    final Object objectFactory;
    final ClassWriter cw;

    
    private WrapperHelperCompiler(Class<?> wrapperType,
                                  Method setMethods[],
                                  Method getMethods[],
                                  Method jaxbMethods[],
                                  Field fields[],
                                  Object objectFactory) {
        this.wrapperType = wrapperType;
        this.setMethods = setMethods;
        this.getMethods = getMethods;
        this.jaxbMethods = jaxbMethods;
        this.fields = fields;
        this.objectFactory = objectFactory;
        cw = createClassWriter();
    }

    static WrapperHelper compileWrapperHelper(Class<?> wrapperType,
                                              Method setMethods[],
                                              Method getMethods[],
                                              Method jaxbMethods[],
                                              Field fields[],
                                              Object objectFactory) {
        try {
            return new WrapperHelperCompiler(wrapperType,
                                        setMethods,
                                        getMethods,
                                        jaxbMethods,
                                        fields,
                                        objectFactory).compile();
            
        } catch (Throwable t) {
            // Some error - probably a bad version of ASM or similar
        }
        return null;
    }
    

    
    
    
    public WrapperHelper compile() {
        if (cw == null) {
            return null;
        }
        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
        newClassName = newClassName.replaceAll("\\$", ".");
        newClassName = periodToSlashes(newClassName);

        Class<?> cls = super.findClass(newClassName.replace('/', '.'), wrapperType);
        while (cls != null) {
            try {
                WrapperHelper helper = WrapperHelper.class.cast(cls.newInstance());
                if (!helper.getSignature().equals(computeSignature())) {
                    count++;
                    newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
                    newClassName = newClassName.replaceAll("\\$", ".");
                    newClassName = periodToSlashes(newClassName);
                    cls = super.findClass(newClassName.replace('/', '.'), wrapperType);
                } else {
                    return helper;
                }
            } catch (Exception e) {
                return null;
            }
        }
        
        
        
        cw.visit(Opcodes.V1_5,
                 Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                 newClassName,
                 null,
                 "java/lang/Object",
                 new String[] {periodToSlashes(WrapperHelper.class.getName())});
        
        addConstructor(newClassName, cw, objectFactory == null ? null : objectFactory.getClass());
        boolean b = addSignature();
        if (b) {
            b = addCreateWrapperObject(newClassName,
                                       objectFactory == null ? null : objectFactory.getClass());
        }
        if (b) {
            b = addGetWrapperParts(newClassName, wrapperType,
                           getMethods, fields, cw);
        }
                                                                          
        try {
            if (b) {
                cw.visitEnd();
                byte bt[] = cw.toByteArray();                
                Class<?> cl = loadClass(newClassName.replace('/', '.'), wrapperType, bt);
                Object o = cl.newInstance();
                return WrapperHelper.class.cast(o);
            }
        } catch (Throwable e) {
            // ignore, we'll just fall down to reflection based
            e.printStackTrace();
        }
        return null;
    }
    
    private String computeSignature() {
        StringBuilder b = new StringBuilder();
        b.append(setMethods.length).append(':');
        for (int x = 0; x < setMethods.length; x++) {
            if (getMethods[x] == null) {
                b.append("null,");
            } else {
                b.append(getMethods[x].getName()).append('/');
                b.append(getMethods[x].getReturnType().getName()).append(',');                
            }
        }
        return b.toString();
    }
    
    private boolean addSignature() {
        String sig = computeSignature();
        if (sig == null) {
            return false;
        }
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                          "getSignature", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(sig);
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(100, l0);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }

    private static void addConstructor(String newClassName, ClassWriter cw,  Class<?> objectFactory) {
        
        if (objectFactory != null) {
            String ofName = "L" + periodToSlashes(objectFactory.getName()) + ";";
            FieldVisitor fv = cw.visitField(0, "factory",
                                            ofName,
                                            null, null);
            fv.visitEnd();
        }
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        mv.visitLineNumber(102, l0);
        
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                           "java/lang/Object",
                           "<init>",
                           "()V");
        if (objectFactory != null) {
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitTypeInsn(Opcodes.NEW, periodToSlashes(objectFactory.getName()));
            mv.visitInsn(Opcodes.DUP);
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               periodToSlashes(objectFactory.getName()),
                               "<init>", "()V");
            mv.visitFieldInsn(Opcodes.PUTFIELD, periodToSlashes(newClassName),
                              "factory", "L" + periodToSlashes(objectFactory.getName()) + ";");
        } 
        
        mv.visitInsn(Opcodes.RETURN);
    
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLineNumber(103, l0);

        mv.visitLocalVariable("this", "L" + newClassName + ";", null, l0, l1, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
    
    private boolean addCreateWrapperObject(String newClassName,
                                           Class<?> objectFactoryClass) {
        
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                          "createWrapperObject",
                                          "(Ljava/util/List;)Ljava/lang/Object;",
                                          "(Ljava/util/List<*>;)Ljava/lang/Object;",
                                          new String[] {
                                              "org/apache/cxf/interceptor/Fault"
                                          });
        mv.visitCode();
        Label lBegin = new Label();
        mv.visitLabel(lBegin);
        mv.visitLineNumber(104, lBegin);

        mv.visitTypeInsn(Opcodes.NEW, periodToSlashes(wrapperType.getName()));
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, periodToSlashes(wrapperType.getName()),
                           "<init>", "()V");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
    
        for (int x = 0; x < setMethods.length; x++) {
            if (getMethods[x] == null) { 
                if (setMethods[x] == null
                    && fields[x] == null) {
                    // null placeholder
                    continue;
                } else {
                    return false;
                }
            }
            Class<?> tp = getMethods[x].getReturnType();
            mv.visitVarInsn(Opcodes.ALOAD, 2);            
            
            if (List.class.isAssignableFrom(tp)) {
                doCollection(mv, x);
            } else { 
                if (JAXBElement.class.isAssignableFrom(tp)) {
                    mv.visitVarInsn(Opcodes.ALOAD, 0);
                    mv.visitFieldInsn(Opcodes.GETFIELD, periodToSlashes(newClassName),
                                      "factory",
                                      "L" + periodToSlashes(objectFactoryClass.getName()) + ";");
                }
                mv.visitVarInsn(Opcodes.ALOAD, 1);
                mv.visitIntInsn(Opcodes.BIPUSH, x);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;");
                
                if (tp.isPrimitive()) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, NONPRIMITIVE_MAP.get(tp));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, NONPRIMITIVE_MAP.get(tp), 
                                       tp.getName() + "Value", "()" + PRIMITIVE_MAP.get(tp));
                } else if (JAXBElement.class.isAssignableFrom(tp)) {
                    mv.visitTypeInsn(Opcodes.CHECKCAST,
                                     periodToSlashes(jaxbMethods[x].getParameterTypes()[0].getName()));
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, periodToSlashes(objectFactoryClass.getName()),
                                       jaxbMethods[x].getName(),
                                       getMethodSignature(jaxbMethods[x]));
                } else if (tp.isArray()) { 
                    mv.visitTypeInsn(Opcodes.CHECKCAST, getClassCode(tp));
                } else {
                    mv.visitTypeInsn(Opcodes.CHECKCAST, periodToSlashes(tp.getName()));
                }
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                   periodToSlashes(wrapperType.getName()),
                                   setMethods[x].getName(), "(" + getClassCode(tp) + ")V");
            }
        }
        
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
    
        Label lEnd = new Label();
        mv.visitLabel(lEnd);
        mv.visitLocalVariable("this", "L" + newClassName + ";", null, lBegin, lEnd, 0);
        mv.visitLocalVariable("lst", "Ljava/util/List;", "Ljava/util/List<*>;", lBegin, lEnd, 1);
        mv.visitLocalVariable("ok", "L" + periodToSlashes(wrapperType.getName()) + ";",
                              null, lBegin, lEnd, 2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }
    
    private void doCollection(MethodVisitor mv, int x) {
        // List aVal = obj.getA();
        // List newA = (List)lst.get(99);
        // if (aVal == null) {
        // obj.setA(newA);
        // } else if (newA != null) {
        // aVal.addAll(newA);
        // }
        
        Label l3 = new Label();
        mv.visitLabel(l3);
        mv.visitLineNumber(114, l3);

        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                           periodToSlashes(wrapperType.getName()),
                           getMethods[x].getName(),
                           getMethodSignature(getMethods[x]));
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitIntInsn(Opcodes.BIPUSH, x);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                           "get", "(I)Ljava/lang/Object;");
        mv.visitTypeInsn(Opcodes.CHECKCAST, "java/util/List");
        mv.visitVarInsn(Opcodes.ASTORE, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        Label nonNullLabel = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, nonNullLabel);

        if (setMethods[x] == null) {
            mv.visitTypeInsn(Opcodes.NEW, "java/lang/RuntimeException");
            mv.visitInsn(Opcodes.DUP);
            mv.visitLdcInsn(getMethods[x].getName() + " returned null and there isn't a set method.");
            mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                               "java/lang/RuntimeException",
                               "<init>", "(Ljava/lang/String;)V");
            mv.visitInsn(Opcodes.ATHROW);
        } else {
            mv.visitVarInsn(Opcodes.ALOAD, 2);
            mv.visitVarInsn(Opcodes.ALOAD, 4);
            mv.visitTypeInsn(Opcodes.CHECKCAST,
                             getMethods[x].getReturnType().getName().replace('.', '/'));
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                               periodToSlashes(wrapperType.getName()),
                               setMethods[x].getName(),
                               getMethodSignature(setMethods[x]));
        }
        Label jumpOverLabel = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, jumpOverLabel);
        mv.visitLabel(nonNullLabel);
        mv.visitLineNumber(106, nonNullLabel);

        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitJumpInsn(Opcodes.IFNULL, jumpOverLabel);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                           "java/util/List", "addAll", "(Ljava/util/Collection;)Z");
        mv.visitInsn(Opcodes.POP);
        mv.visitLabel(jumpOverLabel);
        mv.visitLineNumber(107, jumpOverLabel);
    }
    
    private static boolean addGetWrapperParts(String newClassName,
                                           Class<?> wrapperClass,
                                           Method getMethods[],
                                           Field fields[],
                                           ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC,
                                          "getWrapperParts",
                                          "(Ljava/lang/Object;)Ljava/util/List;",
                                          "(Ljava/lang/Object;)Ljava/util/List<Ljava/lang/Object;>;", 
                                          new String[] {
                                              "org/apache/cxf/interceptor/Fault" 
                                          });
        mv.visitCode();
        Label lBegin = new Label();
        mv.visitLabel(lBegin);
        mv.visitLineNumber(108, lBegin);
               
        // the ret List
        mv.visitTypeInsn(Opcodes.NEW, "java/util/ArrayList");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        
        // cast the Object to the wrapperType type
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitTypeInsn(Opcodes.CHECKCAST, periodToSlashes(wrapperClass.getName()));
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        
        for (int x = 0; x < getMethods.length; x++) {
            Method method = getMethods[x];
            if (method == null && fields[x] != null) {
                // fallback to reflection mode
                return false;
            }
            
            if (method == null) {
                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitLineNumber(200 + x, l3);

                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitInsn(Opcodes.ACONST_NULL);
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List",
                                   "add", "(Ljava/lang/Object;)Z");
                mv.visitInsn(Opcodes.POP);
            } else {
                Label l3 = new Label();
                mv.visitLabel(l3);
                mv.visitLineNumber(250 + x, l3);
                
                mv.visitVarInsn(Opcodes.ALOAD, 2);
                mv.visitVarInsn(Opcodes.ALOAD, 3);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, 
                                   periodToSlashes(wrapperClass.getName()), 
                                   method.getName(), 
                                   getMethodSignature(method));
                if (method.getReturnType().isPrimitive()) {
                    // wrap into Object type
                    createObjectWrapper(mv, method.getReturnType());
                }
                if (JAXBElement.class.isAssignableFrom(method.getReturnType())) {
                    mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                       "javax/xml/bind/JAXBElement",
                                       "getValue", "()Ljava/lang/Object;");
                }
                
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
                mv.visitInsn(Opcodes.POP);
            }
        }
        
        // return the list
        Label l2 = new Label();
        mv.visitLabel(l2);
        mv.visitLineNumber(108, l2);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitInsn(Opcodes.ARETURN);
        
        
        Label lEnd = new Label();
        mv.visitLabel(lEnd);
        mv.visitLocalVariable("this", "L" + newClassName + ";", null, lBegin, lEnd, 0);
        mv.visitLocalVariable("o", "Ljava/lang/Object;", null, lBegin, lEnd, 1);
        mv.visitLocalVariable("ret", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/Object;>;",
                              lBegin, lEnd, 2);
        mv.visitLocalVariable("ok", "L" + periodToSlashes(wrapperClass.getName()) + ";",
                              null, lBegin, lEnd, 3);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }
    
    


    private static void createObjectWrapper(MethodVisitor mv, Class<?> cl) {
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, NONPRIMITIVE_MAP.get(cl),
                           "valueOf", "(" + PRIMITIVE_MAP.get(cl) + ")L" 
                           + NONPRIMITIVE_MAP.get(cl) + ";");
    }
    

    
    
        


}
