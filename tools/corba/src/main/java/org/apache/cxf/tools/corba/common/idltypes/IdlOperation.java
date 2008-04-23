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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public final class IdlOperation extends IdlScopeBase {
    private IdlType returnType;
    private List<Object> exceptions;
    private boolean oneway;

    private IdlOperation(IdlScopeBase parent, String name, boolean isOneway) {
        super(parent, name);
        exceptions = new Vector<Object>();
        oneway = isOneway;
    }
    
    public static IdlOperation create(IdlScopeBase parent, String name, boolean isOneway) {
        return new IdlOperation(parent, name, isOneway);
    }


    public void addParameter(IdlParam arg) {
        super.addToScope(arg);
    }


    public void addReturnType(IdlType rt) {
        returnType = rt;
    }


    public void addException(IdlException exc) {
        exceptions.add(exc);
    }


    public void write(PrintWriter pw) {
        IdlScopedName sn = scopeName();
        pw.print(indent());

        if (returnType != null && !returnType.isEmptyDef()) {
            pw.println(returnType.fullName(sn));
        } else {
            if (oneway) {
                pw.print("oneway ");
            }

            pw.println("void");
        }

        pw.print(indent() + localName() + "(");

        Collection defns = definitions();

        if (defns.size() != 0) {
            pw.println();
            indentMore();

            int needComma = defns.size() - 1;
            Iterator it = defns.iterator();

            while (it.hasNext()) {
                IdlParam def = (IdlParam)it.next();
                def.write(pw);

                if (needComma-- != 0 && !def.isEmptyDef()) {
                    pw.println(",");
                }
            }

            pw.println();
            indentLess();
            pw.print(indent());
        }

        if (exceptions.isEmpty()) {
            pw.println(");");
        } else {
            pw.println(") raises(");
            indentMore();

            int needComma = exceptions.size() - 1;
            Iterator it = exceptions.iterator();

            while (it.hasNext()) {
                IdlException exc = (IdlException)it.next();
                pw.print(indent() + exc.fullName(scopeName()));

                if (needComma-- != 0) {
                    pw.println(",");
                }
            }

            pw.println();
            indentLess();
            pw.println(indent() + ");");
        }
    }
    
}
