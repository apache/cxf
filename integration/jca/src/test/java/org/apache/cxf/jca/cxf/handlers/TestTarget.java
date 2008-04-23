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
package org.apache.cxf.jca.cxf.handlers;

import java.lang.reflect.Method;


public class TestTarget implements TestInterface { 
    boolean methodInvoked; 

    Method lastMethod; 

    public void testMethod() { 
        try { 
            methodInvoked = true; 
            lastMethod = getClass().getMethod("testMethod", new Class[0]); 
        } catch (NoSuchMethodException ex) { 
            throw new RuntimeException(ex); 
        } 
    } 

    public String toString() { 
        try { 
            methodInvoked = true; 
            lastMethod = getClass().getMethod("toString", new Class[0]); 
            return "TestTarget"; 
            // don't delegate to super as this
            // calls hashCode which messes up the
            // test 
        } catch (NoSuchMethodException ex) { 
            throw new RuntimeException(ex); 
        } 
    } 


    public int hashCode() { 
        try { 
            methodInvoked = true;  
            lastMethod = getClass().getMethod("hashCode", new Class[0]); 
            return super.hashCode(); 
        } catch (NoSuchMethodException ex) { 
            throw new RuntimeException(ex); 
        } 

    } 

    public boolean equals(Object obj) { 
        try { 
            methodInvoked = true; 
            lastMethod = getClass().getMethod("equals", new Class[] {Object.class}); 
            return super.equals(obj); 
        } catch (NoSuchMethodException ex) { 
            throw new RuntimeException(ex); 
        } 

    } 

} 
