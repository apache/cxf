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
package org.apache.cxf.ws.addressing;


import java.util.HashSet;
import java.util.Set;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;

/**
 * Holder for utility methods relating to contexts, allows to lazily load JAXB compared to ContextUtils.
 */
public final class ContextJAXBUtils {
    private static JAXBContext jaxbContext;
    private static Set<Class<?>> jaxbContextClasses;

   /**
    * Prevents instantiation.
    */
    private ContextJAXBUtils() {
    }

    /**
     * Retrieve a JAXBContext for marshalling and unmarshalling JAXB generated
     * types.
     *
     * @return a JAXBContext
     */
    public static JAXBContext getJAXBContext() throws JAXBException {
        synchronized (ContextJAXBUtils.class) {
            if (jaxbContext == null || jaxbContextClasses == null) {
                Set<Class<?>> tmp = new HashSet<>();
                JAXBContextCache.addPackage(tmp, ContextUtils.WSA_OBJECT_FACTORY.getClass().getPackage().getName(),
                            ContextUtils.WSA_OBJECT_FACTORY.getClass().getClassLoader());
                JAXBContextCache.scanPackages(tmp);
                CachedContextAndSchemas ccs
                    = JAXBContextCache.getCachedContextAndSchemas(tmp, null, null, null, false);
                jaxbContextClasses = ccs.getClasses();
                jaxbContext = ccs.getContext();
            }
        }
        return jaxbContext;
    }

    /**
     * Set the encapsulated JAXBContext (used by unit tests).
     *
     * @param ctx JAXBContext
     */
    public static void setJAXBContext(JAXBContext ctx) {
        synchronized (ContextJAXBUtils.class) {
            jaxbContext = ctx;
            jaxbContextClasses = new HashSet<>();
        }
    }
}
