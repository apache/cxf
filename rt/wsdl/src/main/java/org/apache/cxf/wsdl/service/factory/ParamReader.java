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

package org.apache.cxf.wsdl.service.factory;

// import org.apache.axis.utils.Messages;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
 */
class ParamReader {
    private Map<Member, String[]> paramNames = new HashMap<>();

    /**
     * process a class file, given it's class. We'll use the defining
     * classloader to locate the bytecode.
     *
     * @param c
     * @throws IOException
     */
    ParamReader(Class<?> c) throws IOException {
        final Map<String, ParameterNameExtractor> parameterNameExtractor = new HashMap<>();
        for (Method method : c.getDeclaredMethods()) {
            parameterNameExtractor.put(key(method.getName(), Type.getMethodDescriptor(method)),
                    new ParameterNameExtractor(method, method.getParameterTypes()));
        }
        for (Constructor<?> constructor : c.getDeclaredConstructors()) {
            parameterNameExtractor.put(key("<init>", Type.getConstructorDescriptor(constructor)),
                    new ParameterNameExtractor(constructor, constructor.getParameterTypes()));
        }
        ClassReader reader;
        try (InputStream in = c.getResourceAsStream(c.getSimpleName() + ".class")) {
            reader = new ClassReader(in);
        }
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                return parameterNameExtractor.get(key(name, desc));
            }
        }, 0);
        for (ParameterNameExtractor e : parameterNameExtractor.values()) {
            paramNames.put(e.member, e.parameterNames);
        }
    }

    /**
     * Retrieve a list of function parameter names from a method Returns null if
     * unable to read parameter names (i.e. bytecode not built with debug).
     */
    static String[] getParameterNamesFromDebugInfo(Method method) {
        try {
            return new ParamReader(method.getDeclaringClass()).getParameterNames(method);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * return the names of the declared parameters for the given constructor. If
     * we cannot determine the names, return null. The returned array will have
     * one name per parameter. The length of the array will be the same as the
     * length of the Class[] array returned by Constructor.getParameterTypes().
     *
     * @param ctor
     * @return String[] array of names, one per parameter, or null
     */
    public String[] getParameterNames(Constructor<?> ctor) {
        return paramNames.get(ctor);
    }

    /**
     * return the names of the declared parameters for the given method. If we
     * cannot determine the names, return null. The returned array will have one
     * name per parameter. The length of the array will be the same as the
     * length of the Class[] array returned by Method.getParameterTypes().
     *
     * @param method
     * @return String[] array of names, one per parameter, or null
     */
    public String[] getParameterNames(Method method) {
        return paramNames.get(method);
    }

    private static String key(String memberName, String memberDescription) {
        return memberName + ", " + memberDescription;
    }

    /**
     * ASM-method-visitor extracting the names of method-parameters from the debug-info
     */
    private static class ParameterNameExtractor extends MethodVisitor {
        final Member member;
        final String[] parameterNames;
        private Map<Integer, Integer> varToParam = new TreeMap<>();

        ParameterNameExtractor(Member member, Class<?>[] parameterTypes) {
            super(Opcodes.ASM5);
            this.member = member;
            parameterNames = new String[parameterTypes.length];
            int localVariableIndex = Modifier.isStatic(member.getModifiers()) ? 0 : 1;
            for (int i = 0; i < parameterTypes.length; i++) {
                varToParam.put(localVariableIndex, i);
                localVariableIndex++;
                Class t = parameterTypes[i];
                if (Long.TYPE.equals(t) || Double.TYPE.equals(t)) {
                    localVariableIndex++;
                }
            }
        }

        @Override
        public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
            Integer parameterIndex = varToParam.get(index);
            if (parameterIndex != null) {
                parameterNames[parameterIndex] = name;
            }
            super.visitLocalVariable(name, desc, signature, start, end, index);
        }
    }
}
