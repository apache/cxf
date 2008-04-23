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
package org.apache.cxf.tools.corba.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.xml.sax.InputSource;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.util.PropertyUtil;
import org.apache.cxf.tools.util.URIParserUtil;


public class ProcessorEnvironment {

    private Map<String, Object> paramMap;
    private String packageName;
    private Map<String, String> namespacePackageMap = new HashMap<String, String>();
    private Map<String, String> excludeNamespacePackageMap = new HashMap<String, String>();
    private final Map<String, InputSource> jaxbBindingFiles = new HashMap<String, InputSource>();

    public ProcessorEnvironment() {
    }
    
    public void loadDefaultNS2Pck()  {
        try {
            PropertyUtil properties = new PropertyUtil();
            properties.load(getResourceAsStream("toolspec/toolspecs/namespace2package.cfg"));
            namespacePackageMap.putAll(properties.getMaps());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void loadDefaultExcludes()  {
        try {
            PropertyUtil properties = new PropertyUtil();
            properties.load(getResourceAsStream("toolspec/toolspecs/wsdltojavaexclude.cfg"));
            excludeNamespacePackageMap.putAll(properties.getMaps());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private InputStream getResourceAsStream(String file) throws IOException {
        return ProcessorEnvironment.class.getResourceAsStream(file);
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

    public boolean getBooleanValue(String key, String defaultValue) {
        return Boolean.valueOf((String) get(key, defaultValue)).booleanValue();
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
        return optionSet(ToolConstants.CFG_VERBOSE);
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
        this.excludeNamespacePackageMap.put(namespace, pn);
    }

    public boolean hasExcludeNamespace(String ns) {
        return this.excludeNamespacePackageMap.containsKey(ns);
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
        if (hasNamespace(ns)) {
            return mapNamespaceToPackageName(ns);
        } else {
            return getPackageName();
        }
    }

    public String getCustomizedNS(String ns) {
        return URIParserUtil.getNamespace(mapPackageName(ns));
    }

    public void addJaxbBindingFile(String location, InputSource is) {
        this.jaxbBindingFiles.put(location, is);
    }

    public Map<String, InputSource> getJaxbBindingFile() {
        return this.jaxbBindingFiles;
    }

    public boolean isExcludeNamespaceEnabled() {        
        return excludeNamespacePackageMap.size() > 0;
    }
}
