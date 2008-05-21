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

package org.apache.cxf.tools.wsdlto.databinding.jaxb;

import com.sun.tools.xjc.api.ClassNameAllocator;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.util.ClassCollector;

public class ClassNameAllocatorImpl implements ClassNameAllocator {
    private static final String TYPE_SUFFIX = "_Type";
    private ClassCollector collector;
    private boolean autoResolveConflicts;

    public ClassNameAllocatorImpl(ClassCollector classCollector, boolean autoResolve) {
        collector = classCollector;
        autoResolveConflicts = autoResolve;
    }

    private boolean isNameCollision(String packageName, String className) {
        return collector.containSeiClass(packageName, className);
    }

    public String assignClassName(String packageName, String className) {
        String fullClzName = className;
        if (isNameCollision(packageName, className)) {
            fullClzName = className + TYPE_SUFFIX;
        }

        String fullPckClass = packageName + "." + fullClzName;
        
        if (autoResolveConflicts) {
            String t2 = collector.getTypesFullClassName(packageName, className);
            int cnt = 1;
            while (!StringUtils.isEmpty(t2)) {
                
                cnt++;
                t2 = collector.getTypesFullClassName(packageName, className + cnt);
            }
            if (cnt != 1) {
                className = className + cnt;
                fullClzName = fullClzName + cnt;
                fullPckClass = packageName + "." + fullClzName;
            }
        }
        collector.addTypesClassName(packageName, className, fullPckClass);
        
        return fullClzName;
    }
   
}
