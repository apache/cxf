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

public class IdlField extends IdlDefnImplBase {
    
    protected static final Logger LOG = LogUtils.getL7dLogger(IdlField.class);
    private IdlType type;

    protected IdlField(IdlScopeBase parent, String name, IdlType idlType) {
        super(parent, name);
        this.type = idlType;
    }
    
    public static IdlField create(IdlScopeBase parent, String name, IdlType type) {
        return new IdlField(parent, name, type);
    }


    IdlType type() {
        return this.type;
    }


    public void write(PrintWriter pw) {
        if (!type.isEmptyDef()) {
            pw.print(indent() + type.fullName(definedIn().scopeName()) + " ");
            pw.print(localName());
            pw.println(";");
        } else {
            LOG.log(Level.WARNING, "Ignoring Field " + localName() + " with Empty Type.");
        }
    }

    public boolean isEmptyDef() {
        return type.isEmptyDef();
    }
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        return type.getCircularScope(startScope, doneDefn);
    }
}
