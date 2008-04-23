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

package org.apache.cxf.wsdl;


import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.namespace.QName;

/**
 * Implements the <code>ExtensibilityElement</code> interface.
 */
public class TExtensibilityElementImpl
    extends TExtensibilityElement
    implements ExtensibilityElement {

    @XmlTransient()
    QName elementType;

    /**
     * Returns the type of this extensibility element.
     * @return QName the type of this element.
     */
    public QName getElementType() {
        return elementType;
    }
    
    /**
     * Sets the type of this extensibility element.
     * @param type QName the type of this element.
     */
    public void setElementType(QName type) {
        elementType = type;
    }

    /**
     * Get whether or not the semantics of this extension are required.
     * Relates to the wsdl:required attribute.
     * @return Boolean
     */
    public Boolean getRequired() {
        return isSetRequired() ? isRequired() : null;
    }
    public void setRequired(Boolean value) {
        this.required = value;
    }

}
