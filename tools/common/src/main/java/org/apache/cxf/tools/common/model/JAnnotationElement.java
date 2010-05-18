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

import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class JAnnotationElement {
    private String name;
    private Object value;
    private boolean isPrimitive;

    private Set<String> imports = new HashSet<String>();

    public JAnnotationElement() {
    }

    public JAnnotationElement(String n, Object v) {
        this(n, v, false);
    }

    public JAnnotationElement(String n, Object v, boolean primitive) {
        this.name = n;
        this.value = v;
        this.isPrimitive = primitive;
    }

    public Set<String> getImports() {
        return imports;
    }

    public String getName() {
        return name;
    }

    public void setName(final String newName) {
        this.name = newName;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(final Object newValue) {
        this.value = newValue;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (name != null) {
            sb.append(name);
        }
        if (value != null) {
            if (name != null) {
                sb.append(" = ");
            }
            if (value instanceof List) {
                List list = (List) value;
                sb.append("{");
                for (int i = 0; i < list.size(); i++) {
                    appendValue(sb, list.get(i));
                    if (i < list.size() - 1) {
                        sb.append(", ");
                    }
                }
                sb.append("}");
            } else if (value instanceof String[]) {
                sb.append("{");
                for (int i = 0; i < Array.getLength(value); i++) {
                    appendValue(sb, Array.get(value, i));
                    if (i < Array.getLength(value) - 1) {
                        sb.append(", ");
                    }
                }                
                sb.append("}");
            } else {
                appendValue(sb, value);
            }
        }
        return sb.toString();
    }

    private void appendValue(final StringBuilder sb, final Object obj) {
        if (obj instanceof String) {
            if (isPrimitive) {
                sb.append(obj);
            } else {
                getStringValue(sb, obj);
            }
        } else if (obj instanceof Class) {
            Class clz = (Class) obj;
            if (containsSameClassName(clz) && !imports.contains(clz.getName())) {
                sb.append(clz.getName());
            } else {
                sb.append(clz.getSimpleName());
                imports.add(clz.getName());
            }
            sb.append(".class");
        } else if (obj instanceof JAnnotation) {
            sb.append(obj);
            imports.addAll(((JAnnotation)obj).getImports());
        } else if (obj instanceof Enum) {
            appendEnumValue(sb, obj);
        } else if (obj instanceof JavaType) {
            JavaType type = (JavaType)obj;
            sb.append(type.getClassName());
            sb.append(".class");
        } else if (isPrimitive) {
            sb.append(obj);
        }
    }

    private void appendEnumValue(final StringBuilder sb, final Object obj) {
        Enum e = (Enum) obj;

        String clzName = e.getClass().getName();
        if (clzName.contains("$")) {
            imports.add(obj.getClass().getName().substring(0, clzName.lastIndexOf("$")));
        } else {
            imports.add(obj.getClass().getName());
        }
        sb.append(clzName.substring(clzName.lastIndexOf(".") + 1).replace("$", "."));
        sb.append(".");
        sb.append(e.name());
    }

    private void getStringValue(final StringBuilder sb, final Object obj) {
        sb.append("\"");
        sb.append(obj);
        sb.append("\"");
    }

    private boolean containsSameClassName(Class clz) {
        return imports.contains(clz.getName());
    }

    public int hashCode() {
        return this.toString().hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof JAnnotationElement)) {
            return false;
        }

        JAnnotationElement element = (JAnnotationElement) obj;
        return element.toString().equals(this.toString());
    }
}
