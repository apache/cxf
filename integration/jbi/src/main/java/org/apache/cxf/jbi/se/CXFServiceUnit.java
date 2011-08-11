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


package org.apache.cxf.jbi.se;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jws.WebService;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.EndpointUtils;
import org.apache.cxf.jbi.ServiceConsumer;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;


/**
 * Wraps a CXF service or client.
 */
public class CXFServiceUnit {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CXFServiceUnit.class);
    
    private final Bus bus; 
    
    private Object serviceImplementation; 
    private ServiceConsumer serviceConsumer; 
    private EndpointImpl endpoint;
    private final String rootPath; 
    private final ClassLoader parentLoader;
    private boolean isProvider;
    private QName serviceName; 
    private String endpointName;
    private ServiceEndpoint ref;
    
    public CXFServiceUnit(Bus b, String path, ComponentClassLoader parent) {
        
        URL url = null; 
        try { 
            url = new File(path + File.separator).toURI().toURL();
            
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, new Message("SU.FAILED.INIT", LOG).toString(), ex);
        } 
        bus = b;
        rootPath = path;
        parent.addResource(url);
        parentLoader = parent;
        parseJbiDescriptor(); 
    }
    
    public boolean isServiceProvider() { 
        return isProvider;
    } 
    
    public void stop(ComponentContext ctx) {
        if (ref != null) {
            try {
                ctx.deactivateEndpoint(ref);
            } catch (JBIException e) {
                LOG.severe(new Message("SU.FAILED.DEACTIVATE.ENDPOINT", LOG).toString() 
                           + ref + e);
            }
        } else {
            serviceConsumer.stop();
        }
    }
    
    public void start(ComponentContext ctx, CXFServiceUnitManager serviceUnitManager) {
        if (isServiceProvider()) { 
            LOG.info(new Message("SU.START.PROVIDER", LOG).toString());
            ref = null;
            try {
                ((JBITransportFactory)bus.getExtension(ConduitInitiatorManager.class).
                        getConduitInitiator(CXFServiceEngine.JBI_TRANSPORT_ID)).
                        setDeliveryChannel(ctx.getDeliveryChannel());
                ref = ctx.activateEndpoint(getServiceName(), getEndpointName());
            } catch (JBIException e) {
                LOG.severe(new Message("SU.FAILED.ACTIVATE.ENDPOINT", LOG).toString() + e);
            } catch (BusException e) {
                LOG.severe(new Message("SU.FAILED.ACTIVATE.ENDPOINT", LOG).toString() + e);
            } 
            LOG.info("activated endpoint: " + ref.getEndpointName() 
                     + " service: " + ref.getServiceName());
            serviceUnitManager.putServiceEndpoint(ref, this);
            
        } else {
            LOG.info(new Message("SU.START.CONSUMER", LOG).toString());
            try {
                ((JBITransportFactory)bus.getExtension(ConduitInitiatorManager.class).
                        getConduitInitiator(CXFServiceEngine.JBI_TRANSPORT_ID)).
                        setDeliveryChannel(ctx.getDeliveryChannel());
            } catch (BusException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (MessagingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            
            new Thread(serviceConsumer).start();
        }
    }
    
    public QName getServiceName() { 
        
        QName ret = null; 
        
        if (isServiceProvider()) { 
            if (serviceName == null) { 
                WebService ws = (WebService)serviceImplementation.getClass().getAnnotation(WebService.class);
                serviceName = new QName(ws.targetNamespace(), ws.serviceName());
            }
            ret = serviceName;
        } else {
            WebService ws;
            WebServiceClassFinder finder = new WebServiceClassFinder(rootPath, parentLoader);
            Collection<Class<?>> classes = null;
            try {
                classes = finder.findWebServiceInterface();
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
            if (classes.size() > 0) {
                Class<?> clz = classes.iterator().next();
                ws = clz.getAnnotation(WebService.class);
                serviceName = new QName(ws.targetNamespace(), ws.serviceName());
                ret = serviceName;
            }
        }
        return ret;
    } 
    
    public String getEndpointName() { 
        return endpointName;
    } 
    
    public void prepare(ComponentContext ctx) throws ClassNotFoundException {

        try { 
            WebServiceClassFinder finder = new WebServiceClassFinder(rootPath, parentLoader);
            Collection<Class<?>> classes = finder.findWebServiceClasses(); 
            if (classes.size() > 0) {
                LOG.info(new Message("SU.PUBLISH.ENDPOINT", LOG).toString());
                isProvider = true;
                Class<?> clz = classes.iterator().next();
                serviceImplementation = clz.newInstance();
                if (EndpointUtils.isValidImplementor(serviceImplementation)) {
                    createProviderConfiguration();
                    
                    endpoint = new EndpointImpl(bus, serviceImplementation, "null");
                    //dummy endpoint to publish on
                    endpoint.publish("http://foo/bar/baz");
                    
                }
                
            } else {
                createConsumerConfiguration();
                classes = finder.findServiceConsumerClasses();
                Class<?> clz = classes.iterator().next();
                serviceConsumer = (ServiceConsumer)clz.newInstance();
                serviceConsumer.setComponentContext(ctx);
                
            }
        } catch (Exception ex) { 
                      
            if (ex.getCause() != null) { 
                ex = (Exception)ex.getCause();
            } 
   
            LOG.log(Level.SEVERE, new Message("SU.FAILED.PUBLISH.ENDPOINT", LOG).toString(), ex);
        } 
    } 
    
    
    public ClassLoader getClassLoader() { 
        return parentLoader;
    } 
    
    
    Document getWsdlAsDocument() { 
        
        Document doc = null;
        try { 
            WebService ws = null;
            WebServiceClassFinder finder = new WebServiceClassFinder(rootPath, parentLoader);
            Collection<Class<?>> classes = finder.findWebServiceInterface(); 
            if (classes.size() > 0) {
                Class<?> clz = classes.iterator().next();
                ws = clz.getAnnotation(WebService.class);
            }
            if (ws != null) {
                InputSource in = new InputSource(ws.wsdlLocation());
                StaxUtils.createXMLStreamReader(in);
                doc = StaxUtils.read(in);
            } else { 
                LOG.severe(new Message("SU.COULDNOT.GET.ANNOTATION", LOG).toString());
            }
        } catch (Exception ex) {
            //ex.printStackTrace();
        } 
        return doc;
    } 
    
    
    
    
    
    private void parseJbiDescriptor() { 
        
        // right now, all we are interested in is an endpoint name
        // from the jbi dd.
        File metaInf = new File(rootPath, "META-INF");
        File jbiXml = new File(metaInf, "jbi.xml");
        try { 
            Document doc = XMLUtils.parse(jbiXml);
            
            Element providesEl = (Element)findNode(doc.getDocumentElement(), "provides");
            Element consumersEl = (Element)findNode(doc.getDocumentElement(), "consumes");
            if (providesEl != null) {
                endpointName = providesEl.getAttribute("endpoint-name");
            } else if (consumersEl != null) {
                endpointName = consumersEl.getAttribute("endpoint-name");
            }
        } catch (Exception ex) { 
            LOG.log(Level.SEVERE, "error parsing " + jbiXml, ex);
        } 
        
    } 
    
    
    private Node findNode(Node root, String name) { 
        
        if (name.equals(root.getNodeName())) {
            return root;
        } 
        
        Node node = root.getFirstChild();
        
        while (node != null) {
            Node found = findNode(node, name);
            if (found != null) {
                return found;
            }
            node = node.getNextSibling();
        }
        return null;
    } 
    
    private void createProviderConfiguration() {
        //revisit later on
    }
    
    private void createConsumerConfiguration() {
        //revisit later on        
    }

}
