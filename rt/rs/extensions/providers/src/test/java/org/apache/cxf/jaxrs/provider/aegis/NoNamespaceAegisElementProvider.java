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

package org.apache.cxf.jaxrs.provider.aegis;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.apache.cxf.aegis.AegisContext;
import org.apache.cxf.aegis.type.AbstractTypeCreator;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.TypeCreator;
import org.apache.cxf.aegis.type.TypeMapping;
import org.apache.cxf.aegis.type.XMLTypeCreator;
import org.apache.cxf.aegis.type.java5.Java5TypeCreator;
import org.apache.ws.commons.schema.constants.Constants;

/**
 * Class that sets options to disable xml namespaces.
 */
class NoNamespaceAegisElementProvider<T> extends AegisElementProvider<T> {
    /*
     * This can't use the cache in AbstractAegisProvider. It could have its own cache.
     */

    private TypeCreator createTypeCreator(TypeCreationOptions options) {
        AbstractTypeCreator xmlCreator = createRootTypeCreator(options);

        Java5TypeCreator j5Creator = new NoNamespaceJava5TypeCreator();
        j5Creator.setNextCreator(createDefaultTypeCreator(options));
        j5Creator.setConfiguration(options);
        xmlCreator.setNextCreator(j5Creator);

        return xmlCreator;
    }

    protected AbstractTypeCreator createRootTypeCreator(TypeCreationOptions options) {
        AbstractTypeCreator creator = new XMLTypeCreator();
        creator.setConfiguration(options);
        return creator;
    }

    protected AbstractTypeCreator createDefaultTypeCreator(TypeCreationOptions options) {
        AbstractTypeCreator creator = new NoNamespaceTypeCreator();
        creator.setConfiguration(options);
        return creator;
    }

    @Override
    protected AegisContext getAegisContext(Class<?> plainClass, Type genericType) {
        AegisContext context = new AegisContext();
        context.setWriteXsiTypes(writeXsiType);
        context.setReadXsiTypes(readXsiType);
        TypeCreationOptions tco = new TypeCreationOptions();
        tco.setQualifyElements(false);
        Set<java.lang.reflect.Type> rootClasses = new HashSet<>();
        rootClasses.add(genericType);
        context.setTypeCreationOptions(tco);
        context.setRootClasses(rootClasses);
        TypeMapping baseMapping = DefaultTypeMapping.createSoap11TypeMapping(true, false, false);
        DefaultTypeMapping mapping = new DefaultTypeMapping(Constants.URI_2001_SCHEMA_XSD, baseMapping);
        TypeCreator stockTypeCreator = createTypeCreator(tco);

        mapping.setTypeCreator(stockTypeCreator);
        context.setTypeMapping(mapping);
        context.initialize();
        return context;
    }
}
