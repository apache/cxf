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


public class IdlArrayBase extends IdlDefnImplBase implements IdlType {
    private IdlType elemType;
    private List<Object> dims;    
    private int size;

    protected IdlArrayBase(IdlScopeBase parent, String name, IdlType elem, int length) {
        super(parent, name);
        this.size = length;
        dims = new Vector<Object>();

        if (elem instanceof IdlAnonArray) {
            IdlAnonArray arr = (IdlAnonArray)elem;
            elemType = arr.elemType();

            Iterator it = arr.dimensions().iterator();

            while (it.hasNext()) {
                dims.add(it.next());
            }
        } else {
            elemType = elem;
        }
        
        dims.add(0, new Integer(size));            
    }

    public void write(PrintWriter pw) {
        Iterator it = dims.iterator();

        while (it.hasNext()) {
            pw.print("[" + it.next() + "]");
        }
    }


    public void write(PrintWriter pw, String name) {
        pw.print(name);

        Iterator it = dims.iterator();

        while (it.hasNext()) {
            pw.print("[" + it.next() + "]");
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


    protected List dimensions() {
        return dims;
    }
}
