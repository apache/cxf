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

package org.apache.cxf.tools.corba.processors.idl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cxf.tools.corba.common.ToolCorbaConstants;

public class ModuleToNSMapper {

    Map<String, String> userMap;
    Map<String, List> exModules;
    boolean defaultMapping = true;

    public ModuleToNSMapper() {
        userMap = new HashMap<String, String>();
        exModules = new HashMap<String, List>();
    }

    public void setDefaultMapping(boolean flag) {
        defaultMapping = flag;
    }

    public boolean isDefaultMapping() {
        return defaultMapping;
    }

    public void setUserMapping(Map<String, String> map) {
        userMap = map;
    }
    
    public Map<String, String> getUserMapping() {
        return userMap;
    }

    public void setExcludedModuleMap(Map<String, List> map) {
        exModules = map;
    }
    
    public Map<String, List> getExcludedModuleMap() {
        return exModules;
    }

    public Iterator<String> getExcludedModules() {
        return exModules.keySet().iterator();
    }

    public List getExcludedImports(String module) {
        return exModules.get(module);
    }

    public boolean containsExcludedModule(String module) {
        return exModules.containsKey(module);
    }
    
    public String map(String scopeStr, String separator) {
        Scope scope = new Scope(scopeStr, separator);
        return map(scope);
    }
    
    public String map(Scope scope) {
        return map(scope, ToolCorbaConstants.MODULE_SEPARATOR);
    }

    public String map(Scope scope, String separator) {
        if (defaultMapping) {
            return null;
        } else {
            String uri = userMap.get(scope.toString(separator));
            if (uri == null) {
                //try the parent scope for mapping
                Scope currentScope = scope;
                String parentURI = null;
                uri = "";
                while (parentURI == null && !currentScope.toString().equals("")
                       && currentScope != currentScope.getParent()) {
                    parentURI = userMap.get(currentScope.toString(separator));
                    if (parentURI == null) {
                        if (!"".equals(uri)) {
                            uri = "/" + uri;
                        }
                        uri = currentScope.tail() + uri;
                    }
                    currentScope = currentScope.getParent();
                }
                if (parentURI != null) {
                    if (!parentURI.endsWith("/")) {
                        parentURI = parentURI + "/";
                    }
                    uri = parentURI + uri;
                } else {
                    uri = "urn:" + uri;
                }
            }
            return uri;
        }
    }
    
    public String mapToQName(Scope scope) {
        if (defaultMapping) {
            return scope.toString();
        } else {
            return scope.tail();
        }
    }

    public String mapNSToPrefix(String nsURI) {
        int pos = nsURI.indexOf(":");
        if (pos != -1) {
            nsURI = nsURI.substring(pos + 1);
        }
        return nsURI.replaceAll("/", "_");      
    }

}
