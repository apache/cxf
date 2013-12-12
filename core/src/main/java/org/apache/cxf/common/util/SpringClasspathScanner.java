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
package org.apache.cxf.common.util;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

class SpringClasspathScanner extends ClasspathScanner {
    private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    private final MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);

    protected Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClassesInternal(
        Collection< String > basePackages, List<Class< ? extends Annotation > > annotations) 
        throws IOException, ClassNotFoundException {
        
        final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes = 
            new HashMap< Class< ? extends Annotation >, Collection< Class< ? > > >();
        
        for (Class< ? extends Annotation > annotation: annotations) {
            classes.put(annotation, new ArrayList< Class < ? > >());
        }
        
        if (basePackages == null || basePackages.isEmpty()) {
            return classes;
        }
        
        for (final String basePackage: basePackages) {
            final boolean scanAllPackages = basePackage.equals(ALL_PACKAGES);
            
            final String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX 
                + (scanAllPackages ? "" : ClassUtils.convertClassNameToResourcePath(basePackage)) 
                + ALL_CLASS_FILES;
            
            final Resource[] resources = resolver.getResources(packageSearchPath);                        
            for (final Resource resource: resources) {
                final MetadataReader reader = factory.getMetadataReader(resource);
                final AnnotationMetadata metadata = reader.getAnnotationMetadata();
                
                if (scanAllPackages && shouldSkip(metadata.getClassName())) {
                    continue;
                }
                
                for (Class< ? extends Annotation > annotation: annotations) {
                    if (metadata.isAnnotated(annotation.getName())) {                                
                        classes.get(annotation).add(ClassLoaderUtils.loadClass(metadata.getClassName(), getClass()));
                    }
                }
                
            }                        
        }
        
        return classes;
    }
    
    protected List<URL> findResourcesInternal(Collection<String> basePackages, String extension) 
        throws IOException {
        final List<URL> resourceURLs = new ArrayList<URL>();
        if (basePackages == null || basePackages.isEmpty()) {
            return resourceURLs;
        }
        
        for (final String basePackage: basePackages) {
            final boolean scanAllPackages = basePackage.equals(ALL_PACKAGES);
            
            final String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX 
                + (scanAllPackages ? "" : ClassUtils.convertClassNameToResourcePath(basePackage)) 
                + ALL_FILES + "." + extension;
            
            final Resource[] resources = resolver.getResources(packageSearchPath);                        
            for (final Resource resource: resources) {
                resourceURLs.add(resource.getURL());
            }                        
        }
        
        return resourceURLs;
    }
    
    
       
    private boolean shouldSkip(final String classname) {
        for (String packageToSkip: PACKAGES_TO_SKIP) {
            if (classname.startsWith(packageToSkip)) {
                return true;
            }
        }
        
        return false;
    }
}
