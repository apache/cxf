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

package org.apache.cxf.javascript;

import java.lang.reflect.InvocationTargetException;

import org.w3c.dom.NamedNodeMap;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * 
 */
public class JsNamedNodeMap extends ScriptableObject {
    
    private NamedNodeMap wrappedMap;
    
    public JsNamedNodeMap() {
        // just to make Rhino happy.
    }
    
    @Override
    public String getClassName() {
        return "NamedNodeMap";
    }
    
    public static void register(ScriptableObject scope) {
        try {
            ScriptableObject.defineClass(scope, JsNamedNodeMap.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /** * @return Returns the wrappedMap.
     */
    public NamedNodeMap getWrappedMap() {
        return wrappedMap;
    }

    /**
     * @param wrappedMap The wrappedMap to set.
     */
    public void setWrappedMap(NamedNodeMap wrappedMap) {
        this.wrappedMap = wrappedMap;
    }
    
    // Rhino won't let us use a constructor.
    void initialize(NamedNodeMap map) {
        wrappedMap = map;
    }
    
    public static JsNamedNodeMap wrapMap(Scriptable scope, NamedNodeMap map) {
        Context cx = Context.enter();
        JsNamedNodeMap newObject = (JsNamedNodeMap)cx.newObject(scope, "NamedNodeMap");
        newObject.initialize(map);
        return newObject;
    }

    // CHECKSTYLE:OFF
    
    public int jsGet_length() {
        return wrappedMap.getLength();
    }
    
    public Object jsFunction_getNamedItem(String name) {
        return JsSimpleDomNode.wrapNode(getParentScope(), wrappedMap.getNamedItem(name));
    }
    
    public Object jsFunction_getNamedItemNS(String uri, String local) {
        return JsSimpleDomNode.wrapNode(getParentScope(), wrappedMap.getNamedItemNS(uri, local));
    }

    public Object jsFunction_item(int index) {
        return JsSimpleDomNode.wrapNode(getParentScope(), wrappedMap.item(index));
    }
    
    // don't implement the 'modify' APIs.
}
