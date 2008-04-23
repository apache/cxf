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
import java.util.Vector;


public final class IdlUnionBranch extends IdlField {
    private boolean isDefault;
    private List<Object> cases;

    private IdlUnionBranch(IdlUnion union, String name, IdlType type, boolean hasDefault) {
        super(union, name, type);
        this.isDefault = hasDefault;
        cases = new Vector<Object>();
    }
    
    public static IdlUnionBranch create(IdlUnion union, String name, IdlType type, boolean isDefault) {
        return new IdlUnionBranch(union, name, type, isDefault);
    }


    public void addCase(String label) {
        if (!isDefault) {
            cases.add(label);
        }
    }


    public void write(PrintWriter pw) {
        if (isDefault) {
            pw.println(indent() + "default:");
        } else {
            Iterator it = cases.iterator();

            while (it.hasNext()) {
                pw.println(indent() + "case " + it.next() + ":");
            }
        }

        indentMore();
        super.write(pw);
        indentLess();
    }

}
