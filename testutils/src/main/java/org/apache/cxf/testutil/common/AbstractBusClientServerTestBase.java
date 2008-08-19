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

package org.apache.cxf.testutil.common;


import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.junit.After;
import org.junit.AfterClass;

public abstract class AbstractBusClientServerTestBase extends AbstractClientServerTestBase {

    static {
        System.setProperty("javax.xml.ws.spi.Provider", "org.apache.cxf.jaxws.spi.ProviderImpl");
    }
    
    protected static String defaultConfigFileName;
    protected static Bus staticBus; 
    
    protected String configFileName = defaultConfigFileName;
    protected Bus bus; 

    public void createBus(String config) throws Exception {
        configFileName = config;
        createBus();
    }
    
    public void createBus() throws Exception {
        if (configFileName != null) {
            System.setProperty("cxf.config.file", configFileName);
        }
        BusFactory bf = BusFactory.newInstance();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
    }
    
    public static Bus createStaticBus(String config) throws Exception {
        defaultConfigFileName = config;
        return createStaticBus();
    }
    public static Bus createStaticBus() throws Exception {
        if (defaultConfigFileName != null) {
            System.setProperty("cxf.config.file", defaultConfigFileName);
        }
        BusFactory bf = BusFactory.newInstance();
        staticBus = bf.createBus();
        BusFactory.setDefaultBus(staticBus);
        return staticBus;
    }
    
    @After
    public void deleteBus() throws Exception {
        if (null != bus) {
            bus.shutdown(true);
            bus = null;
        }
        if (configFileName != null) {
            System.clearProperty("cxf.config.file");
        }
    } 
    @AfterClass
    public static void deleteStaticBus() throws Exception {
        if (null != staticBus) {
            staticBus.shutdown(true);
            staticBus = null;
        }
        if (defaultConfigFileName != null) {
            System.clearProperty("cxf.config.file");
        }
    } 


    protected Bus getBus() {
        if (bus == null) {
            return staticBus;
        }
        return bus;
    }

    protected void setBus(Bus b) {
        bus = b;
    }
    
    
    protected HttpURLConnection getHttpConnection(String target) throws Exception {
        URL url = new URL(target);        
        
        URLConnection connection = url.openConnection();            
        
        assertTrue(connection instanceof HttpURLConnection);
        return (HttpURLConnection)connection;        
    }

    protected boolean runClient(Runnable clientImpl, long timeOut, TimeUnit timeUnit)
        throws InterruptedException {
        FutureTask<?> client = new FutureTask<Object>(clientImpl, null);
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(1, 1, 10000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>());
        tpe.execute(client);
        tpe.shutdown();
        tpe.awaitTermination(timeOut, timeUnit);
        if (!client.isDone()) {
            return false;
        }
        return true;
    }
}
