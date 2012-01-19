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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public final class IdlRoot extends IdlScopeBase {
    private Map<Object, Object> primitiveTypes;
    private List<Object> includeList;

    private IdlRoot() {
        super(null, "");
        primitiveTypes = new HashMap<Object, Object>();
        includeList = new ArrayList<Object>();

        for (short i = IdlPrimitive.MINIMUM; i <= IdlPrimitive.MAXIMUM; ++i) {
            IdlPrimitive prim = IdlPrimitive.create(this, i);
            primitiveTypes.put(prim.wsdlName(), prim);
        }

        primitiveTypes.put("string", IdlString.create());
        primitiveTypes.put("wstring", IdlWString.create());
    }
    
    public static IdlRoot create() {
        return new IdlRoot();
    }


    public IdlDefn lookup(String nm) {
        return lookup(nm, false);
    }


    public IdlDefn lookup(String nm, boolean undefined) {
        IdlDefn result = null;

        if (!undefined && primitiveTypes.containsKey(nm)) {
            result = (IdlDefn)primitiveTypes.get(nm);
        } else {
            result = super.lookup(nm, undefined);
        }

        return result;
    }


    public void addInclude(String includefile) {
        if (!includeList.contains(includefile)) {
            includeList.add(includefile);
        }
    }


    public void write(PrintWriter pw) {
        //Write the Include files
        Iterator it = includeList.iterator();

        while (it.hasNext()) {
            String s = (String)it.next();
            pw.println("#include " + s);
        }

        if (includeList.size() > 0) {
            pw.println();
        }

        super.writeFwd(pw);
        super.write(pw);
    }
    
}
