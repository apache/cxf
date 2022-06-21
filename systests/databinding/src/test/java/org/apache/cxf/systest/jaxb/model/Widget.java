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

package org.apache.cxf.systest.jaxb.model;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@XmlType(name = "widget", namespace = "http://cxf.org.apache/model")
@XmlRootElement(name = "widget", namespace = "http://cxf.org.apache/model")
@XmlAccessorType(XmlAccessType.FIELD)
public abstract class Widget {

    @XmlAttribute(required = true)
    private long id;

    @XmlElement(required = true, namespace = "http://cxf.org.apache/model")
    private String name;

    @XmlElement(required = false, namespace = "http://cxf.org.apache/model")
    private String serialNumber;

    @XmlElement(required = true, namespace = "http://cxf.org.apache/model")
    private boolean broken;

    /**
     *
     */
    public Widget() {
        super();
    }

    /**
     * @param id
     * @param name
     * @param serialNumber
     * @param broken
     */
    public Widget(long id, String name, String serialNumber, boolean broken) {
        super();
        this.id = id;
        this.name = name;
        this.serialNumber = serialNumber;
        this.broken = broken;
    }

    /**
     * @return the id
     */
    public long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the serialNumber
     */
    public String getSerialNumber() {
        return serialNumber;
    }

    /**
     * @param serialNumber the serialNumber to set
     */
    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    /**
     * @return the broken
     */
    public boolean isBroken() {
        return broken;
    }

    /**
     * @param broken the broken to set
     */
    public void setBroken(boolean broken) {
        this.broken = broken;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean ret = false;
        if (obj instanceof Widget) {
            Widget w = (Widget)obj;
            ret = new EqualsBuilder().append(id, w.id).append(name, w.name).append(serialNumber,
                                                                                   w.serialNumber)
                .append(broken, w.broken).isEquals();
        }
        return ret;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result *= Long.hashCode(id);
        if (name != null) {
            result = 31 * result + name.hashCode();
        }
        if (serialNumber != null) {
            result = 31 * result + serialNumber.hashCode();
        }
        result *= Boolean.hashCode(broken);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).append("id", id).append("name", name).append("serialNumber",
                                                                                      serialNumber)
            .append("broken", broken).toString();
    }

}
