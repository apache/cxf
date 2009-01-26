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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.wsdl.Fault;

import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.model.JavaClass;
import org.apache.cxf.tools.java2wsdl.processor.internal.jaxws.FaultBean;

public final class FaultBeanGenerator extends BeanGenerator {

    private String getSEIPackage(final Method method) {
        String pkg = PackageUtils.getPackageName(method.getDeclaringClass());
        if (pkg.length() == 0) {
            return ToolConstants.DEFAULT_PACKAGE_NAME;
        } else {
            return pkg;
        }
    }
    
    protected Collection<JavaClass> generateBeanClasses(final ServiceInfo serviceInfo) {
        Set<Class> exceptionClasses = new HashSet<Class>();
        String seiPackageName = null;
        for (OperationInfo op : serviceInfo.getInterface().getOperations()) {
            Method method = (Method) op.getProperty("operation.method");
            exceptionClasses.addAll(getExceptionClasses(method));
            seiPackageName = getSEIPackage(method);
        }

        Collection<JavaClass> faultBeanClasses = new HashSet<JavaClass>();
        String defaultPackage = seiPackageName + ".jaxws";
        FaultBean bean = new FaultBean();
        for (Class clz : exceptionClasses) {
            if (!bean.faultBeanExists(clz)) {
                faultBeanClasses.add(bean.transform(clz, defaultPackage));
            }
        }

        return faultBeanClasses;
    }

    protected Set<Class> getExceptionClasses(final Method method) {
        Set<Class> exps = new HashSet<Class>();
        final Class[] exceptionClasses = method.getExceptionTypes();
        for (int i = 0; i < exceptionClasses.length; i++) {
            boolean exclude = false;
            Class exClazz = exceptionClasses[i];
            
            if (exClazz.equals(Exception.class) 
                || Fault.class.isAssignableFrom(exClazz)
                || exClazz.equals(RuntimeException.class)
                || exClazz.equals(Throwable.class)
                || exClazz.equals(RemoteException.class)
                || exClazz.equals(ServerException.class)) {
                continue;
            }

            Method[] expMethods = exClazz.getMethods();
            for (Method expMethod : expMethods) {
                if ("getFaultInfo".equals(expMethod.getName())) {
                    exclude = true;
                    break;
                }
            }
            if (exclude) {
                continue;
            }
            exps.add(exClazz);
        }
        return exps;
    }
}
