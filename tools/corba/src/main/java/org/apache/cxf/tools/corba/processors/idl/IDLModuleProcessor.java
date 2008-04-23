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

import java.util.Map;

import antlr.collections.AST;

import org.apache.cxf.tools.corba.common.ToolCorbaConstants;


public class IDLModuleProcessor extends IDLProcessor {

    ModuleToNSMapper mapper;

    public IDLModuleProcessor() {
        super();
        mapper = new ModuleToNSMapper();
        mapper.setDefaultMapping(false);
    }
    
    public ModuleToNSMapper getMapper() {
        return mapper;
    }

    public void buildModuleNSMap(Map<String, String> moduleNSMap) {
        AST node = getIDLTree();
        Scope rootScope = new Scope();
        buildModuleNSMap(moduleNSMap, rootScope, node);
    }

    public void buildModuleNSMap(Map<String, String> map, Scope parent, AST node) {
        while (node != null) {
            if (node.getType() == IDLTokenTypes.LITERAL_module) {
                AST identifierNode = node.getFirstChild();
                AST definitionNode = identifierNode.getNextSibling();
                Scope moduleScope = new Scope(parent, identifierNode);
                String scope = moduleScope.toString(ToolCorbaConstants.MODULE_SEPARATOR);
                if (!map.containsKey(scope)) {
                    map.put(scope, mapper.map(moduleScope));
                }
                buildModuleNSMap(map, moduleScope, definitionNode);
            }
            node = node.getNextSibling();
        }
    }

}
