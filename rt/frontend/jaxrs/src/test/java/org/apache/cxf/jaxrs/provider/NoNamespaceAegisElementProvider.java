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

package org.apache.cxf.jaxrs.provider;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.TypeCreationOptions;

/**
 * Class that sets options to disable xml namespaces.
 */
class NoNamespaceAegisElementProvider extends AegisElementProvider<Object> {
    /*
     * This can't use the cache in AbstractAegisProvider. It could have its own cache.
     */

    @Override
    protected AegisContext getAegisContext(Class<?> plainClass, Type genericType) {
        AegisContext context = new AegisContext();
        context.setWriteXsiTypes(writeXsiType);
        context.setReadXsiTypes(readXsiType);
        TypeCreationOptions tco = new TypeCreationOptions();
        tco.setQualifyElements(false);
        Set<java.lang.reflect.Type> rootClasses = new HashSet<java.lang.reflect.Type>();
        rootClasses.add(genericType);
        context.setTypeCreationOptions(tco);
        context.setRootClasses(rootClasses);
        context.initialize();
        return context;
    }
}
