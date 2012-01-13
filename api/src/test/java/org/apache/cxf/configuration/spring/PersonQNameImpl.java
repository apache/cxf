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
package org.apache.cxf.configuration.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

public class PersonQNameImpl implements Person {
    private static int loadCount;
    private static List<Person> loaded = new ArrayList<Person>();
    private Collection<QName> ids;
    private String id;
    
    public PersonQNameImpl() {
        ids = new ArrayList<QName>();
        ids.add(new QName("http://cxf.apache.org", "anonymous"));
        loadCount++;
        loaded.add(this);
        id = "Person " + loadCount;
    }
    
    public Collection<QName> getIds() {
        return ids;
    }

    public void setIds(Collection<QName> ids) {
        this.ids = ids;
    }

    public static int getLoadCount() {
        return loadCount;
    }

    public static List<Person> getLoaded() {
        return loaded;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(id);
        buf.append(" [");
        for (QName qn : ids) {
            buf.append(qn);
            buf.append(",");
        }
        buf.setLength(buf.length() - 1);
        buf.append("]");
        return buf.toString();
    }
}
