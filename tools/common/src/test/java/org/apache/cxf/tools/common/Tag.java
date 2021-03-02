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

package org.apache.cxf.tools.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

public class Tag {
    QName name;
    Map<QName, String> attributes;
    String text;

    List<Tag> tags;
    Tag parent;

    List<String> ignoreAttr;


    public List<String> getIgnoreAttr() {
        if (ignoreAttr == null) {
            ignoreAttr = new ArrayList<>();
        }
        return ignoreAttr;
    }

    public Tag getParent() {
        return this.parent;
    }

    public void setParent(Tag nTag) {
        this.parent = nTag;
    }

    public List<Tag> getTags() {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        return tags;
    }

    public String getText() {
        return text;
    }

    public void setText(String nText) {
        this.text = nText;
    }

    public QName getName() {
        return name;
    }

    public void setName(QName nName) {
        this.name = nName;
    }

    public Map<QName, String> getAttributes() {
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        return attributes;
    }

    private String createIndent(int size) {
        String indent = "    ";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < size; i++) {
            sb.append(indent);
        }
        return sb.toString();
    }

    private String formatAttribute(final Tag tag) {
        StringBuilder sb = new StringBuilder();
        sb.append(tag.getName().getLocalPart());
        sb.append(' ');
        for (Map.Entry<QName, String> attr : tag.getAttributes().entrySet()) {
            sb.append(attr.getKey());
            sb.append("=\"");
            sb.append(attr.getValue());
            sb.append("\" ");
        }
        return sb.toString().trim();
    }

    private void formatTag(Tag tag, int indent, StringBuilder sb) {
        sb.append(createIndent(indent));
        sb.append(indent);
        sb.append('<');
        sb.append(formatAttribute(tag));
        sb.append('>');
        if (tag.getParent() != null) {
            sb.append(" (").append(tag.getParent().getName().getLocalPart()).append(')');
        }
        if (text != null) {
            sb.append(text);
        }
        sb.append('\n');

        if (!tag.getTags().isEmpty()) {
            indent++;
            for (Tag subTag : tag.getTags()) {
                formatTag(subTag, indent, sb);
            }
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        formatTag(this, 0, sb);
        return sb.toString();
    }

    public int hashCode() {
        return getName().hashCode() + getAttributes().hashCode();
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (!(object instanceof Tag)) {
            return false;
        }

        if (object == this) {
            return true;
        }
        Tag tag = (Tag) object;
        if (!getName().equals(tag.getName())) {
            return false;
        }
        for (QName attr : getAttributes().keySet()) {
            if (getIgnoreAttr().contains(attr.getLocalPart())
                || getIgnoreAttr().contains(getName().getLocalPart() + "@" + attr.getLocalPart())) {
                continue;
            }
            if (!tag.getAttributes().containsKey(attr)) {
                return false;
            }
            if (!tag.getAttributes().get(attr).equals(getAttributes().get(attr))) {
                return false;
            }
        }
        return true;
    }
}
