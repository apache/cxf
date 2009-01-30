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

package org.apache.cxf.aegis.type.java5;

import javax.xml.datatype.XMLGregorianCalendar;

public class BeanWithGregorianDate {
    private static final long serialVersionUID = 1481418882118674796L;

    private int id;
    private String name;
    private BeanWithGregorianDate parent;
    private XMLGregorianCalendar date;

    public final XMLGregorianCalendar getDate() {
        return date;
    }

    public final void setDate(XMLGregorianCalendar date) {
        this.date = date;
    }

    public final int getId() {
        return id;
    }

    public final void setId(int id) {
        this.id = id;
    }

    public final String getName() {
        return name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final BeanWithGregorianDate getParent() {
        return parent;
    }

    public final void setParent(BeanWithGregorianDate parent) {
        this.parent = parent;
    }

    @Override
    public String toString() {
        return "BeanWithGregorianDate[id:" + id + ",name:" + name + ",date:" + date + "]";
    }
}
