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

package org.apache.cxf.xmlbeans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.cxf.databinding.AbstractWrapperHelper;
import org.apache.xmlbeans.XmlOptions;


public class XmlBeansWrapperHelper extends AbstractWrapperHelper {
    
    private boolean validate;
            
    public XmlBeansWrapperHelper(Class<?> wt, Method[] sets, Method[] gets, Field[] f) {
        super(wt, sets, gets, f);
       
    }
    
    public void setValidate(boolean v) {
        validate = v;
    }
    
    public boolean getValidate() {
        return validate;
    }

    @Override
    protected Object createWrapperObject(Class typeClass) throws Exception {
        Class<?> cls[] = typeClass.getDeclaredClasses();
        Method newType = null;
        for (Method method : typeClass.getMethods()) {
            if (method.getName().startsWith("addNew")) {
                newType = method;
                break;
            }
        }                     
        Object obj = null;
        for (Class<?> c : cls) {                        
            if ("Factory".equals(c.getSimpleName())) {
                if (validate) {
                    // set the validation option here
                    Method method = c.getMethod("newInstance", XmlOptions.class); 
                    XmlOptions options = new XmlOptions();                    
                    options.setValidateOnSet();                    
                    obj = method.invoke(null, options);
                } else {
                    Method method = c.getMethod("newInstance", NO_CLASSES);
                    obj = method.invoke(null, NO_PARAMS);                    
                }
                // create the value object
                obj = newType.invoke(obj, NO_PARAMS);
                break;
            }
        }
        
        return obj;
    }

    @Override
    protected Object getWrapperObject(Object object) throws Exception {                            
        Class<?> valueClass = getXMLBeansValueType(wrapperType);
        // we need get the real Object first
        Method method = wrapperType.getMethod("get" + valueClass.getSimpleName(), NO_CLASSES);
        return method.invoke(object, NO_PARAMS);
        
    }
    
    public static Class<?> getXMLBeansValueType(Class<?> wrapperType)  {
        Class<?> result = wrapperType;
        for (Method method : wrapperType.getMethods()) {
            if (method.getName().startsWith("addNew")) {                
                result = method.getReturnType();
                break;
            }
        }
        return result;
    }

}
