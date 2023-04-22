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

import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlType
public class JaxbBean4 {
    private String nillableProperty;

    private String minOccursProperty;

    @XmlElement(nillable = false)
    public String getNillableProperty() {
        return nillableProperty;
    }

    public void setNillableProperty(String nillableProperty) {
        this.nillableProperty = nillableProperty;
    }

    @XmlElement(required = true)
    public String getMinOccursProperty() {
        return minOccursProperty;
    }

    public void setMinOccursProperty(String minOccursProperty) {
        this.minOccursProperty = minOccursProperty;
    }
}
