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


package org.apache.cxf.jbi.se;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.jws.WebService;


import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jbi.ServiceConsumer;

public class WebServiceClassFinder {

    private static final Logger LOG = LogUtils.getL7dLogger(WebServiceClassFinder.class);
    private final String rootPath;
    private final ClassLoader parent;

    public WebServiceClassFinder(String argRootPath, ClassLoader loader) {
        if (argRootPath.endsWith(File.separator)) {
            argRootPath = argRootPath.substring(0, argRootPath.length() - 2);
        }
        rootPath = argRootPath;
        parent = loader;
    }

    public Collection<Class<?>> findServiceConsumerClasses() throws MalformedURLException {
        return find(new Matcher() {
            public boolean accept(Class<?> clz) {
                return ServiceConsumer.class.isAssignableFrom(clz)
                       && (clz.getModifiers() & Modifier.ABSTRACT) == 0;
            }
        });
    }

    public Collection<Class<?>> findWebServiceClasses() throws MalformedURLException {

        return find(new Matcher() {
            public boolean accept(Class<?> clz) {
                return clz.getAnnotation(WebService.class) != null
                       && (clz.getModifiers() & Modifier.ABSTRACT) == 0;
            }
        });
    }
    
    public Collection<Class<?>> findWebServiceInterface() throws MalformedURLException {

        return find(new Matcher() {
            public boolean accept(Class<?> clz) {
                return clz.getAnnotation(WebService.class) != null
                       && (clz.getModifiers() & Modifier.INTERFACE) == Modifier.INTERFACE;
            }
        });
    }

    private Collection<Class<?>> find(Matcher matcher) throws MalformedURLException {
        List<Class<?>> classes = new ArrayList<Class<?>>();

        File root = new File(rootPath);
        URL[] urls = {root.toURI().toURL()};
        URLClassLoader loader = new URLClassLoader(urls, parent);

        find(root, loader, classes, matcher);
        return classes;
    }

    private void find(File dir, ClassLoader loader, Collection<Class<?>> classes, Matcher matcher) {

        File[] files = dir.listFiles();
        for (File f : files) {
            if (f.toString().endsWith(".class")) {
                Class<?> clz = loadClass(loader, f);
                if (matcher.accept(clz)) {
                    classes.add(clz);
                }
            } else if (f.isDirectory()) {
                find(f, loader, classes, matcher);
            }
        }
    }

    private Class<?> loadClass(ClassLoader loader, File classFile) {

        String fileName = classFile.toString();
        String className = fileName.substring(rootPath.length());
        className = className.substring(0, className.length() - ".class".length())
            .replace(File.separatorChar, '.');
        if (className.startsWith(".")) {
            // ServiceMix and OpenESB are little different with rootPath, so here className may be begin
            // with "."
            className = className.substring(1, className.length());
        }
        try {
            return loader.loadClass(className);
        } catch (ClassNotFoundException ex) {
            LOG.severe(new Message("FAILED.LOAD.CLASS", LOG) + className);
        }
        return null;
    }

    interface Matcher {
        boolean accept(Class<?> clz);
    }

    
}
