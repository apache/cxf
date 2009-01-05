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
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

/**
 * A Rhino wrapper around org.w3c.dom.Node. Not comprehensive, but enough to test CXF JavaScript. 
 */
public class JsSimpleDomNode extends ScriptableObject {
    private Node wrappedNode;
    private boolean childrenWrapped;
    private boolean attributesWrapped;
    private JsSimpleDomNode previousSibling;
    private JsSimpleDomNode nextSibling;
    private List<JsSimpleDomNode> children;
    private JsNamedNodeMap attributes;

    /**
     * Only exists to make Rhino happy. Should never be used.
     */
    public JsSimpleDomNode() {
    }
    
    public static void register(ScriptableObject scope) {
        try {
            ScriptableObject.defineClass(scope, JsSimpleDomNode.class);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public String getClassName() {
        return "Node";
    }
    
    public Node getWrappedNode() {
        return wrappedNode;
    }
    
    //CHECKSTYLE:OFF
    public String jsGet_localName() {
        return wrappedNode.getLocalName();       
    }
    
    public String jsGet_namespaceURI() {
        return wrappedNode.getNamespaceURI();
    }
    
    public Object jsGet_firstChild() {
        establishChildren();
        if (children.size() > 0)
            return children.get(0);
        else 
            return null;
    }

    public Object jsGet_nextSibling() {
        return nextSibling; 
    }

    public Object jsGet_previousSibling() {
        return previousSibling; 
    }
    
    public Object jsGet_parentNode() {
        // risk errors in object equality ...
        return wrapNode(this, wrappedNode.getParentNode());
    }
    
    public int jsGet_nodeType() {
        return wrappedNode.getNodeType();
    }
    
    public String jsGet_nodeValue() {
        return wrappedNode.getNodeValue();
    }
    
    public String jsGet_nodeName() {
        return wrappedNode.getNodeName();
    }
    
    // in a more complete version of this, we'd use a different object type to wrap documents.
    public Object jsGet_documentElement() {
        if(9 /* Document */ != wrappedNode.getNodeType()) {
            return null;
        } else {
            establishChildren();
            return children.get(0); // it is, after all, just a convenience feature.
        }
    }
    
    public Object[] jsGet_childNodes() {
        establishChildren();
        return children.toArray();
    }
    
    public Object jsGet_attributes() {
        establishAttributes();
        return attributes;
    }
    
    public String jsFunction_getAttributeNS(String namespaceURI, String localName) {
        NamedNodeMap attributes = wrappedNode.getAttributes();
        Node attrNode = attributes.getNamedItemNS(namespaceURI, localName);
        if(attrNode == null) {
            return null;
        } else {
            Attr attribute = (Attr) attrNode;
            return attribute.getValue();
        }
    }

    public String jsFunction_getAttribute(String localName) {
        NamedNodeMap attributes = wrappedNode.getAttributes();
        Node attrNode = attributes.getNamedItem(localName);
        if(attrNode == null) {
            return null;
        } else {
            Attr attribute = (Attr) attrNode;
            return attribute.getValue();
        }
    }

    //CHECKSTYLE:ON
    
    public static JsSimpleDomNode wrapNode(Scriptable scope, Node node) {
        if (node == null) {
            return null;
        }
        
        Context cx = Context.enter();
        JsSimpleDomNode newObject = (JsSimpleDomNode)cx.newObject(scope, "Node");
        newObject.initialize(node, null);
        return newObject;
    }
    
    private JsSimpleDomNode newObject(Node node, JsSimpleDomNode prev) {
        Context cx = Context.enter();
        JsSimpleDomNode newObject = (JsSimpleDomNode)cx.newObject(getParentScope(), "Node");
        newObject.initialize(node, prev);
        return newObject;
    }

    private void establishChildren() {
        if (!childrenWrapped) {
            if (wrappedNode.hasChildNodes()) {
                children = new ArrayList<JsSimpleDomNode>();
                Node node = wrappedNode.getFirstChild();
                int x = 0;
                while (node != null) {
                    JsSimpleDomNode prev = null;
                    if (x > 0) {
                        prev = (JsSimpleDomNode)children.get(x - 1); 
                    }
                    children.add(x, newObject(node, prev));
                    if (x > 0) {
                        children.get(x - 1).setNext(children.get(x));
                    }                    
                    node = node.getNextSibling();
                    x++;
                }
            } else {
                children = new ArrayList<JsSimpleDomNode>();
            }
            childrenWrapped = true;
        }
    }
    
    private void establishAttributes() {
        if (!attributesWrapped) {
            NamedNodeMap nodeAttributes = wrappedNode.getAttributes();
            attributes = JsNamedNodeMap.wrapMap(getParentScope(), nodeAttributes);
            attributesWrapped = true;
        }
    }

    //rhino won't let us use a constructor.
    void initialize(Node node, JsSimpleDomNode prev) {
        wrappedNode = node;
        childrenWrapped = false;
        previousSibling = prev;
    }
    
    void setNext(JsSimpleDomNode next)  {
        nextSibling = next;
    }


}
