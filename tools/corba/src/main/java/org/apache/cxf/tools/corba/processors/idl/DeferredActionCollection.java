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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;

/**
 * Class that holds a fully qualified name as the key that represents
 * a type that was forward declared. 
 * Associated with each fully qualified name is a list of actions.
 * Each action represents a task that is deferred until 
 * the type is really declared. 
 */

public final class DeferredActionCollection {    
    Map<String, List<Object>> deferredActions = new HashMap<String, List<Object>>();
    public void add(Scope scope, DeferredAction action) {
        Object obj = deferredActions.get(scope.toString());
        List<Object> list;
        if (obj == null) {
            list = new ArrayList<Object>();
            deferredActions.put(scope.toString(), list);
        } else {
            list = CastUtils.cast((List)obj);
        }
        list.add(action);
    }
    
    public void remove(Scope scope, DeferredAction action) {
        List list = deferredActions.get(scope.toString());
        if (list != null) {
            list.remove(action);
        }
    }

    public void removeScope(Scope scope) {
        deferredActions.remove(scope.toString());
    }
                   
    public int getSize() {
        return deferredActions.size();
    }
    
    public List getActions(Scope scope) {   
        return deferredActions.get(scope.toString());
    }
}
