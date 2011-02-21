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

package org.apache.cxf.jibx;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.cxf.databinding.AbstractWrapperHelper;

/**
 * JibxWrapperHelper 
 */
public class JibxWrapperHelper extends AbstractWrapperHelper {
    
    protected JibxWrapperHelper(Class<?> wt, Method[] sets, Method[] gets, Field[] f) {
        super(wt, sets, gets, f);
    }
    
    @Override
    protected Object createWrapperObject(Class clazz) throws Exception {
        return clazz.newInstance();
    }

    @Override
    protected Object getWrapperObject(Object obj) throws Exception {
        // Wrapper object instance is indeed the true wrapper in used.
        return obj;
    }
}
