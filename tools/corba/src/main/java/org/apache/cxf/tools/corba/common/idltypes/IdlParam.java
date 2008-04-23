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

import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;


public final class IdlParam extends IdlDefnImplBase {
    private static final Logger LOG = LogUtils.getL7dLogger(IdlParam.class);
    private IdlType type;
    private String mode;

    private IdlParam(IdlOperation parent, String name, IdlType typeType, String modeValue) {
        super(parent, name);
        this.type = typeType;
        this.mode = new String(modeValue);        
    }
    
    public static IdlParam create(IdlOperation parent, String name, IdlType type, String mode) {
        name = CorbaUtils.mangleName(name);

        return new IdlParam(parent, name, type, mode);
    }


    public void write(PrintWriter pw) {
        if (!type.isEmptyDef()) {
            IdlScopedName sn = definedIn().scopeName();
            pw.print(indent() + mode + " " + type.fullName(sn) + " " + localName());
        } else {
            LOG.log(Level.WARNING, "Ignoring Param " + localName() + " with Empty Type");
        }
    }
    
    public boolean isEmptyDef() {
        return type.isEmptyDef();
    }
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        return type.getCircularScope(startScope, doneDefn);
    }
    
}
