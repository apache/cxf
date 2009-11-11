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

package org.apache.cxf.tools.common.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JAnnotation {

    private Class type;
    private List<JAnnotationElement> elements = new ArrayList<JAnnotationElement>();
    private Set<String> imports = new HashSet<String>();

    public JAnnotation() {
        // empty
    }

    public JAnnotation(Class t) {
        setType(t);
    }

    public Set<String> getImports() {
        prompt();
        for (JAnnotationElement element : elements) {
            imports.addAll(element.getImports());
        }
        return imports;
    }

    private void prompt() {
        toString();        
    }

    public List<JAnnotationElement> getElements() {
        return elements;
    }

    public void addElement(JAnnotationElement element) {
        if (elements.contains(element)) {
            return;
        }
        JAnnotationElement e = getElementByName(element.getName());
        if (e != null) {
            elements.remove(e);
        }
        elements.add(element);
    }

    private JAnnotationElement getElementByName(String name) {
        if (name != null) {
            for (JAnnotationElement e : elements) {
                if (name.equals(e.getName())) {
                    return e;
                }
            }
        }
        return null;
    }

    public Class getType() {
        return type;
    }

    public void setType(final Class newType) {
        this.type = newType;
        imports.add(type.getName());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@");
        sb.append(type.getSimpleName());
        if (getElements().isEmpty()) {
            return sb.toString();
        }

        sb.append("(");
        for (int i = 0; i < elements.size(); i++) {
            sb.append(elements.get(i));
            if (i < elements.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(")");
        return sb.toString();
    }
}
