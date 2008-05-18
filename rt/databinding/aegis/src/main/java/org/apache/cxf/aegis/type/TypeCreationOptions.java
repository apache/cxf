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

/**
 * This class contains a set of flags that control Aegis' process of mapping from Java types to XML Schema.
 * These options are respected by the standard Aegis type creation classes. An application that replaces
 * these with custom creators will make its own arrangements.
 * 
 * @see AbstractTypeCreator
 * @see DefaultTypeCreator
 * @see XMLTypeCreator
 * @see Java5TypeCreator
 * 
 * @since 2.1
 */
package org.apache.cxf.aegis.type;

public class TypeCreationOptions {

    private boolean defaultExtensibleElements;
    private boolean defaultExtensibleAttributes;
    private boolean defaultNillable = true;
    private int defaultMinOccurs;
    private boolean qualifyElements = true;
    private boolean qualifyAttributes;

    public TypeCreationOptions() {
        super();
    }

    /**
     * Should all elements permit 'any attribute'?
     * @return
     */
    public boolean isDefaultExtensibleAttributes() {
        return defaultExtensibleAttributes;
    }

    public void setDefaultExtensibleAttributes(boolean defaultExtensibleAttributes) {
        this.defaultExtensibleAttributes = defaultExtensibleAttributes;
    }

    /**
     * Should all complex types include an xsd:any to allow for future expansion?
     * @return
     */
    public boolean isDefaultExtensibleElements() {
        return defaultExtensibleElements;
    }

    public void setDefaultExtensibleElements(boolean defaultExtensibleElements) {
        this.defaultExtensibleElements = defaultExtensibleElements;
    }

    /**
     * Absent any annotations of XML mapping, the value of the minOccurs attribute on elements.
     * @return
     */
    public int getDefaultMinOccurs() {
        return defaultMinOccurs;
    }

    public void setDefaultMinOccurs(int defaultMinOccurs) {
        this.defaultMinOccurs = defaultMinOccurs;
    }
    /**
     * Absent any annotations of XML mapping, the value of the nillable attribute on elements.
     * @return
     */

    public boolean isDefaultNillable() {
        return defaultNillable;
    }

    public void setDefaultNillable(boolean defaultNillable) {
        this.defaultNillable = defaultNillable;
    }

    /** 
     * Whether or not elements are qualified absent any annotations
     * or mapping files. 
     * True by default.
     * @return 
     */
    public boolean isQualifyElements() {
        return qualifyElements;
    }

    /**
     * Turn on of off element qualification.
     * @param qualifyElements 
     */
    public void setQualifyElements(boolean qualifyElements) {
        this.qualifyElements = qualifyElements;
    }

    /**
     * Whether or not attributes are qualified absent any annotations
     * or mapping files.
     * False by default. 
     * @return 
     */
    public boolean isQualifyAttributes() {
        return qualifyAttributes;
    }

    /**
     * Turn on or off attribute qualification. 
     * @param qualifyAttributes 
     */
    public void setQualifyAttributes(boolean qualifyAttributes) {
        this.qualifyAttributes = qualifyAttributes;
    }
}
