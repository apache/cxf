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
package org.apache.cxf.systest.http_jetty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.InetAddress;

import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Properties;


import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.endpoint.ServerRegistry;
import org.apache.cxf.helpers.IOUtils;

import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.webapp.WebAppContext;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * This class tests starting up and shutting down the embedded server when there
 * is extra jetty configuration.
 */
public class EngineLifecycleTest extends Assert {
    private static final String PORT1 = TestUtil.getPortNumber(EngineLifecycleTest.class, 1);
    private static final String PORT2 = TestUtil.getPortNumber(EngineLifecycleTest.class, 2);
    private String close;
    private GenericApplicationContext applicationContext;
    
    
        
    private void readBeans(Resource beanResource) {
        XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(applicationContext);
        reader.loadBeanDefinitions(beanResource);
    }
    
    @Before 
    public void setSystemProperties() {
        close = System.getProperty("org.apache.cxf.transports.http_jetty.DontClosePort");        
        System.setProperty("org.apache.cxf.transports.http_jetty.DontClosePort", "false");
        
    }
    
    @After
    public void resetSystemProperties() {
        if (close != null) {
            System.setProperty("org.apache.cxf.transports.http_jetty.DontClosePort", close);
        }
    }
    
    public void setUpBus(boolean includeService) throws Exception {
        applicationContext = new GenericApplicationContext();
        readBeans(new ClassPathResource("/org/apache/cxf/systest/http_jetty/cxf.xml"));
        readBeans(new ClassPathResource("META-INF/cxf/cxf.xml"));
        readBeans(new ClassPathResource("META-INF/cxf/cxf-extension-soap.xml"));
        readBeans(new ClassPathResource("META-INF/cxf/cxf-extension-http.xml"));
        readBeans(new ClassPathResource("META-INF/cxf/cxf-extension-http-jetty.xml"));
        readBeans(new ClassPathResource("jetty-engine.xml", getClass()));
        if (includeService) {
            readBeans(new ClassPathResource("server-lifecycle-beans.xml", getClass()));
        }
        
        // bring in some property values from a Properties file
        PropertyPlaceholderConfigurer cfg = new PropertyPlaceholderConfigurer();
        Properties properties = new Properties();
        properties.setProperty("staticResourceURL", getStaticResourceURL());
        cfg.setProperties(properties);
        // now actually do the replacement
        cfg.postProcessBeanFactory(applicationContext.getBeanFactory());        
        applicationContext.refresh();
    }
    
    private void invokeService() {        
        DummyInterface client = (DummyInterface) applicationContext.getBean("dummy-client");
        assertEquals("We should get out put from this client", "hello world", client.echo("hello world"));
    }

    private void invokeService8801() {        
        DummyInterface client = (DummyInterface) applicationContext.getBean("dummy-client-8801");
        assertEquals("We should get out put from this client", "hello world", client.echo("hello world"));
    }
    
    private HttpURLConnection getHttpConnection(String target) throws Exception {
        URL url = new URL(target);       
        
        URLConnection connection = url.openConnection();            
        
        assertTrue(connection instanceof HttpURLConnection);
        return (HttpURLConnection)connection;        
    }
    
    private void getTestHtml() throws Exception {
        HttpURLConnection httpConnection = 
            getHttpConnection("http://localhost:" + PORT2 + "/test.html");    
        httpConnection.connect();
        InputStream in = httpConnection.getInputStream();        
        assertNotNull(in);
        CachedOutputStream response = new CachedOutputStream();
        IOUtils.copy(in, response);
        in.close();
        response.close();
              
        FileInputStream htmlFile = 
            new FileInputStream("target/test-classes/org/apache/cxf/systest/http_jetty/test.html");    
        CachedOutputStream html = new CachedOutputStream();
        IOUtils.copy(htmlFile, html);
        htmlFile.close();
        html.close();
        
        assertEquals("Can't get the right test html", html.toString(), response.toString());
    }
    
    public String getStaticResourceURL() throws Exception {
        File staticFile = new File(this.getClass().getResource("test.html").toURI());
        staticFile = staticFile.getParentFile();
        staticFile = staticFile.getAbsoluteFile();
        URL furl = staticFile.toURI().toURL();
        return furl.toString();
    }

    public void shutdownService() throws Exception {   
        Bus bus = (Bus)applicationContext.getBean("cxf");
        bus.shutdown(true);
        applicationContext.destroy();
        applicationContext.close();
                
    }
    
    
    @Test
    public void testUpDownWithServlets() throws Exception {        
        setUpBus(true);
       
        Bus bus = (Bus)applicationContext.getBean("cxf");
        ServerRegistry sr = bus.getExtension(ServerRegistry.class);
        ServerImpl si = (ServerImpl) sr.getServers().get(0);
        JettyHTTPDestination jhd = (JettyHTTPDestination) si.getDestination();
        JettyHTTPServerEngine e = (JettyHTTPServerEngine) jhd.getEngine();
        org.eclipse.jetty.server.Server jettyServer = e.getServer();

        Handler[] contexts = jettyServer.getChildHandlersByClass(WebAppContext.class);
        WebAppContext servletContext = null;
        for (Handler h : contexts) {
            WebAppContext wac = (WebAppContext) h;
            if (wac.getContextPath().equals("/jsunit")) {
                servletContext = wac;
                break;
            }
        }
        servletContext.addServlet("org.eclipse.jetty.servlet.DefaultServlet", "/bloop");
        getTestHtml();
        invokeService();        
        shutdownService();
        verifyNoServer(PORT2);
        verifyNoServer(PORT1);
    }
    
        
    private void verifyNoServer(String port) {
        try {
            Socket socket = new Socket(InetAddress.getLocalHost(), Integer.parseInt(port));
            socket.close();
        } catch (UnknownHostException e) {
            fail("Unknown host for local address");
        } catch (IOException e) {            
            return; // this is what we want.
        }
        fail("Server on port " + port + " accepted a connection.");
        
    }

    /**
     * 
     * @throws Exception
     */
    @Test   
    public void testServerUpDownUp() throws Exception {
        
        setUpBus(true);
        
        getTestHtml();
        invokeService();    
        invokeService8801();
        shutdownService();
        verifyNoServer(PORT2);
        verifyNoServer(PORT1);
        
        
        setUpBus(true);
       
        invokeService();            
        invokeService8801();
        getTestHtml();
        shutdownService();
        verifyNoServer(PORT2);
        verifyNoServer(PORT1);
        

    }

}
