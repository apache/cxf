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

package org.apache.cxf.helpers;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Comparator;

/**
 * Sorts methods according to their name, number of parameters, and parameter
 * types.
 */
public class MethodComparator implements Comparator<Method>, Serializable {

    public int compare(Method m1, Method m2) {

        int val = m1.getName().compareTo(m2.getName());
        if (val == 0) {
            val = m1.getParameterTypes().length - m2.getParameterTypes().length;
            if (val == 0) {
                Class[] types1 = m1.getParameterTypes();
                Class[] types2 = m2.getParameterTypes();
                for (int i = 0; i < types1.length; i++) {
                    val = types1[i].getName().compareTo(types2[i].getName());

                    if (val != 0) {
                        break;
                    }
                }
            }
        }
        return val;
    }

}
