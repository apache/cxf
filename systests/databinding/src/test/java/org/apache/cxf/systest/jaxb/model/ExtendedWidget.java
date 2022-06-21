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
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;


@XmlType(name = "extendedwidget", namespace = "http://cxf.org.apache/model")
@XmlRootElement(name = "extendedwidget", namespace = "http://cxf.org.apache/model")
@XmlAccessorType(XmlAccessType.FIELD)
public class ExtendedWidget extends Widget {

    @XmlElement(required = true, namespace = "http://cxf.org.apache/model")
    private boolean extended = true;

    /**
     *
     */
    public ExtendedWidget() {
        super();
    }

    /**
     * @param id
     * @param name
     * @param serialNumber
     * @param broken
     * @param extended
     */
    public ExtendedWidget(long id, String name, String serialNumber, boolean broken, boolean extended) {
        super(id, name, serialNumber, broken);
        this.extended = extended;
    }

    /**
     * @return the extended
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * @param extended the extended to set
     */
    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        boolean ret = false;
        if (obj instanceof ExtendedWidget) {
            ExtendedWidget w = (ExtendedWidget)obj;
            ret = new EqualsBuilder().appendSuper(true).append(extended, w.extended).isEquals();
        }

        return ret;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result *= Boolean.hashCode(extended);
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this).appendSuper(super.toString()).append("extended", extended)
            .toString();
    }
}
