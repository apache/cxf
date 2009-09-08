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

import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

import org.apache.cxf.common.classloader.ClassLoaderUtils;

public final class ReflectionUtil {
    
    private static Method springBeanUtilsDescriptorFetcher; 
    private static boolean springChecked;
    
    private ReflectionUtil() {
        // intentionally empty
    }

    public static List<String> getPackagesFromJar(File jarFile) throws IOException {
        List<String> packageNames = new ArrayList<String>();
        if (jarFile.isDirectory()) {
            getPackageNamesFromDir(jarFile, jarFile, packageNames);
        } else {
            JarResource resource = new JarResource();
            for (String item : resource.getJarContents(jarFile)) {
                if (!item.endsWith(".class")) {
                    continue;
                }
                String packageName = getPackageName(item);
                if (!StringUtils.isEmpty(packageName)
                    && !packageNames.contains(packageName)) {
                    packageNames.add(packageName);
                }
            }
        }
        return packageNames;
    }
    
    private static void getPackageNamesFromDir(File base, File dir, List<String> pkgs) {
        boolean foundClass = false;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                getPackageNamesFromDir(base, file, pkgs);
            } else if (!foundClass && file.getName().endsWith(".class")) {
                foundClass = true;
                String pkg = "";
                file = dir;
                while (!file.equals(base)) {
                    if (!"".equals(pkg)) {
                        pkg = "." + pkg;
                    }
                    pkg = file.getName() + pkg;
                    file = file.getParentFile();
                }
                if (!pkgs.contains(pkg)) {
                    pkgs.add(pkg);
                }
            }
        }
    }

    private static String getPackageName(String clzName) {
        if (clzName.indexOf("/") == -1) {
            return null;
        }
        String packageName = clzName.substring(0, clzName.lastIndexOf("/"));
        return packageName.replace("/", ".");
    }
    
    /**
     *  create own array of property descriptors to:
     *  <pre>
     *  - prevent memory leaks by Introspector's cache
     *  - get correct type for generic properties from superclass
     *     that are limited to a specific type in beanClass
     *    see http://bugs.sun.com/view_bug.do?bug_id=6528714
     *   we cannot use BeanUtils.getPropertyDescriptors because of issue SPR-6063
     *   </pre>
     * @param refClass calling class for class loading.
     * @param beanInfo Bean in question
     * @param beanClass class for bean in question
     * @param propertyDescriptors raw descriptors
     * @return 
     */
    public static PropertyDescriptor[] getPropertyDescriptorsAvoidSunBug(Class<?> refClass, 
                                                                  BeanInfo beanInfo,
                                                                  Class<?> beanClass,
                                                                  PropertyDescriptor[] propertyDescriptors) {
        if (!springChecked) {
            try {
                springChecked = true;
                Class<?> cls = ClassLoaderUtils
                    .loadClass("org.springframework.beans.BeanUtils", refClass);
                springBeanUtilsDescriptorFetcher 
                    = cls.getMethod("getPropertyDescriptor", new Class[] {Class.class, String.class});
            } catch (Exception e) {
                //ignore - just assume it's an unsupported/unknown annotation
            }
        }
        
        if (springBeanUtilsDescriptorFetcher != null) {
            PropertyDescriptor[] descriptors = null;
            if (propertyDescriptors != null) {
                descriptors = new PropertyDescriptor[propertyDescriptors.length];
                for (int i = 0; i < propertyDescriptors.length; i++) {
                    PropertyDescriptor propertyDescriptor = propertyDescriptors[i];
                    try {
                        descriptors[i] = 
                            (PropertyDescriptor)
                            springBeanUtilsDescriptorFetcher.invoke(null,
                                                                    beanClass, 
                                                                    propertyDescriptor.getName());
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    } catch (InvocationTargetException e) {
                        throw new RuntimeException(e);
                    } 
                }
            }
            return descriptors;
        } else {
            return beanInfo.getPropertyDescriptors();
        }
    }
}
