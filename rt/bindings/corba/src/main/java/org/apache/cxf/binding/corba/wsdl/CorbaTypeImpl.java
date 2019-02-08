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

package org.apache.cxf.binding.corba.wsdl;

import javax.xml.namespace.QName;


public class CorbaTypeImpl {
    protected QName qname;

    /**
     * Gets the value of the qname property.
     *
     * @return
     *     possible object is
     *     {@link QName }
     *
     */
    public QName getQName() {
        return qname;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link QName }
     *
     */
    public void setQName(QName value) {
        this.qname = value;
    }

    public boolean isSetQName() {
        return this.qname != null;
    }

}



