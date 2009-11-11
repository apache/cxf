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

package org.apache.cxf.tools.corba.common.idltypes;

import org.apache.cxf.tools.corba.common.ToolCorbaConstants;

public class IdlScopedName {
    private String fullName;
    private String localName;
    private String parentNames[];

    IdlScopedName(IdlScopeBase parent, String name) {
        if (parent != null) {
            fullName = new String(parent.fullName() + ToolCorbaConstants.MODULE_SEPARATOR + name);
            parentNames = parent.name().parentNames();
        } else {
            fullName = new String(name);
            parentNames = null;
        }

        localName = new String(name);
    }

    String localName() {
        return this.localName;
    }


    String fullName() {
        return this.fullName;
    }


    String fullName(IdlScopedName relativeTo) {
        if (relativeTo == null) {
            return fullName();
        }

        StringBuilder nm = new StringBuilder(fullName);
        String rel = relativeTo.fullName() + ToolCorbaConstants.MODULE_SEPARATOR;

        if (fullName.indexOf(rel) == 0) {
            nm.delete(0, rel.length());
        }

        return nm.toString();
    }


    String[] parentNames() {
        return this.parentNames;
    }
}
