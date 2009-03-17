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
package org.apache.cxf.databinding;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;

/**
 *  This wrapper helper will use reflection to handle the wrapped message
 */
public abstract class AbstractWrapperHelper implements WrapperHelper {
    
    public static final Class NO_CLASSES[] = new Class[0];
    public static final Object NO_PARAMS[] = new Object[0];
        
    protected final Class<?> wrapperType;
    protected final Method setMethods[];
    protected final Method getMethods[];    
    protected final Field fields[];   
                 
    protected AbstractWrapperHelper(Class<?> wt,
                  Method sets[],
                  Method gets[],                  
                  Field f[]) {
        setMethods = sets;
        getMethods = gets;
        fields = f;        
        wrapperType = wt;        
    }
    
    public String getSignature() {
        return "" + System.identityHashCode(this);
    }
    
    protected abstract Object createWrapperObject(Class typeClass) throws Exception;
    
    protected abstract Object getWrapperObject(Object object) throws Exception;
    
    protected Object getPartObject(int index, Object object) throws Exception {
        return object;
    }
    
    protected Object getValue(Method method, Object in) throws IllegalAccessException,
    InvocationTargetException {        
        return method.invoke(in);    
    }
    
    public Object createWrapperObject(List<?> lst) 
        throws Fault {
        try {
            Object wrapperObject = createWrapperObject(wrapperType);
            
            for (int x = 0; x < setMethods.length; x++) {
                if (getMethods[x] == null
                    && setMethods[x] == null 
                    && fields[x] == null) {
                    //this part is a header or something
                    //that is not part of the wrapper.
                    continue;
                }
                Object o = lst.get(x);
                o = getPartObject(x, o);                
                if (o instanceof List) {
                    List<Object> col = CastUtils.cast((List)getMethods[x].invoke(wrapperObject));
                    if (col == null) {
                        //broken generated java wrappers
                        if (setMethods[x] != null) {
                            setMethods[x].invoke(wrapperObject, o);
                        } else {
                            fields[x].set(wrapperObject, lst.get(x));
                        }
                    } else {
                        List<Object> olst = CastUtils.cast((List)o);
                        col.addAll(olst);
                    }
                } else if (setMethods[x] != null) {                        
                    setMethods[x].invoke(wrapperObject, o);
                } else if (fields[x] != null) {
                    fields[x].set(wrapperObject, lst.get(x));
                }
            }
            return wrapperObject;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new Fault(ex.getCause());
        }
    }
    
    public List<Object> getWrapperParts(Object o) throws Fault {
        try {
            Object wrapperObject = getWrapperObject(o);                          
            List<Object> ret = new ArrayList<Object>(getMethods.length);
            for (int x = 0; x < getMethods.length; x++) {
                if (getMethods[x] != null) {
                    ret.add(getValue(getMethods[x], wrapperObject));                        
                } else if (fields[x] != null) {
                    ret.add(fields[x].get(wrapperObject));
                } else {
                    //placeholder
                    ret.add(null);
                }
            }
            
            return ret;
        } catch (Exception ex) {
            throw new Fault(ex.getCause());
        }
    }
    
   
}


