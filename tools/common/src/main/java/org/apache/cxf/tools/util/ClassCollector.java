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

package org.apache.cxf.tools.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class ClassCollector {

    private Map<String, String> seiClassNames
        = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private Map<String, String> typesClassNames = new HashMap<>();
    private Map<String, String> exceptionClassNames = new HashMap<>();
    private Map<String, String> serviceClassNames = new HashMap<>();
    private Map<String, String> implClassNames = new HashMap<>();
    private final Map<String, String> clientClassNames = new HashMap<>();
    private final Map<String, String> serverClassNames = new HashMap<>();
    private final Map<String, String> reservedClassNames = new HashMap<>();


    private final Set<String> typesPackages = new HashSet<>();

    public ClassCollector() {

    }
    public void reserveClass(String fullName) {
        String cls = fullName;
        int idx = cls.lastIndexOf('.');
        String pkg = "";
        if (idx != -1) {
            pkg = cls.substring(0, idx);
            cls = cls.substring(idx + 1);
        }
        reservedClassNames.put(key(pkg, cls), fullName);

        addSeiClassName(pkg, cls, fullName);
        addTypesClassName(pkg, cls, fullName);
        addServerClassName(pkg, cls, fullName);
        addImplClassName(pkg, cls, fullName);
        addClientClassName(pkg, cls, fullName);
        addServiceClassName(pkg, cls, fullName);
        addExceptionClassName(pkg, cls, fullName);
    }

    public boolean isReserved(String packagename, String type) {
        return reservedClassNames.containsKey(key(packagename, type));
    }
    public boolean containSeiClass(String packagename, String type) {
        return seiClassNames.containsKey(key(packagename, type));
    }

    public boolean containTypesClass(String packagename, String type) {
        return typesClassNames.containsKey(key(packagename, type));
    }

    public boolean containExceptionClass(String packagename, String type) {
        return exceptionClassNames.containsKey(key(packagename, type));
    }
    public boolean containServiceClass(String packagename, String type) {
        return this.serviceClassNames.containsKey(key(packagename, type));
    }
    public boolean containClientClass(String packagename, String type) {
        return this.clientClassNames.containsKey(key(packagename, type));
    }
    public boolean containServerClass(String packagename, String type) {
        return this.serverClassNames.containsKey(key(packagename, type));
    }
    public boolean containImplClass(String packagename, String type) {
        return this.implClassNames.containsKey(key(packagename, type));
    }

    public void addSeiClassName(String packagename, String type, String fullClassName) {
        seiClassNames.put(key(packagename, type), fullClassName);
    }

    public void addTypesClassName(String packagename, String type, String fullClassName) {
        typesClassNames.put(key(packagename, type), fullClassName);
    }

    public void addServerClassName(String packagename, String type, String fullClassName) {
        serverClassNames.put(key(packagename, type), fullClassName);
    }

    public void addImplClassName(String packagename, String type, String fullClassName) {
        implClassNames.put(key(packagename, type), fullClassName);
    }

    public void addClientClassName(String packagename, String type, String fullClassName) {
        clientClassNames.put(key(packagename, type), fullClassName);
    }

    public void addServiceClassName(String packagename, String type, String fullClassName) {
        serviceClassNames.put(key(packagename, type), fullClassName);
    }

    public void addExceptionClassName(String packagename, String type, String fullClassName) {
        exceptionClassNames.put(key(packagename, type), fullClassName);
    }

    public String getTypesFullClassName(String packagename, String type) {
        return typesClassNames.get(key(packagename, type));
    }

    public boolean containsTypeIgnoreCase(String packagename, String type) {
        String key = key(packagename, type);
        if (typesClassNames.containsKey(key)) {
            //try the common fast case first
            return true;
        }
        for (String s : typesClassNames.keySet()) {
            if (key.equalsIgnoreCase(s)) {
                return true;
            }
        }
        return false;
    }
    private String key(String packagename, String type) {
        return packagename + "#" + type;
    }

    public Set<String> getTypesPackages() {
        return typesPackages;
    }

    public Collection<String> getGeneratedFileInfo() {
        Set<String> generatedFileList = new TreeSet<>();
        generatedFileList.addAll(seiClassNames.values());
        generatedFileList.addAll(typesClassNames.values());
        generatedFileList.addAll(exceptionClassNames.values());
        generatedFileList.addAll(serviceClassNames.values());
        generatedFileList.addAll(implClassNames.values());
        generatedFileList.addAll(clientClassNames.values());
        return generatedFileList;
    }

    public Map<String, String> getSeiClassNames() {
        return seiClassNames;
    }
    public void setSeiClassNames(Map<String, String> seiClassNames) {
        this.seiClassNames = seiClassNames;
    }
    public Map<String, String> getTypesClassNames() {
        return typesClassNames;
    }
    public void setTypesClassNames(Map<String, String> typesClassNames) {
        this.typesClassNames = typesClassNames;
    }
    public Map<String, String> getExceptionClassNames() {
        return exceptionClassNames;
    }
    public void setExceptionClassNames(Map<String, String> exceptionClassNames) {
        this.exceptionClassNames = exceptionClassNames;
    }
    public Map<String, String> getServiceClassNames() {
        return serviceClassNames;
    }
    public void setServiceClassNames(Map<String, String> serviceClassNames) {
        this.serviceClassNames = serviceClassNames;
    }
    public Map<String, String> getImplClassNames() {
        return implClassNames;
    }
    public void setImplClassNames(Map<String, String> implClassNames) {
        this.implClassNames = implClassNames;
    }

}
