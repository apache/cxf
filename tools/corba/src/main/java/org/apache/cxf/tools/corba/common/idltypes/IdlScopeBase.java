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
import java.util.ListIterator;
import java.util.Stack;
import java.util.Vector;


public abstract class IdlScopeBase extends IdlDefnImplBase {
    private List<Object> defns;
    private Stack<Object> hold;
    private List<Object> park;

    protected IdlScopeBase(IdlScopeBase parent, String name) {
        super(parent, name);
        defns = new Vector<Object>();
        hold = new Stack<Object>();
        park = new Vector<Object>();
    }

    public IdlDefn addToScope(IdlDefn def) {
        String nm = def.localName();
        IdlDefn result = lookup(nm);

        if (result == null) {
            defns.add(def);
            result = def;
        }

        return result;
    }

    
    public IdlDefn holdForScope(IdlDefn def) {
        hold.push(def);
        return def;
    }


    public IdlDefn promoteHeldToScope() {
        IdlDefn result = (IdlDefn)hold.pop();
        defns.add(result);
        return result;
    }


    public IdlDefn parkHeld() {
        IdlDefn result = (IdlDefn)hold.pop();
        park.add(result);
        return result;
    }


    public IdlDefn lookup(String nm) {
        return lookup(nm, false);
    }


    public IdlDefn lookup(String nm, boolean undefined) {
        IdlDefn result = null;
        Iterator it = park.iterator();       

        while (it.hasNext()) {
            IdlDefn nextDef = (IdlDefn)it.next();
            if (nextDef.localName().equals(nm)) {
                result = nextDef;
                break;
            }
        }

        if (result == null) {
            it = hold.iterator();      

            while (it.hasNext()) {
                IdlDefn nextDef = (IdlDefn)it.next();
                if (nextDef.localName().equals(nm)) {
                    result = nextDef;
                    break;
                }
            }
        }

        if (undefined) {
            return result;
        }
        
        it = defns.iterator();

        while (it.hasNext()) {
            IdlDefn nextDef = (IdlDefn)it.next();
            if (nextDef.localName().equals(nm)) {
                result = nextDef;
                break;
            }
        }

        return result;
    }

    public IdlDefn lookup(IdlScopedName name) {
        return lookup(name, false);
    }


    public IdlDefn lookup(IdlScopedName name, boolean undefined) {
        IdlScopeBase scope = this;
        String parents[] = name.parentNames();

        if (parents != null) {
            IdlDefn defn = lookup(parents, undefined);

            if (!(defn instanceof IdlScopeBase)) { 
                //|| defn == null) {
                return null;
            }

            scope = (IdlScopeBase)defn;
        }

        return scope.lookup(name.localName(), undefined);
    }


    public IdlDefn lookup(String scopedName[]) {
        return lookup(scopedName, false);
    }

    public IdlDefn lookup(String scopedName[], boolean undefined) {
        IdlScopeBase scope = this;

        for (;;) {
            IdlScopeBase parent = scope.definedIn();

            if (parent == null) {
                break;
            }

            scope = parent;
        }

        IdlDefn result = null;

        for (int i = 0; i < scopedName.length; ++i) {
            boolean inParentScope = scopedName.length > 1 && i < scopedName.length - 1;
            result = scope.lookup(scopedName[i], undefined && !inParentScope);

            if (result == null) {
                return null;
            }

            if (i != (scopedName.length - 1)) {
                if (result instanceof IdlScopeBase) {
                    scope = (IdlScopeBase)result;
                } else {
                    return null;
                }
            }
        }

        return result;
    }
    
    public IdlScopeBase getCircularScope(IdlScopeBase startScope, List<Object> doneDefn) {
        if (doneDefn.contains(this)) {
            return (this == startScope) ? this : null;
        }
        doneDefn.add(this);

        Iterator it = definitions().iterator();
        while (it.hasNext()) {
            IdlDefn defn = (IdlDefn)it.next();
            IdlScopeBase circularScope = defn.getCircularScope(startScope, doneDefn);
            if (circularScope != null) {
                return circularScope;
            }
        }

        doneDefn.remove(this);
        return null;
    }


    public void write(PrintWriter pw) {
        Iterator it = defns.iterator();

        while (it.hasNext()) {
            IdlDefn defn = (IdlDefn)it.next();
            defn.write(pw);
        }
    }


    public void writeFwd(PrintWriter pw) {
        ListIterator it = defns.listIterator(defns.size());

        while (it.hasPrevious()) {
            IdlDefn defn = (IdlDefn)it.previous();
            defn.writeFwd(pw);
        }
    }


    public void flush() {
        promoteParkedToScope();
        
        Iterator it = definitions().iterator();
        while (it.hasNext()) {
            IdlDefn defn = (IdlDefn)it.next();
            defn.flush();
        }
    }


    protected Collection definitions() {
        return defns;
    }


    private void promoteParkedToScope() {
        Iterator it = park.iterator();

        while (it.hasNext()) {
            IdlDefn nextDef = (IdlDefn)it.next();
            defns.add(nextDef);
        }

        park.clear();
    }
}

