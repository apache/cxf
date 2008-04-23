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


public final class IdlUnion extends IdlScopeBase implements IdlType {
    private IdlType discriminator;

    private IdlUnion(IdlScopeBase parent, String name, IdlType disc) {
        super(parent, name);
        discriminator = disc;
    }
    
    public static IdlUnion create(IdlScopeBase parent, String name, IdlType discriminator) {
        return new IdlUnion(parent, name, discriminator);
    }


    public void addBranch(IdlUnionBranch ub) {
        addToScope(ub);
    }


    public void write(PrintWriter pw) {
        pw.println(indent() + "union " + localName() + " switch(" + discriminator.fullName(scopeName())
                   + ") {");
        indentMore();
        super.write(pw);
        indentLess();
        pw.println(indent() + "};");
    }

    
    public void writeFwd(PrintWriter pw) {
        if (isCircular()) {
            pw.println(indent() + "union " + localName() + ";");
        }
    }    
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        if (startScope == null) {
            startScope = this;
        }
        return super.getCircularScope(startScope, doneDefn);
    }
}
