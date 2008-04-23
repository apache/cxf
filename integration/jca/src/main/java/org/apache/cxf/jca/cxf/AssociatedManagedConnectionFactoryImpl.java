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
package org.apache.cxf.jca.cxf;

import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Logger;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionManager;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.ResourceAdapterAssociation;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jca.core.resourceadapter.ResourceAdapterInternalException;

public class AssociatedManagedConnectionFactoryImpl 
    extends ManagedConnectionFactoryImpl implements ResourceAdapterAssociation {

    private static final Logger LOG = LogUtils.getL7dLogger(AssociatedManagedConnectionFactoryImpl.class);
    private ResourceAdapter ra;

    public AssociatedManagedConnectionFactoryImpl() {
        super();
    }

    public AssociatedManagedConnectionFactoryImpl(Properties props) {
        super(props);
    }

    public Object createConnectionFactory(ConnectionManager connMgr) throws ResourceException {
        Object connFactory = super.createConnectionFactory(connMgr);
        registerBus();
        return connFactory;
    }

    public void setResourceAdapter(ResourceAdapter aRA) throws ResourceException {
        LOG.info("Associate Resource Adapter with ManagedConnectionFactory by appserver. ra = " + ra);
        if (!(aRA instanceof ResourceAdapterImpl)) {
            throw new ResourceAdapterInternalException(
                "ResourceAdapter is not correct, it should be instance of ResourceAdapterImpl");
        }
        this.ra = aRA;
        mergeResourceAdapterProps();
    }

    public ResourceAdapter getResourceAdapter() {
        return ra;
    }
    
    /**
     * If outbound-resourceAdapter and the resourceAdapter has same property,
     * the outbound-resourceAdapter property's value would take precedence.
     */
    protected void mergeResourceAdapterProps() {
        Properties raProps = ((ResourceAdapterImpl)ra).getPluginProps();
        Properties props = getPluginProps();
        Enumeration raPropsEnum = raProps.propertyNames();
        while (raPropsEnum.hasMoreElements()) {
            String key = (String)raPropsEnum.nextElement();
            if (!props.containsKey(key)) {
                setProperty(key, raProps.getProperty(key));
            } else {
                LOG.fine("ManagedConnectionFactory's props already contain [" + key + "]. No need to merge");
            }
        }
    }

    protected void registerBus() throws ResourceException {
        if (ra == null) {
            throw new ResourceAdapterInternalException("ResourceAdapter can not be null");
        }
        
        ((ResourceAdapterImpl)ra).registerBus(getBus());
    }

    protected Object getBootstrapContext() {
        return ((ResourceAdapterImpl)ra).getBootstrapContext();
    }
    
    //Explicit override these two methods, 
    //otherwise when deploy rar to weblogic9.1, it would complaint about this.
    public int hashCode() {
        return super.hashCode();
    }
    
    public boolean equals(Object o) {
        return super.equals(o);
    }
}






