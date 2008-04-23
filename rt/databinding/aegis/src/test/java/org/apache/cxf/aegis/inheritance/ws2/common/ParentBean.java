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
package org.apache.cxf.aegis.inheritance.ws2.common;

import org.apache.cxf.aegis.inheritance.ws2.common.pack1.ContentBean1;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class ParentBean {
    private String id;

    private ContentBean1 content;

    public ParentBean() {
    }

    public ParentBean(String id, ContentBean1 content) {
        this.id = id;
        this.content = content;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ContentBean1 getContent() {
        return content;
    }

    public void setContent(ContentBean1 content) {
        this.content = content;
    }

    public String toString() {
        return "[" + getClass().getName() + "] id=" + id + "; content={" + content + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ParentBean that = (ParentBean)o;

        if (content != null ? !content.equals(that.content) : that.content != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = id != null ? id.hashCode() : 0;
        result = 29 * result + (content != null ? content.hashCode() : 0);
        return result;
    }
}
