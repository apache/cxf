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
import java.util.Iterator;
import java.util.List;


public abstract class IdlStructBase extends IdlScopeBase implements IdlType {
    private String kind;

    protected IdlStructBase(IdlScopeBase parent, String name, String type) {
        super(parent, name);   
        this.kind = new String(type);
    }

    void addField(IdlField f) {
        addToScope(f);
    }


    public void write(PrintWriter pw) {
        pw.println(indent() + kind + " " + localName() + " {");
        indentMore();
        super.write(pw);
        indentLess();
        pw.println(indent() + "};");
    }
       
    public void writeFwd(PrintWriter pw) {
        if (isCircular()) {
            pw.println(indent() + kind + " " + localName() + ";");
        }
    }

    public boolean isEmptyDef() {
        if (isCircular()) {
            return false;
        }
        if (definitions().size() == 0) {
            return true;
        }
        boolean hasNonEmptyMembers = false;
        Iterator it = definitions().iterator();
        while (it.hasNext()) {
            IdlDefn defn = (IdlDefn)it.next();
            if (!defn.isEmptyDef()) {
                hasNonEmptyMembers = true;
                break;
            }
        }
        return !hasNonEmptyMembers;
    }
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        if (startScope == null) {
            startScope = this;
        }
        return super.getCircularScope(startScope, doneDefn);
    }
}
