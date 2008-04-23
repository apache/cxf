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

@SuppressWarnings("deprecation")
public class XFireBean2 {
    private String elementProperty;
    private String attributeProperty;
    private String ignoredProperty;

    @org.codehaus.xfire.aegis.type.java5.XmlAttribute(name = "attribute")
    public String getAttributeProperty() {
        return attributeProperty;
    }

    public void setAttributeProperty(String attributeProperty) {
        this.attributeProperty = attributeProperty;
    }

    @org.codehaus.xfire.aegis.type.java5.XmlElement(name = "element")
    public String getElementProperty() {
        return elementProperty;
    }

    public void setElementProperty(String elementProperty) {
        this.elementProperty = elementProperty;
    }

    @org.codehaus.xfire.aegis.type.java5.IgnoreProperty
    public String getIgnoredProperty() {
        return ignoredProperty;
    }

    public void setIgnoredProperty(String ignoredProperty) {
        this.ignoredProperty = ignoredProperty;
    }
}