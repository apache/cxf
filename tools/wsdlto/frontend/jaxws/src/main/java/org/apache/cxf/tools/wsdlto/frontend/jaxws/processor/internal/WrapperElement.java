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

package org.apache.cxf.tools.wsdlto.frontend.jaxws.processor.internal;

import javax.xml.namespace.QName;

public final class WrapperElement {

    private QName elementName;

    private QName schemaTypeName;

    public WrapperElement() {
    }

    public WrapperElement(QName en, QName tn) {
        this.elementName = en;
        this.schemaTypeName = tn;
    }

    public QName getElementName() {
        return elementName;
    }

    public void setElementName(final QName newElementName) {
        this.elementName = newElementName;
    }

    public QName getSchemaTypeName() {
        return schemaTypeName;
    }

    public void setSchemaTypeName(final QName newSchemaTypeName) {
        this.schemaTypeName = newSchemaTypeName;
    }

    public String toString() {
        return elementName.toString() + " " + schemaTypeName.toString();
    }
}
