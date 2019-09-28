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
import java.util.ArrayList;
import java.util.List;


public class IdlArrayBase extends IdlDefnImplBase implements IdlType {
    private IdlType elemType;
    private List<Integer> dims;
    private int size;

    protected IdlArrayBase(IdlScopeBase parent, String name, IdlType elem, int length) {
        super(parent, name);
        this.size = length;
        dims = new ArrayList<>();

        if (elem instanceof IdlAnonArray) {
            IdlAnonArray arr = (IdlAnonArray)elem;
            elemType = arr.elemType();

            for (Integer i : arr.dimensions()) {
                dims.add(i);
            }
        } else {
            elemType = elem;
        }

        dims.add(0, Integer.valueOf(size));
    }

    public void write(PrintWriter pw) {
        for (Integer i : dims) {
            pw.print("[" + i + "]");
        }
    }


    public void write(PrintWriter pw, String name) {
        pw.print(name);

        for (Integer i : dims) {
            pw.print("[" + i + "]");
        }
    }


    public boolean isEmptyDef() {
        return elemType.isEmptyDef();
    }

    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        return elemType.getCircularScope(startScope, doneDefn);
    }


    IdlType elemType() {
        return elemType;
    }


    int size() {
        return size;
    }


    protected List<Integer> dimensions() {
        return dims;
    }
}
