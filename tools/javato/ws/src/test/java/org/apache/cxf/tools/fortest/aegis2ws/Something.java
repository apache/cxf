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
package org.apache.cxf.tools.fortest.aegis2ws;

/**
 * Test data type for Aegis in java2ws
 */
public class Something {
    // the .aegis.xml file sets no special properties on the following.
    private String multiple;
    // the .aegis.xml file sets the following to minOccurs=1.
    private String singular;
    
    /**
     *  @return Returns the multiple.
     */
    public String getMultiple() {
        return multiple;
    }
    /**
     * @param multiple The multiple to set.
     */
    public void setMultiple(String multiple) {
        this.multiple = multiple;
    }
    /**
     * @return Returns the singular.
     */
    public String getSingular() {
        return singular;
    }
    /**
     * @param singular The singular to set.
     */
    public void setSingular(String singular) {
        this.singular = singular;
    }
}
