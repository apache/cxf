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
package org.apache.cxf.aegis.inheritance.ws1;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class RootBean {
    private String id;
    private BeanA child;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BeanA getChild() {
        return child;
    }

    public void setChild(BeanA child) {
        this.child = child;
    }

    public String toString() {
        return "[" + getClass().getName() + "] id=" + id + "; child={" + child + "}";
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final RootBean rootBean = (RootBean)o;

        if (child != null ? !child.equals(rootBean.child) : rootBean.child != null) {
            return false;
        }
        if (id != null ? !id.equals(rootBean.id) : rootBean.id != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = id != null ? id.hashCode() : 0;
        result = 29 * result + (child != null ? child.hashCode() : 0);
        return result;
    }
}
