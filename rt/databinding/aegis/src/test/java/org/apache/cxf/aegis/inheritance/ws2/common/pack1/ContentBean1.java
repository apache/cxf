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
package org.apache.cxf.aegis.inheritance.ws2.common.pack1;

/**
 * <br/>
 * 
 * @author xfournet
 */
public class ContentBean1 {
    private String data1;

    public ContentBean1() {
    }

    public ContentBean1(String data1) {
        this.data1 = data1;
    }

    public String getData1() {
        return data1;
    }

    public void setData1(String data1) {
        this.data1 = data1;
    }

    public String toString() {
        return "[" + getClass().getName() + "] data1=" + data1;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final ContentBean1 that = (ContentBean1)o;

        if (data1 != null ? !data1.equals(that.data1) : that.data1 != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        return data1 != null ? data1.hashCode() : 0;
    }
}
