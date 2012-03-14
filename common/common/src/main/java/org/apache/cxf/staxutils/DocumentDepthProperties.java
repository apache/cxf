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
package org.apache.cxf.staxutils;

public class DocumentDepthProperties {
    
    public static final String TOTAL_ELEMENT_COUNT = "depthTotalElementCountThreshold";
    public static final String INNER_ELEMENT_COUNT = "depthInnerElementCountThreshold";
    public static final String INNER_ELEMENT_LEVEL = "depthInnerElementLevelThreshold";
    
    private int elementCountThreshold = -1;
    private int innerElementLevelThreshold = -1;
    private int innerElementCountThreshold = -1;
    public DocumentDepthProperties() {
        
    }
    public DocumentDepthProperties(int elementCountThreshold,
                                   int innerElementLevelThreshold,
                                   int innerElementCountThreshold) {
        this.elementCountThreshold = elementCountThreshold;
        this.innerElementLevelThreshold = innerElementLevelThreshold;
        this.innerElementCountThreshold = innerElementCountThreshold;    
    }
    
    public boolean isEffective() {
        return elementCountThreshold != -1 || innerElementLevelThreshold != -1
            || innerElementCountThreshold != -1;
    }
    
    public void setElementCountThreshold(int elementCountThreshold) {
        this.elementCountThreshold = elementCountThreshold;
    }
    public int getElementCountThreshold() {
        return elementCountThreshold;
    }
    public void setInnerElementLevelThreshold(int innerElementLevelThreshold) {
        this.innerElementLevelThreshold = innerElementLevelThreshold;
    }
    public int getInnerElementLevelThreshold() {
        return innerElementLevelThreshold;
    }
    public void setInnerElementCountThreshold(int innerElementCountThreshold) {
        this.innerElementCountThreshold = innerElementCountThreshold;
    }
    public int getInnerElementCountThreshold() {
        return innerElementCountThreshold;
    }
}
