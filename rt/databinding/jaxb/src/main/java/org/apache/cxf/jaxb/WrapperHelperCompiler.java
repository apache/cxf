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

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.ClassGeneratorClassLoader;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.OpcodesProxy;
import org.apache.cxf.databinding.WrapperHelper;

public final class WrapperHelperCompiler extends ClassGeneratorClassLoader implements WrapperHelperCreator {

    private Class<?> wrapperType;
    private Method[] setMethods;
    private Method[] getMethods;
    private Method[] jaxbMethods;
    private Field[] fields;
    private Object objectFactory;
    private ASMHelper.ClassWriter cw;
    private ASMHelper asmhelper;

    WrapperHelperCompiler(Bus bus) {
        super(bus);
    }

    public WrapperHelper compile(Bus bus, Class<?> wt, Method[] setters,
                                 Method[] getters, Method[] jms,
                                 Field[] fs, Object of) {
        this.wrapperType = wt;
        this.setMethods = setters;
        this.getMethods = getters;
        this.jaxbMethods = jms;
        this.fields = fs;
        this.objectFactory = of;
        asmhelper = bus.getExtension(ASMHelper.class);
        cw = asmhelper.createClassWriter();

        if (cw == null) {
            return null;
        }
        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
        newClassName = newClassName.replaceAll("\\$", ".");
        newClassName = asmhelper.periodToSlashes(newClassName);

        Class<?> cls = findClass(newClassName.replace('/', '.'));
        while (cls != null) {
            try {
                WrapperHelper helper = WrapperHelper.class.cast(cls.newInstance());
                if (!helper.getSignature().equals(computeSignature(setMethods, getMethods))) {
                    count++;
                    newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
                    newClassName = newClassName.replaceAll("\\$", ".");
                    newClassName = asmhelper.periodToSlashes(newClassName);
                    cls = findClass(newClassName.replace('/', '.'));
                } else {
                    return helper;
                }
            } catch (Exception e) {
                return null;
            }
        }

        OpcodesProxy opCodes = asmhelper.getOpCodes();

        cw.visit(opCodes.V1_5,
                 opCodes.ACC_PUBLIC | opCodes.ACC_SUPER,
                 newClassName,
                 null,
                 "java/lang/Object",
                 new String[] {asmhelper.periodToSlashes(WrapperHelper.class.getName())});

        addConstructor(newClassName, objectFactory == null ? null : objectFactory.getClass());
        boolean b = addSignature();
        if (b) {
            b = addCreateWrapperObject(newClassName,
                                       objectFactory == null ? null : objectFactory.getClass());
        }
        if (b) {
            b = addGetWrapperParts(newClassName, wrapperType);
        }

        try {
            if (b) {
                cw.visitEnd();
                byte[] bt = cw.toByteArray();
                Class<?> cl = loadClass(newClassName.replace('/', '.'), bt);
                Object o = cl.newInstance();
                return WrapperHelper.class.cast(o);
            }
        } catch (Throwable e) {
            // ignore, we'll just fall down to reflection based
        }
        return null;
    }

    public static String computeSignature(Method[] setMethods, Method[] getMethods) {
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
        OpcodesProxy opCodes = asmhelper.getOpCodes();
        String sig = computeSignature(setMethods, getMethods);
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC,
                                          "getSignature", "()Ljava/lang/String;", null, null);
        mv.visitCode();
        mv.visitLdcInsn(sig);
        ASMHelper.Label l0 = asmhelper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(100, l0);
        mv.visitInsn(opCodes.ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }

    private void addConstructor(String newClassName, Class<?> objectFactoryCls) {

        if (objectFactoryCls != null) {
            String ofName = "L" + asmhelper.periodToSlashes(objectFactoryCls.getName()) + ";";
            ASMHelper.FieldVisitor fv = cw.visitField(0, "factory",
                                            ofName,
                                            null, null);
            fv.visitEnd();
        }
        OpcodesProxy opCodes = asmhelper.getOpCodes();

        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        ASMHelper.Label l0 = asmhelper.createLabel();
        mv.visitLabel(l0);
        mv.visitLineNumber(102, l0);

        mv.visitVarInsn(opCodes.ALOAD, 0);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                           "java/lang/Object",
                           "<init>",
                           "()V", false);
        if (objectFactoryCls != null) {
            mv.visitVarInsn(opCodes.ALOAD, 0);
            mv.visitTypeInsn(opCodes.NEW, asmhelper.periodToSlashes(objectFactoryCls.getName()));
            mv.visitInsn(opCodes.DUP);
            mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                    asmhelper.periodToSlashes(objectFactoryCls.getName()),
                               "<init>", "()V", false);
            mv.visitFieldInsn(opCodes.PUTFIELD, asmhelper.periodToSlashes(newClassName),
                              "factory", "L" + asmhelper.periodToSlashes(objectFactoryCls.getName()) + ";");
        }

        mv.visitInsn(opCodes.RETURN);

        ASMHelper.Label l1 = asmhelper.createLabel();
        mv.visitLabel(l1);
        mv.visitLineNumber(103, l0);

        mv.visitLocalVariable("this", "L" + newClassName + ";", null, l0, l1, 0);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private boolean addCreateWrapperObject(String newClassName, Class<?> objectFactoryClass) {

        OpcodesProxy opCodes = asmhelper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC,
                                          "createWrapperObject",
                                          "(Ljava/util/List;)Ljava/lang/Object;",
                                          "(Ljava/util/List<*>;)Ljava/lang/Object;",
                                          new String[] {
                                              "org/apache/cxf/interceptor/Fault"
                                          });
        mv.visitCode();
        ASMHelper.Label lBegin = asmhelper.createLabel();
        mv.visitLabel(lBegin);
        mv.visitLineNumber(104, lBegin);

        mv.visitTypeInsn(opCodes.NEW, asmhelper.periodToSlashes(wrapperType.getName()));
        mv.visitInsn(opCodes.DUP);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, asmhelper.periodToSlashes(wrapperType.getName()),
                           "<init>", "()V", false);
        mv.visitVarInsn(opCodes.ASTORE, 2);

        for (int x = 0; x < setMethods.length; x++) {
            if (getMethods[x] == null) {
                if (setMethods[x] == null
                    && fields[x] == null) {
                    // null placeholder
                    continue;
                }
                return false;
            }
            Class<?> tp = getMethods[x].getReturnType();
            mv.visitVarInsn(opCodes.ALOAD, 2);

            if (List.class.isAssignableFrom(tp)) {
                doCollection(mv, x);
            } else {
                if (JAXBElement.class.isAssignableFrom(tp)) {
                    mv.visitVarInsn(opCodes.ALOAD, 0);
                    mv.visitFieldInsn(opCodes.GETFIELD, asmhelper.periodToSlashes(newClassName),
                                      "factory",
                                      "L" + asmhelper.periodToSlashes(objectFactoryClass.getName()) + ";");
                }
                mv.visitVarInsn(opCodes.ALOAD, 1);
                mv.visitIntInsn(opCodes.SIPUSH, x);
                mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "java/util/List", "get", "(I)Ljava/lang/Object;", true);

                if (tp.isPrimitive()) {
                    mv.visitTypeInsn(opCodes.CHECKCAST, asmhelper.getNonPrimitive(tp));
                    ASMHelper.Label l45 = asmhelper.createLabel();
                    ASMHelper.Label l46 = asmhelper.createLabel();
                    mv.visitInsn(opCodes.DUP);
                    mv.visitJumpInsn(opCodes.IFNULL, l45);
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL, asmhelper.getNonPrimitive(tp),
                                       tp.getName() + "Value", "()" + asmhelper.getPrimitive(tp), false);
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                            asmhelper.periodToSlashes(wrapperType.getName()),
                                       setMethods[x].getName(), "(" + asmhelper.getClassCode(tp) + ")V", false);
                    mv.visitJumpInsn(opCodes.GOTO, l46);
                    mv.visitLabel(l45);
                    mv.visitInsn(opCodes.POP);
                    mv.visitLabel(l46);
                } else if (JAXBElement.class.isAssignableFrom(tp)) {
                    mv.visitTypeInsn(opCodes.CHECKCAST,
                                     asmhelper.periodToSlashes(jaxbMethods[x].getParameterTypes()[0].getName()));
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL, asmhelper.periodToSlashes(objectFactoryClass.getName()),
                                       jaxbMethods[x].getName(),
                                       asmhelper.getMethodSignature(jaxbMethods[x]), false);
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                                       asmhelper.periodToSlashes(wrapperType.getName()),
                                       setMethods[x].getName(), "(" + asmhelper.getClassCode(tp) + ")V", false);
                } else if (tp.isArray()) {
                    mv.visitTypeInsn(opCodes.CHECKCAST, asmhelper.getClassCode(tp));
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                                       asmhelper.periodToSlashes(wrapperType.getName()),
                                       setMethods[x].getName(), "(" + asmhelper.getClassCode(tp) + ")V", false);
                } else {
                    mv.visitTypeInsn(opCodes.CHECKCAST, asmhelper.periodToSlashes(tp.getName()));
                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                                       asmhelper.periodToSlashes(wrapperType.getName()),
                                       setMethods[x].getName(), "(" + asmhelper.getClassCode(tp) + ")V", false);
                }
            }
        }

        mv.visitVarInsn(opCodes.ALOAD, 2);
        mv.visitInsn(opCodes.ARETURN);

        ASMHelper.Label lEnd = asmhelper.createLabel();
        mv.visitLabel(lEnd);
        mv.visitLocalVariable("this", "L" + newClassName + ";", null, lBegin, lEnd, 0);
        mv.visitLocalVariable("lst", "Ljava/util/List;", "Ljava/util/List<*>;", lBegin, lEnd, 1);
        mv.visitLocalVariable("ok", "L" + asmhelper.periodToSlashes(wrapperType.getName()) + ";",
                              null, lBegin, lEnd, 2);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }

    private void doCollection(ASMHelper.MethodVisitor mv, int x) {
        // List aVal = obj.getA();
        // List newA = (List)lst.get(99);
        // if (aVal == null) {
        // obj.setA(newA);
        // } else if (newA != null) {
        // aVal.addAll(newA);
        // }

        OpcodesProxy opCodes = asmhelper.getOpCodes();
        ASMHelper.Label l3 = asmhelper.createLabel();
        mv.visitLabel(l3);
        mv.visitLineNumber(114, l3);

        mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                           asmhelper.periodToSlashes(wrapperType.getName()),
                           getMethods[x].getName(),
                           asmhelper.getMethodSignature(getMethods[x]), false);
        mv.visitVarInsn(opCodes.ASTORE, 3);
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitIntInsn(opCodes.SIPUSH, x);
        mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "java/util/List",
                           "get", "(I)Ljava/lang/Object;", true);
        mv.visitTypeInsn(opCodes.CHECKCAST, "java/util/List");
        mv.visitVarInsn(opCodes.ASTORE, 4);
        mv.visitVarInsn(opCodes.ALOAD, 3);
        ASMHelper.Label nonNullLabel = asmhelper.createLabel();
        mv.visitJumpInsn(opCodes.IFNONNULL, nonNullLabel);

        if (setMethods[x] == null) {
            mv.visitTypeInsn(opCodes.NEW, "java/lang/RuntimeException");
            mv.visitInsn(opCodes.DUP);
            mv.visitLdcInsn(getMethods[x].getName() + " returned null and there isn't a set method.");
            mv.visitMethodInsn(opCodes.INVOKESPECIAL,
                               "java/lang/RuntimeException",
                               "<init>", "(Ljava/lang/String;)V", false);
            mv.visitInsn(opCodes.ATHROW);
        } else {
            mv.visitVarInsn(opCodes.ALOAD, 2);
            mv.visitVarInsn(opCodes.ALOAD, 4);
            mv.visitTypeInsn(opCodes.CHECKCAST,
                             getMethods[x].getReturnType().getName().replace('.', '/'));
            mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                               asmhelper.periodToSlashes(wrapperType.getName()),
                               setMethods[x].getName(),
                               asmhelper.getMethodSignature(setMethods[x]), false);
        }
        ASMHelper.Label jumpOverLabel = asmhelper.createLabel();
        mv.visitJumpInsn(opCodes.GOTO, jumpOverLabel);
        mv.visitLabel(nonNullLabel);
        mv.visitLineNumber(106, nonNullLabel);

        mv.visitVarInsn(opCodes.ALOAD, 4);
        mv.visitJumpInsn(opCodes.IFNULL, jumpOverLabel);
        mv.visitVarInsn(opCodes.ALOAD, 3);
        mv.visitVarInsn(opCodes.ALOAD, 4);
        mv.visitMethodInsn(opCodes.INVOKEINTERFACE,
                           "java/util/List", "addAll", "(Ljava/util/Collection;)Z", true);
        mv.visitInsn(opCodes.POP);
        mv.visitLabel(jumpOverLabel);
        mv.visitLineNumber(107, jumpOverLabel);
    }

    private boolean addGetWrapperParts(String newClassName, Class<?> wrapperClass) {
        OpcodesProxy opCodes = asmhelper.getOpCodes();
        ASMHelper.MethodVisitor mv = cw.visitMethod(opCodes.ACC_PUBLIC,
                                          "getWrapperParts",
                                          "(Ljava/lang/Object;)Ljava/util/List;",
                                          "(Ljava/lang/Object;)Ljava/util/List<Ljava/lang/Object;>;",
                                          new String[] {
                                              "org/apache/cxf/interceptor/Fault"
                                          });
        mv.visitCode();
        ASMHelper.Label lBegin = asmhelper.createLabel();
        mv.visitLabel(lBegin);
        mv.visitLineNumber(108, lBegin);

        // the ret List
        mv.visitTypeInsn(opCodes.NEW, "java/util/ArrayList");
        mv.visitInsn(opCodes.DUP);
        mv.visitMethodInsn(opCodes.INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V", false);
        mv.visitVarInsn(opCodes.ASTORE, 2);

        // cast the Object to the wrapperType type
        mv.visitVarInsn(opCodes.ALOAD, 1);
        mv.visitTypeInsn(opCodes.CHECKCAST, asmhelper.periodToSlashes(wrapperClass.getName()));
        mv.visitVarInsn(opCodes.ASTORE, 3);

        for (int x = 0; x < getMethods.length; x++) {
            Method method = getMethods[x];
            if (method == null && fields[x] != null) {
                // fallback to reflection mode
                return false;
            }

            if (method == null) {
                ASMHelper.Label l3 = asmhelper.createLabel();
                mv.visitLabel(l3);
                mv.visitLineNumber(200 + x, l3);

                mv.visitVarInsn(opCodes.ALOAD, 2);
                mv.visitInsn(opCodes.ACONST_NULL);
                mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "java/util/List",
                                   "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(opCodes.POP);
            } else {
                ASMHelper.Label l3 = asmhelper.createLabel();
                mv.visitLabel(l3);
                mv.visitLineNumber(250 + x, l3);

                mv.visitVarInsn(opCodes.ALOAD, 2);
                mv.visitVarInsn(opCodes.ALOAD, 3);
                mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                                   asmhelper.periodToSlashes(wrapperClass.getName()),
                                   method.getName(),
                        asmhelper.getMethodSignature(method), false);
                if (method.getReturnType().isPrimitive()) {
                    // wrap into Object type
                    createObjectWrapper(mv, method.getReturnType());
                }
                if (JAXBElement.class.isAssignableFrom(method.getReturnType())) {
                    ASMHelper.Label jumpOverLabel = asmhelper.createLabel();
                    mv.visitInsn(opCodes.DUP);
                    mv.visitJumpInsn(opCodes.IFNULL, jumpOverLabel);

                    mv.visitMethodInsn(opCodes.INVOKEVIRTUAL,
                                       "javax/xml/bind/JAXBElement",
                                       "getValue", "()Ljava/lang/Object;", false);
                    mv.visitLabel(jumpOverLabel);
                }

                mv.visitMethodInsn(opCodes.INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(opCodes.POP);
            }
        }

        // return the list
        ASMHelper.Label l2 = asmhelper.createLabel();
        mv.visitLabel(l2);
        mv.visitLineNumber(108, l2);
        mv.visitVarInsn(opCodes.ALOAD, 2);
        mv.visitInsn(opCodes.ARETURN);


        ASMHelper.Label lEnd = asmhelper.createLabel();
        mv.visitLabel(lEnd);
        mv.visitLocalVariable("this", "L" + newClassName + ";", null, lBegin, lEnd, 0);
        mv.visitLocalVariable("o", "Ljava/lang/Object;", null, lBegin, lEnd, 1);
        mv.visitLocalVariable("ret", "Ljava/util/List;", "Ljava/util/List<Ljava/lang/Object;>;",
                              lBegin, lEnd, 2);
        mv.visitLocalVariable("ok", "L" + asmhelper.periodToSlashes(wrapperClass.getName()) + ";",
                              null, lBegin, lEnd, 3);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        return true;
    }




    private void createObjectWrapper(ASMHelper.MethodVisitor mv, Class<?> cl) {
        OpcodesProxy opCodes = asmhelper.getOpCodes();
        mv.visitMethodInsn(opCodes.INVOKESTATIC, asmhelper.getNonPrimitive(cl),
                           "valueOf", "(" + asmhelper.getPrimitive(cl) + ")L"
                           + asmhelper.getNonPrimitive(cl) + ";", false);
    }
}
