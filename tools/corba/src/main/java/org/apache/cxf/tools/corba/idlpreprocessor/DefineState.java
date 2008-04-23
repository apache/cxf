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

package org.apache.cxf.tools.corba.idlpreprocessor;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry for #define preprocessor instructions that allows tracking whether a symbol is defined or not.
 */
public class DefineState {

    Map<String, String> defines = new HashMap<String, String>();

    public DefineState(Map<String, String> initialDefines) {
        if (initialDefines != null) {
            defines.putAll(initialDefines);
        }
    }

    public void define(String symbol, String value) {
        defines.put(symbol, value);
    }

    public boolean isDefined(String symbol) {
        return defines.containsKey(symbol);
    }

    public String getValue(String symbol) {
        return defines.get(symbol);
    }

    public void undefine(String symbol) {
        defines.remove(symbol);
    }
}
