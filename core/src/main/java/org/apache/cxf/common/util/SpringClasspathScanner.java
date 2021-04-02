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
import java.util.HashSet;
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

    private static final boolean IN_OSGI =  isSpringInOsgi();
    
    
    SpringClasspathScanner() throws Exception {
        Class.forName("org.springframework.core.io.support.PathMatchingResourcePatternResolver");
        Class.forName("org.springframework.core.type.classreading.CachingMetadataReaderFactory");
    }
    private static boolean isSpringInOsgi() {
        try {
            Class.forName("org.springframework.osgi.io.OsgiBundleResourcePatternResolver");
            Class.forName("org.springframework.osgi.util.BundleDelegatingClassLoader");
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    @Override
    protected Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClassesInternal(
        Collection< String > basePackages,
        List<Class< ? extends Annotation > > annotations,
        ClassLoader loader)
        throws IOException, ClassNotFoundException {

        ResourcePatternResolver resolver = getResolver(loader);
        MetadataReaderFactory factory = new CachingMetadataReaderFactory(resolver);

        final Map< Class< ? extends Annotation >, Collection< Class< ? > > > classes =
            new HashMap<>();
        final Map< Class< ? extends Annotation >, Collection< String > > matchingInterfaces =
            new HashMap<>();
        final Map<String, String[]> nonMatchingClasses = new HashMap<>();

        for (Class< ? extends Annotation > annotation: annotations) {
            classes.put(annotation, new HashSet<>());
            matchingInterfaces.put(annotation, new HashSet<>());
        }

        if (basePackages == null || basePackages.isEmpty()) {
            return classes;
        }

        for (final String basePackage: basePackages) {
            final boolean scanAllPackages = basePackage.equals(WILDCARD);
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
                    boolean concreteClass = !metadata.isInterface() && !metadata.isAbstract();
                    if (metadata.isAnnotated(annotation.getName())) {
                        if (concreteClass) {
                            classes.get(annotation).add(loadClass(metadata.getClassName(), loader));
                        } else {
                            matchingInterfaces.get(annotation).add(metadata.getClassName());
                        }
                    } else if (concreteClass && metadata.getInterfaceNames().length > 0) {
                        nonMatchingClasses.put(metadata.getClassName(), metadata.getInterfaceNames());
                    }
                }
            }
        }
        if (!nonMatchingClasses.isEmpty()) {
            for (Map.Entry<Class<? extends Annotation>, Collection<String>> e1 : matchingInterfaces.entrySet()) {
                for (Map.Entry<String, String[]> e2 : nonMatchingClasses.entrySet()) {
                    for (String intName : e2.getValue()) {
                        if (e1.getValue().contains(intName)) {
                            classes.get(e1.getKey()).add(loadClass(e2.getKey(), loader));
                            break;
                        }
                    }
                }
            }
        }

        for (Map.Entry<Class<? extends Annotation>, Collection<String>> e : matchingInterfaces.entrySet()) {
            if (classes.get(e.getKey()).isEmpty()) {
                for (String intName : e.getValue()) {
                    classes.get(e.getKey()).add(loadClass(intName, loader));
                }
            }
        }

        return classes;
    }

    @Override
    protected List<URL> findResourcesInternal(Collection<String> basePackages,
                                              String extension,
                                              ClassLoader loader)
        throws IOException {
        final List<URL> resourceURLs = new ArrayList<>();
        if (basePackages == null || basePackages.isEmpty()) {
            return resourceURLs;
        }
        ResourcePatternResolver resolver = getResolver(loader);

        for (final String basePackage: basePackages) {
            final boolean scanAllPackages = basePackage.equals(WILDCARD);

            String theBasePackage = basePackage;
            if (theBasePackage.startsWith(CLASSPATH_URL_SCHEME)) {
                theBasePackage = theBasePackage.substring(CLASSPATH_URL_SCHEME.length());
            }

            final String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
                + (scanAllPackages ? "" : basePackage.contains(WILDCARD) ? basePackage
                    : ClassUtils.convertClassNameToResourcePath(theBasePackage)) + ALL_FILES + "." + extension;

            final Resource[] resources = resolver.getResources(packageSearchPath);
            for (final Resource resource: resources) {
                resourceURLs.add(resource.getURL());
            }
        }

        return resourceURLs;
    }

    private ResourcePatternResolver getResolver(ClassLoader loader) {
        ResourcePatternResolver resolver = null;
        
        if (IN_OSGI) {
            resolver = SpringOsgiUtil.getResolver(loader);
        }
        if (resolver == null) {
            resolver = loader != null
                ? new PathMatchingResourcePatternResolver(loader) : new PathMatchingResourcePatternResolver();
        }
        return resolver;
    }

    private boolean shouldSkip(final String classname) {
        for (String packageToSkip: PACKAGES_TO_SKIP) {
            if (classname.startsWith(packageToSkip)) {
                return true;
            }
        }

        return false;
    }

    private Class<?> loadClass(String className, ClassLoader loader)
        throws ClassNotFoundException {
        if (loader == null) {
            return ClassLoaderUtils.loadClass(className, getClass());
        }
        return loader.loadClass(className);
    }
}
