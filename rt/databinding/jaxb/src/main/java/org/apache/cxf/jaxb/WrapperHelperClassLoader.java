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

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.GeneratedClassClassLoader;
import org.apache.cxf.databinding.WrapperHelper;

/** If class has been generated during build time
 *  (use @see org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture capture to save bytes)
 *  you can set class loader to avoid class generation during runtime:
 *  bus.setExtension(new WrapperHelperClassLoader(bus), WrapperHelperCreator.class);
 * @author olivier dufour
 */
public class WrapperHelperClassLoader extends GeneratedClassClassLoader implements WrapperHelperCreator {
    public WrapperHelperClassLoader(Bus bus) {
        super(bus);
    }

    @Override
    public WrapperHelper compile(Class<?> wrapperType, Method[] setMethods, Method[] getMethods,
                                 Method[] jaxbMethods, Field[] fields, Object objectFactory) {

        int count = 1;
        String newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;

        Class<?> cls = findClass(newClassName, wrapperType);
        while (cls != null) {
            try {
                WrapperHelper helper = WrapperHelper.class.cast(cls.getDeclaredConstructor().newInstance());
                if (!helper.getSignature().equals(
                        WrapperHelperClassGenerator.computeSignature(setMethods, getMethods))) {
                    count++;
                    newClassName = wrapperType.getName() + "_WrapperTypeHelper" + count;
                    cls = findClass(newClassName, wrapperType);
                } else {
                    return helper;
                }
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
