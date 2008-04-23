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


public abstract class IdlSequenceBase extends IdlDefnImplBase implements IdlType {
    private IdlType elemType;
    private int bound;

    protected IdlSequenceBase(IdlScopeBase parent, String name, IdlType elem, int boundValue) {
        super(parent, name);
        elemType = elem;
        this.bound = boundValue;
    }

    IdlType elemType() {
        return elemType;
    }


    int bound() {
        return bound;
    }


    public void write(PrintWriter pw) {
        pw.print("sequence<" + elemType.fullName(scopeName()));

        if (bound != 0) {
            pw.print(", " + bound);
        }

        pw.print("> ");
    }

    public boolean isEmptyDef() {
        return elemType.isEmptyDef();
    }

    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        return elemType.getCircularScope(startScope, doneDefn);
    }
}
