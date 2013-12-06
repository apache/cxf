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
package org.apache.cxf.transport.http.blueprint;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.w3c.dom.Element;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.util.PackageUtils;

public final class HolderUtils {
    
    private HolderUtils() {
        
    }
    
    
    public static Object getJaxbObject(Element parent, Class<?> c, 
                                       JAXBContext jaxbContext, 
                                       Set<Class<?>> jaxbClasses,
                                       ClassLoader cl) {

        try {
            Unmarshaller umr = getContext(c, jaxbContext, jaxbClasses, cl).createUnmarshaller();
            JAXBElement<?> ele = (JAXBElement<?>) umr.unmarshal(parent, c);

            return ele.getValue();
        } catch (JAXBException e) {
            return null;
        }
    }

    public static synchronized JAXBContext getContext(Class<?> cls, 
                                                      JAXBContext jaxbContext, 
                                                      Set<Class<?>> jaxbClasses,
                                                      ClassLoader cl) {
        if (jaxbContext == null || jaxbClasses == null || !jaxbClasses.contains(cls)) {
            try {
                Set<Class<?>> tmp = new HashSet<Class<?>>();
                if (jaxbClasses != null) {
                    tmp.addAll(jaxbClasses);
                }
                JAXBContextCache.addPackage(tmp, PackageUtils.getPackageName(cls), 
                                            cls == null ? cl : cls.getClassLoader());
                if (cls != null) {
                    boolean hasOf = false;
                    for (Class<?> c : tmp) {
                        if (c.getPackage() == cls.getPackage() && "ObjectFactory".equals(c.getSimpleName())) {
                            hasOf = true;
                        }
                    }
                    if (!hasOf) {
                        tmp.add(cls);
                    }
                }
                JAXBContextCache.scanPackages(tmp);
                JAXBContextCache.CachedContextAndSchemas ccs 
                    = JAXBContextCache.getCachedContextAndSchemas(tmp, null, null, null, false);
                jaxbClasses = ccs.getClasses();
                jaxbContext = ccs.getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return jaxbContext;
    }
}
