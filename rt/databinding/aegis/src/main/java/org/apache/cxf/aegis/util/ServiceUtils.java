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
package org.apache.cxf.aegis.util;

import java.lang.reflect.Method;

import javax.xml.namespace.QName;

/**
 * Helps when constructing, or using services.
 * 
 * @author Arjen Poutsma
 */
public final class ServiceUtils {
    private ServiceUtils() {

    }

    /**
     * Generates a suitable service name from a given class. The returned name
     * is the simple name of the class, i.e. without the package name.
     * 
     * @param clazz the class.
     * @return the name.
     */
    public static String makeServiceNameFromClassName(Class clazz) {
        String name = clazz.getName();
        int last = name.lastIndexOf(".");
        if (last != -1) {
            name = name.substring(last + 1);
        }

        int inner = name.lastIndexOf("$");
        if (inner != -1) {
            name = name.substring(inner + 1);
        }

        return name;
    }

    public static QName makeQualifiedNameFromClass(Class clazz) {
        String namespace = NamespaceHelper.makeNamespaceFromClassName(clazz.getName(), "http");
        String localPart = makeServiceNameFromClassName(clazz);
        return new QName(namespace, localPart);
    }

    public static String getMethodName(Method m) {
        StringBuffer sb = new StringBuffer();
        sb.append(m.getDeclaringClass().getName());
        sb.append('.');
        sb.append(m.getName());
        sb.append('(');
        Class[] params = m.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            Class param = params[i];
            sb.append(param.getName());
            if (i < params.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }
}
