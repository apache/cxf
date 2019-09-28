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

package org.apache.cxf.tools.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.xml.sax.InputSource;


import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.PropertyUtil;

public class ToolContext {

    protected JavaModel javaModel;
    private Map<String, Object> paramMap;
    private String packageName;
    private boolean packageNameChanged;
    private ToolErrorListener errors;
    private Map<String, String> namespacePackageMap = new HashMap<>();
    private Map<String, String> excludeNamespacePackageMap = new HashMap<>();
    private List<InputSource> jaxbBindingFiles = new ArrayList<>();
    private List<String> excludePkgList = new ArrayList<>();
    private List<String> excludeFileList = new ArrayList<>();

    public ToolContext() {
    }

    public void loadDefaultNS2Pck(InputStream ins) {
        try {
            PropertyUtil properties = new PropertyUtil();
            properties.load(ins);
            namespacePackageMap.putAll(properties.getMaps());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadDefaultExcludes(InputStream ins) {
        try {
            PropertyUtil properties = new PropertyUtil();
            properties.load(ins);
            namespacePackageMap.putAll(properties.getMaps());
            excludeNamespacePackageMap.putAll(properties.getMaps());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JavaModel getJavaModel() {
        return javaModel;
    }

    public void setJavaModel(JavaModel jModel) {
        this.javaModel = jModel;
    }

    public void addParameters(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!optionSet(entry.getKey())) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void setParameters(Map<String, Object> map) {
        this.paramMap = map;
    }

    public boolean containsKey(String key) {
        return (paramMap != null) && paramMap.containsKey(key);
    }

    public Object get(String key) {
        return (paramMap == null) ? null : paramMap.get(key);
    }
    public String[] getArray(String key) {
        Object o = get(key);
        if (o instanceof String) {
            return new String[] {(String)o};
        }
        return (String[])o;
    }

    public Object get(String key, Object defaultValue) {
        if (!optionSet(key)) {
            return defaultValue;
        }
        return get(key);
    }

    /**
     * avoid need to suppress warnings on string->object cases.
     * @param <T>
     * @param key
     * @param clazz
     * @return
     */
    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(get(key));
    }

    public <T> T get(String key, Class<T> clazz, Object defaultValue) {
        return clazz.cast(get(key, defaultValue));
    }

    public <T> T get(Class<T> key) {
        return key.cast(get(key.getName()));
    }

    public <T> void put(Class<T> key, T value) {
        put(key.getName(), value);
    }

    public boolean getBooleanValue(String key, String defaultValue) {
        return Boolean.parseBoolean((String)get(key, defaultValue));
    }

    public void put(String key, Object value) {
        if (paramMap == null) {
            paramMap = new HashMap<>();
        }
        paramMap.put(key, value);
    }

    public void remove(String key) {
        if (paramMap == null) {
            return;
        }
        paramMap.remove(key);
    }

    public boolean optionSet(String key) {
        return (get(key) == null) ? false : true;
    }

    public boolean isVerbose() {
        String verboseProperty = get(ToolConstants.CFG_VERBOSE, String.class);
        if (verboseProperty == null) {
            return false;
        }
        return ToolConstants.CFG_VERBOSE.equals(verboseProperty) || Boolean.parseBoolean(verboseProperty);
    }

    // REVIST: Prefer using optionSet, to keep the context clean
    public boolean fullValidateWSDL() {
        Object s = get(ToolConstants.CFG_VALIDATE_WSDL);
        if (s instanceof String && ((String)s).length() > 0 && ((String)s).charAt(0) == '=') {
            s = ((String)s).substring(1);
        }
        return !(s == null || "none".equals(s) || "false".equals(s) || "basic".equals(s));
    }
    public boolean basicValidateWSDL() {
        Object s = get(ToolConstants.CFG_VALIDATE_WSDL);
        if (s instanceof String && ((String)s).length() > 0 && ((String)s).charAt(0) == '=') {
            s = ((String)s).substring(1);
        }
        return !("none".equals(s) || "false".equals(s));
    }

    public void addNamespacePackageMap(String namespace, String pn) {
        this.namespacePackageMap.put(namespace, pn);
    }

    private String mapNamespaceToPackageName(String ns) {
        return this.namespacePackageMap.get(ns);
    }

    public boolean hasNamespace(String ns) {
        return this.namespacePackageMap.containsKey(ns);
    }

    public void addExcludeNamespacePackageMap(String namespace, String pn) {
        excludeNamespacePackageMap.put(namespace, pn);
        excludePkgList.add(pn);
    }

    public boolean hasExcludeNamespace(String ns) {
        return excludeNamespacePackageMap.containsKey(ns);
    }

    public String getExcludePackageName(String ns) {
        return this.excludeNamespacePackageMap.get(ns);
    }

    public void setPackageName(String pkgName) {
        this.packageName = pkgName;
        packageNameChanged = true;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public String mapPackageName(String ns) {
        if (ns == null) {
            ns = "";
        }
        if (hasNamespace(ns)) {
            return mapNamespaceToPackageName(ns);
        }
        if (getPackageName() != null) {
            return getPackageName();
        }
        return PackageUtils.parsePackageName(ns, null);
    }

    public String getCustomizedNS(String ns) {
        return PackageUtils.getNamespace(mapPackageName(ns));
    }

    public void setJaxbBindingFiles(List<InputSource> bindings) {
        jaxbBindingFiles = bindings;
    }

    public List<InputSource> getJaxbBindingFile() {
        return this.jaxbBindingFiles;
    }

    public boolean isExcludeNamespaceEnabled() {
        return !excludeNamespacePackageMap.isEmpty();
    }

    public List<String> getExcludePkgList() {
        return this.excludePkgList;
    }

    public List<String> getExcludeFileList() {
        return this.excludeFileList;
    }

    public QName getQName(String key) {
        return getQName(key, null);
    }

    public QName getQName(String key, String defaultNamespace) {
        if (optionSet(key)) {
            String pns = (String)get(key);
            int pos = pns.indexOf('=');
            String localname = pns;
            if (pos != -1) {
                String ns = pns.substring(0, pos);
                localname = pns.substring(pos + 1);
                return new QName(ns, localname);
            }
            return new QName(defaultNamespace, localname);
        }
        return null;
    }

    public ToolErrorListener getErrorListener() {
        if (errors == null) {
            errors = new ToolErrorListener();
        }
        return errors;
    }
    public void setErrorListener(ToolErrorListener e) {
        errors = e;
    }

    public Map<String, String> getNamespacePackageMap() {
        return namespacePackageMap;
    }

    public boolean isPackageNameChanged() {
        return packageNameChanged;
    }

    /**
     * This method attempts to do a deep copy of items which may change in this ToolContext.
     * The intent of this is to be able to take a snapshot of the state of the ToolContext
     * after it's initialised so we can run a tool multiple times with the same setup
     * while not having the state preserved between multiple runs. I didn't want
     * to call this clone() as it neither does a deep nor shallow copy. It does a mix
     * based on my best guess at what changes and what doesn't.
     */
    public ToolContext makeCopy() {
        ToolContext newCopy = new ToolContext();

        newCopy.javaModel = javaModel;
        newCopy.paramMap = new HashMap<>(paramMap);
        newCopy.packageName = packageName;
        newCopy.packageNameChanged = packageNameChanged;
        newCopy.namespacePackageMap = new HashMap<>(namespacePackageMap);
        newCopy.excludeNamespacePackageMap = new HashMap<>(excludeNamespacePackageMap);
        newCopy.jaxbBindingFiles = new ArrayList<>(jaxbBindingFiles);
        newCopy.excludePkgList = new ArrayList<>(excludePkgList);
        newCopy.excludeFileList = new ArrayList<>(excludeFileList);
        newCopy.errors = errors;
        return newCopy;
    }
}
