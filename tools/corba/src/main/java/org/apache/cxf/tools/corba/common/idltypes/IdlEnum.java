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


public final class IdlEnum extends IdlScopeBase implements IdlType {
    
    private IdlEnum(IdlScopeBase parent, String name) {
        super(parent, name);
    }
    
    public static IdlEnum create(IdlScopeBase parent, String name) {
        return new IdlEnum(parent, name);
    }


    public void addEnumerator(IdlEnumerator e) {
        addToScope(e);
    }


    public void write(PrintWriter pw) {
        pw.println(indent() + "enum " + localName() + " {");
        indentMore();

        Collection enums = definitions();
        int needComma = enums.size() - 1;
        Iterator it = enums.iterator();

        while (it.hasNext()) {
            IdlEnumerator en = (IdlEnumerator)it.next();
            pw.print(indent() + CorbaUtils.mangleEnumIdentifier(localName() + "_" + en.localName()));

            if (needComma-- != 0) {
                pw.println(",");
            }
        }

        pw.println();
        indentLess();
        pw.println(indent() + "};");
    }
    
}