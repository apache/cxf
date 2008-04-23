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


import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.jbi.component.ComponentContext;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.management.DeploymentException;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.w3c.dom.Document;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;

/** Manage deployment of service units to the CXF service engine
 * 
 */
public class CXFServiceUnitManager implements ServiceUnitManager {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CXFServiceUnitManager.class);
    
    private ComponentContext ctx; 
    private final Map<String, CXFServiceUnit> serviceUnits 
        = new HashMap<String, CXFServiceUnit>();
    private final Map<ServiceEndpoint, CXFServiceUnit> csuMap 
        = new HashMap<ServiceEndpoint, CXFServiceUnit>();
    
    private final Bus bus;
    private final ComponentClassLoader componentParentLoader; 
    
    public CXFServiceUnitManager(Bus b, ComponentContext c, ComponentClassLoader loader) {
        ctx = c;
        bus = b;
        componentParentLoader = loader;
    }
    
    // Implementation of javax.jbi.component.ServiceUnitManager
    
    public final void shutDown(final String suName) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.SHUTDOWN", LOG) + suName);
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
        if (!serviceUnits.containsKey(suName)) {
            throw new DeploymentException(new Message("UNDEPLOYED.SU", LOG).toString() + suName);
        }
        serviceUnits.remove(suName);
    }
    
    public final String deploy(final String suName, final String suRootPath) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.DEPLOY", LOG) + suName + " path: " + suRootPath);
        
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
        if (serviceUnits.containsKey(suName)) {
            throw new DeploymentException(new Message("DUPLICATED.SU", LOG) + suName);
        }
        
        if (suRootPath == null) {
            throw new DeploymentException(new Message("SU.ROOT.NULL", LOG).toString());
        } 
        if (suRootPath.length() == 0) {
            throw new DeploymentException(new Message("SU.ROOT.EMPTY", LOG).toString());
        }
        
        String msg =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<jbi-task xmlns=\"http://java.sun.com/xml/ns/jbi/management-message\" "  
            +  "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " 
            +  "version=\"1.0\" " 
            +  "xsi:schemaLocation=\"http://java.sun.com/xml/ns/jbi/management-message " 
            +  "./managementMessage.xsd\">"
            + "<jbi-task-result>"
            + "<frmwk-task-result>" 
            + "<frmwk-task-result-details>" 
            + "<task-result-details>" 
            + "<task-id>deploy</task-id>" 
            + "<task-result>SUCCESS</task-result>" 
            + "</task-result-details>" 
            + "<locale>en_US</locale>" 
            + "</frmwk-task-result-details>"
            + "<is-cause-framework>YES</is-cause-framework>"
            + "</frmwk-task-result>"
            + "<component-task-result>"
            + "<component-name>" + ctx.getComponentName() + "</component-name>"
            + "<component-task-result-details>"
            + "<task-result-details>"
            + "<task-id>deploy</task-id>"
            + "<task-result>SUCCESS</task-result>"
            + "</task-result-details>"
            + "</component-task-result-details>"
            + "</component-task-result>"
            + "</jbi-task-result>"
            + "</jbi-task>";
        
        return msg;
    }
    
    public final String undeploy(final String suName, final String suRootPath) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.UNDEPLOY", LOG) + suName + " path: " + suRootPath);
        
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
                
        if (suRootPath == null) {
            throw new DeploymentException(new Message("SU.ROOT.NULL", LOG).toString());
        } 
        if (suRootPath.length() == 0) {
            throw new DeploymentException(new Message("SU.ROOT.EMPTY", LOG).toString());
        }
        
        
        String msg =  "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<jbi-task xmlns=\"http://java.sun.com/xml/ns/jbi/management-message\" "  
            +  "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " 
            +  "version=\"1.0\" " 
            +  "xsi:schemaLocation=\"http://java.sun.com/xml/ns/jbi/management-message " 
            +  "./managementMessage.xsd\">"
            + "<jbi-task-result>"
            + "<frmwk-task-result>" 
            + "<frmwk-task-result-details>" 
            + "<task-result-details>" 
            + "<task-id>undeploy</task-id>" 
            + "<task-result>SUCCESS</task-result>" 
            + "</task-result-details>" 
            + "<locale>en_US</locale>" 
            + "</frmwk-task-result-details>" 
            + "</frmwk-task-result>"
            + "<component-task-result>"
            + "<component-name>" + ctx.getComponentName() + "</component-name>"
            + "<component-task-result-details>"
            + "<task-result-details>"
            + "<task-id>undeploy</task-id>"
            + "<task-result>SUCCESS</task-result>"
            + "</task-result-details>"
            + "</component-task-result-details>"
            + "</component-task-result>"
            + "</jbi-task-result>"
            + "</jbi-task>";
        serviceUnits.remove(suName);
        return msg;
    }
    
    public final void init(final String suName, final String suRootPath) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.INIT", LOG) + suName + " path: " + suRootPath);
     
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
                
        if (suRootPath == null) {
            throw new DeploymentException(new Message("SU.ROOT.NULL", LOG).toString());
        } 
        if (suRootPath.length() == 0) {
            throw new DeploymentException(new Message("SU.ROOT.EMPTY", LOG).toString());
        }
        
        try { 
            Thread.currentThread().setContextClassLoader(componentParentLoader);
            CXFServiceUnit csu = new CXFServiceUnit(bus, suRootPath, componentParentLoader);
            csu.prepare(ctx);
            serviceUnits.put(suName, csu);    
        } catch (Exception ex) { 
            ex.printStackTrace();
            throw new DeploymentException(ex);
        }
    }
    
    public final void start(final String suName) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.START", LOG) + suName);
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
        if (!serviceUnits.containsKey(suName)) {
            throw new DeploymentException(new Message("UNDEPLOYED.SU", LOG) + suName);
        }
        
        CXFServiceUnit csu = serviceUnits.get(suName); 
        assert csu != null;
        csu.start(ctx, this); 
    }
    
    public void putServiceEndpoint(ServiceEndpoint ref, CXFServiceUnit csu) {
        csuMap.put(ref, csu);
    }
    
    public final CXFServiceUnit getServiceUnitForEndpoint(ServiceEndpoint ep) { 
        return csuMap.get(ep);
    } 
    
    public final void stop(final String suName) throws DeploymentException {
        LOG.info(new Message("SU.MANAGER.STOP", LOG) + suName);
        if (suName == null) {
            throw new DeploymentException(new Message("SU.NAME.NULL", LOG).toString());
        } 
        if (suName.length() == 0) {
            throw new DeploymentException(new Message("SU.NAME.EMPTY", LOG).toString());
        }
        if (!serviceUnits.containsKey(suName)) {
            throw new DeploymentException(new Message("UNDEPLOYED.SU", LOG) + suName);
        }
        serviceUnits.get(suName).stop(ctx);
    }
    
    Document getServiceDescription(final ServiceEndpoint serviceEndpoint) { 
        Document ret = null;
        
        if (csuMap.keySet().contains(serviceEndpoint)) { 
            CXFServiceUnit csu = csuMap.get(serviceEndpoint);
            ret = csu.getWsdlAsDocument();
        } 
        return ret;
    } 
}
