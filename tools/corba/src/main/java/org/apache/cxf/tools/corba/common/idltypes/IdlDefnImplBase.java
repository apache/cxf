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
import java.util.Vector;


public abstract class IdlDefnImplBase implements IdlDefn {
    private static StringBuffer indent = new StringBuffer();
    private IdlScopeBase parent;
    private IdlScopedName name;

    IdlDefnImplBase(IdlScopeBase parentScope, String scopeName) {
        this.parent = parentScope;
        this.name = new IdlScopedName(parent, scopeName);
    }

    public IdlScopeBase definedIn() {
        return parent;
    }


    public String localName() {
        return name.localName();
    }


    public IdlScopedName name() {
        return name;
    }


    public String fullName() {
        return name.fullName();
    }


    public IdlScopedName scopeName() {
        IdlScopedName result;
        IdlScopeBase scope = definedIn();

        if (scope != null) {
            result = ((IdlDefn)scope).name();
        } else {
            result = new IdlScopedName(null, "");
        }

        return result;
    }


    public String fullName(IdlScopedName relativeTo) {
        return name.fullName(relativeTo);
    }


    public void write(PrintWriter pw, String definitionName) {
        pw.print(definitionName);
    }

    
    public void writeFwd(PrintWriter pw) {
        // COMPLETE;
    }

    
    public boolean isEmptyDef() {
        return false;
    }

    public boolean isCircular() {       
        return getCircularScope(null, new Vector<Object>()) != null;
    }
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        return null;
    }


    public void flush() {
        // COMPLETE
    }


    static String indent() {
        return indent.toString();
    }


    static void indentMore() {
        indent.append("    ");
    }


    static void indentLess() {
        indent.delete(indent.length() - 4, indent.length());
    }

}
