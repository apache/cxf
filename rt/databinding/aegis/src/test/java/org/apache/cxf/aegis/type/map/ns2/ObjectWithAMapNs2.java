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

package org.apache.cxf.aegis.type.map.ns2;

import java.util.HashMap;
import java.util.Map;

/**
 * An object containing a property of map value.
 */
public class ObjectWithAMapNs2 {
    private Map<String, Boolean> theMap;
    
    public ObjectWithAMapNs2() {
        theMap = new HashMap<String, Boolean>();
        theMap.put("rainy", Boolean.TRUE);
        theMap.put("sunny", Boolean.FALSE);
    }

    public Map<String, Boolean> getTheMap() {
        return theMap;
    }

    public void setTheMap(Map<String, Boolean> theMap) {
        this.theMap = theMap;
    }

}
