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

import org.apache.cxf.tools.common.model.JavaModel;
import org.apache.cxf.tools.util.PropertyUtil;
import org.apache.cxf.tools.util.URIParserUtil;

public class ToolContext {

    protected JavaModel javaModel;
    private Map<String, Object> paramMap;
    private String packageName;
    private Map<String, String> namespacePackageMap = new HashMap<String, String>();
    private Map<String, String> excludeNamespacePackageMap = new HashMap<String, String>();
    private List<InputSource> jaxbBindingFiles = new ArrayList<InputSource>();
    private List<String> excludePkgList = new ArrayList<String>();
    private List<String> excludeFileList = new ArrayList<String>();

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
        for (String key : map.keySet()) {
            if (!optionSet(key)) {
                put(key, map.get(key));
            }
        }
    }

    public void setParameters(Map<String, Object> map) {
        this.paramMap = map;
    }

    public boolean containsKey(String key) {
        return (paramMap == null) ? false : paramMap.containsKey(key);
    }

    public Object get(String key) {
        return (paramMap == null) ? null : paramMap.get(key);
    }
    
    public Object get(String key, Object defaultValue) {
        if (!optionSet(key)) {
            return defaultValue;
        } else {
            return get(key);
        }
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
        return Boolean.valueOf((String)get(key, defaultValue)).booleanValue();
    }

    public void put(String key, Object value) {
        if (paramMap == null) {
            paramMap = new HashMap<String, Object>();
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
        if (get(ToolConstants.CFG_VERBOSE) == null) {
            return false;
        } else {
            return get(ToolConstants.CFG_VERBOSE) == ToolConstants.CFG_VERBOSE;
        }
    }

    // REVIST: Prefer using optionSet, to keep the context clean
    public boolean validateWSDL() {
        return get(ToolConstants.CFG_VALIDATE_WSDL) != null;

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
        } else {
            if (getPackageName() != null) {
                return getPackageName();
            }
            return URIParserUtil.parsePackageName(ns, null);
            
        }
    }

    public String getCustomizedNS(String ns) {
        return URIParserUtil.getNamespace(mapPackageName(ns));
    }

    public void setJaxbBindingFiles(List<InputSource> bindings) {
        jaxbBindingFiles = bindings;
    }

    public List<InputSource> getJaxbBindingFile() {
        return this.jaxbBindingFiles;
    }

    public boolean isExcludeNamespaceEnabled() {
        return excludeNamespacePackageMap.size() > 0;
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
            int pos = pns.indexOf("=");
            String localname = pns;
            if (pos != -1) {
                String ns = pns.substring(0, pos);
                localname = pns.substring(pos + 1);
                return new QName(ns, localname);
            } else {
                return new QName(defaultNamespace, localname);
            }
        }
        return null;
    }
    
    
    public Map<String, String> getNamespacePackageMap() {
        return namespacePackageMap;
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
        newCopy.paramMap = new HashMap<String, Object>(paramMap);
        newCopy.packageName = packageName;
        newCopy.namespacePackageMap = new HashMap<String, String>(namespacePackageMap);
        newCopy.excludeNamespacePackageMap = new HashMap<String, String>(excludeNamespacePackageMap);
        newCopy.jaxbBindingFiles = new ArrayList<InputSource>(jaxbBindingFiles);
        newCopy.excludePkgList = new ArrayList<String>(excludePkgList);
        newCopy.excludeFileList = new ArrayList<String>(excludeFileList);
        
        return newCopy;
    }    
}
