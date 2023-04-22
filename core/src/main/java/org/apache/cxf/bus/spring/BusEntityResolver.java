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

package org.apache.cxf.bus.spring;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.apache.cxf.common.logging.LogUtils;
import org.springframework.beans.factory.xml.DelegatingEntityResolver;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.util.CollectionUtils;

/**
 *
 */
public class BusEntityResolver extends DelegatingEntityResolver  {

    private static final Logger LOG = LogUtils.getL7dLogger(BusEntityResolver.class);

    private EntityResolver dtdResolver;
    private EntityResolver schemaResolver;
    private Map<String, String> schemaMappings;
    private ClassLoader classLoader;

    public BusEntityResolver(ClassLoader loader, EntityResolver dr, EntityResolver sr) {
        super(dr, sr);
        classLoader = loader;
        dtdResolver = dr;
        schemaResolver = sr;

        try {
            Properties mappings = PropertiesLoaderUtils.loadAllProperties("META-INF/spring.schemas",
                                                                          classLoader);
            schemaMappings = new ConcurrentHashMap<>(mappings.size());
            CollectionUtils.mergePropertiesIntoMap(mappings, schemaMappings);
        } catch (IOException e) {
            //ignore
        }
    }

    @Override
    public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
        InputSource source = super.resolveEntity(publicId, systemId);
        if (null == source && null != systemId) {
            // try the schema and dtd resolver in turn, ignoring the suffix in publicId
            LOG.log(Level.FINE, "Attempting to resolve systemId {0}", systemId);
            source = schemaResolver.resolveEntity(publicId, systemId);
            if (null == source) {
                source = dtdResolver.resolveEntity(publicId, systemId);
            }
        }
        
        if (null == source) {
            return null;
        }
        
        String resourceLocation = schemaMappings.get(systemId);
        if (resourceLocation != null && publicId == null) {
            Resource resource = new ClassPathResource(resourceLocation, classLoader);
            if (resource.exists()) {
                source.setPublicId(systemId);
                source.setSystemId(resource.getURL().toString());
            }
        }
        return source;
    }
}
