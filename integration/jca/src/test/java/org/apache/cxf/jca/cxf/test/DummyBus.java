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
package org.apache.cxf.jca.cxf.test;



import java.util.Collections;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.service.model.AbstractPropertiesHolder;

public class DummyBus extends AbstractBasicInterceptorProvider implements Bus {    
    // for initialise behaviours
    static int initializeCount;
    static int shutdownCount;
    static boolean correctThreadContextClassLoader;
    static boolean throwException;
    static Bus bus = new DummyBus();
  
   
    static String[] invokeArgs;
    static String cxfHome = "File:/local/temp";
    
    
    public static void reset() {
        initializeCount = 0;
        shutdownCount = 0; 
        correctThreadContextClassLoader = false;
        throwException = false;
    }
    
    
    public static Bus init(String[] args) throws BusException {
        
        initializeCount++;
        correctThreadContextClassLoader = 
            Thread.currentThread().getContextClassLoader() 
            == org.apache.cxf.jca.cxf.ManagedConnectionFactoryImpl.class.getClassLoader();
        if (throwException) {
            throw new BusException(new Message("tested bus exception!", 
                                               (ResourceBundle)null, new Object[]{}));
        }
        return bus;
        
    }

    
    public void shutdown(boolean wait) {
        shutdownCount++; 
        
    }



    //    @Override
    public <T> T getExtension(Class<T> extensionType) {
        return null;
    }

    //    @Override
    public <T> void setExtension(T extension, Class<T> extensionType) {

    }
    
    //    @Override
    public String getId() {
        // TODO Auto-generated method stub
        return null;
    }


    public <T> T getConfiguration(AbstractPropertiesHolder props, T defaultValue, Class<T> type) {
        // TODO Auto-generated method stub
        return null;
    }


    //    @Override
    public void run() {
        // TODO Auto-generated method stub
        
    }

    public static boolean isCorrectThreadContextClassLoader() {
        return correctThreadContextClassLoader;
    }


    public static void setCorrectThreadContextClassLoader(boolean correct) {
        DummyBus.correctThreadContextClassLoader = correct;
    }


    public static int getInitializeCount() {
        return initializeCount;
    }


    public static void setInitializeCount(int count) {
        DummyBus.initializeCount = count;
    }


    public Map<String, Object> getProperties() {
        return Collections.emptyMap();
    }


    public Object getProperty(String s) {
        return null;
    }


    public void setProperty(String s, Object o) {
    }

}
