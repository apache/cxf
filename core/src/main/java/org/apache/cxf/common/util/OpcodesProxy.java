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

import java.lang.reflect.Field;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public class OpcodesProxy {
    //CHECKSTYLE:OFF
    //Will use reflection to set these based on the package name and such
    //so we don't want them "final" or the compiler will optimize them out
    //to just "0" which we really don't want
    public int ARETURN = 0;
    public int ALOAD = 0;
    public int IFNULL = 0;
    public int CHECKCAST = 0;
    public int INVOKEINTERFACE = 0;
    public int GETFIELD = 0;
    public int GETSTATIC = 0;
    public int ASTORE = 0;
    public int PUTFIELD = 0;
    public int PUTSTATIC = 0;
    public int RETURN = 0;
    public int F_APPEND = 0;
    public int F_SAME = 0;
    public int F_SAME1 = 0;
    public int INVOKESPECIAL = 0;
    public int ACC_PUBLIC = 0;
    public int ACC_FINAL = 0;
    public int ACC_SUPER = 0;
    public int ACC_PRIVATE = 0;
    public int ACC_STATIC = 0;
    public int V1_5 = 0;
    public int V1_6 = 0;
    public int V1_7 = 0;
    public int ACC_ABSTRACT = 0;
    public int ACC_INTERFACE = 0;
    public int ACC_SYNTHETIC = 0;
    public int ILOAD = 0;
    public int ISTORE = 0;
    public int AALOAD = 0;
    public int ARRAYLENGTH = 0;
    public int IRETURN = 0;
    public int NEW = 0;
    public int ANEWARRAY = 0;
    public int DUP = 0;
    public int ATHROW = 0;
    public int INVOKEVIRTUAL = 0;
    public int GOTO = 0;
    public int POP = 0;
    public int ACONST_NULL = 0;
    public int IFNONNULL = 0;
    public int SIPUSH = 0;
    public int INVOKESTATIC = 0;
    public int ICONST_0;
    public int ICONST_1;
    public int LCONST_0;
    public int FCONST_0;
    public int DCONST_0;
    public int IF_ICMPLT = 0;
    public java.lang.Integer INTEGER;

    public OpcodesProxy(ASMHelper helper) {
        try {
            Class<?> cls = helper.getASMClass();
            cls = ClassLoaderUtils.loadClass(cls.getPackage().getName() + ".Opcodes", cls);
            for (Field f1 : OpcodesProxy.class.getDeclaredFields()) {
                Field f = cls.getDeclaredField(f1.getName());
                ReflectionUtil.setAccessible(f1).set(this, ReflectionUtil.setAccessible(f).get(null));
            }
        } catch (Throwable e) {
            //ignore
        }
    }
    //CHECKSTYLE:ON
}
