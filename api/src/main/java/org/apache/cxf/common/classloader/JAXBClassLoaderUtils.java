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
package org.apache.cxf.common.classloader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.xml.namespace.QName;

public final class JAXBClassLoaderUtils {

    private JAXBClassLoaderUtils() {
    }
    
    /**
     * The TypeReference class is a sun specific class that is found in two different
     * locations depending on environment. In IBM JDK the class is not available at all.
     * So we have to load it at runtime.
     * 
     * @param n
     * @param cls
     * @return initiated TypeReference
     */
    public static Object createTypeReference(QName n, Class<?> cls) {
        Class<?> refClass = null;
        try {
            refClass = ClassLoaderUtils.loadClass("com.sun.xml.bind.api.TypeReference", 
                                                  JAXBClassLoaderUtils.class);
        } catch (Throwable ex) {
            try {
                refClass = ClassLoaderUtils.loadClass("com.sun.xml.internal.bind.api.TypeReference",
                                                      JAXBClassLoaderUtils.class);
            } catch (Throwable ex2) {
                //ignore
            }
        }
        if (refClass != null) {
            try {
                return refClass.getConstructor(QName.class, Type.class, new Annotation[0].getClass())
                    .newInstance(n, cls, new Annotation[0]);
            } catch (Throwable e) {
                //ignore
            }
        }
        return null;
    }
}
