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
package org.apache.cxf.jaxws.spi;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.cxf.Bus;
import org.apache.cxf.common.spi.GeneratedClassClassLoader;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxws.WrapperClassGenerator;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.wsdl.service.factory.ReflectionServiceFactoryBean;

/** If class has been generated during build time
 *  (use @see org.apache.cxf.common.spi.GeneratedClassClassLoaderCapture capture to save bytes)
 *  you can set class loader to avoid class generation during runtime:
 *  bus.setExtension(new GeneratedWrapperClassLoader(bus), WrapperClassCreator.class);
 * @author olivier dufour
 */
public class WrapperClassLoader extends GeneratedClassClassLoader implements WrapperClassCreator {
    public WrapperClassLoader(Bus bus) {
        super(bus);
    }

    @Override
    public Set<Class<?>> generate(JaxWsServiceFactoryBean factory, InterfaceInfo interfaceInfo, boolean q) {
        Set<Class<?>> wrapperBeans = new LinkedHashSet<>();
        for (OperationInfo opInfo : interfaceInfo.getOperations()) {
            if (opInfo.isUnwrappedCapable()) {
                Method method = (Method)opInfo.getProperty(ReflectionServiceFactoryBean.METHOD);
                if (method == null) {
                    continue;
                }
                MessagePartInfo inf = opInfo.getInput().getFirstMessagePart();
                if (inf.getTypeClass() == null) {
                    MessageInfo messageInfo = opInfo.getUnwrappedOperation().getInput();
                    wrapperBeans.add(createWrapperClass(inf,
                            messageInfo,
                            opInfo,
                            method,
                            true,
                            factory));
                }
                MessageInfo messageInfo = opInfo.getUnwrappedOperation().getOutput();
                if (messageInfo != null) {
                    inf = opInfo.getOutput().getFirstMessagePart();
                    if (inf.getTypeClass() == null) {
                        wrapperBeans.add(createWrapperClass(inf,
                                messageInfo,
                                opInfo,
                                method,
                                false,
                                factory));
                    }
                }
            }
        }
        return wrapperBeans;
    }

    private Class<?> createWrapperClass(MessagePartInfo wrapperPart,
                                    MessageInfo messageInfo,
                                    OperationInfo op,
                                    Method method,
                                    boolean isRequest,
                                    JaxWsServiceFactoryBean factory) {
        boolean anonymous = factory.getAnonymousWrapperTypes();

        String pkg = getPackageName(method) + ".jaxws_asm" + (anonymous ? "_an" : "");
        String className = pkg + "."
                + StringUtils.capitalize(op.getName().getLocalPart());
        if (!isRequest) {
            className = className + "Response";
        }

        Class<?> def = findClass(className, method.getDeclaringClass());
        String origClassName = className;
        int count = 0;
        while (def != null) {
            Boolean b = messageInfo.getProperty("parameterized", Boolean.class);
            if (b != null && b) {
                className = origClassName + (++count);
                def = findClass(className, method.getDeclaringClass());
            } else {
                wrapperPart.setTypeClass(def);
                return def;
            }
        }
        //throw new ClassNotFoundException(origClassName);
        return null;
    }
    private String getPackageName(Method method) {
        String pkg = PackageUtils.getPackageName(method.getDeclaringClass());
        return pkg.length() == 0 ? WrapperClassGenerator.DEFAULT_PACKAGE_NAME : pkg;
    }
}
