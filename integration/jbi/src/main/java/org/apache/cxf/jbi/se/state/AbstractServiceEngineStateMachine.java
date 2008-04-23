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



package org.apache.cxf.jbi.se.state;

import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jbi.se.CXFServiceEngine;
import org.apache.cxf.jbi.se.CXFServiceUnitManager;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;



public abstract class AbstractServiceEngineStateMachine implements ServiceEngineStateMachine {

    static final String CXF_CONFIG_FILE = "cxf.xml";
    static final String PROVIDER_PROP = "javax.xml.ws.spi.Provider";
    static JBITransportFactory jbiTransportFactory;
    static CXFServiceUnitManager suManager;
    static ComponentContext ctx;
    static Bus bus;
    private static final Logger LOG = LogUtils.getL7dLogger(AbstractServiceEngineStateMachine.class);
      
    
   

    public void changeState(SEOperation operation, ComponentContext context) throws JBIException {
        
    }

    void configureJBITransportFactory(DeliveryChannel chnl, CXFServiceUnitManager mgr)
        throws BusException, JBIException { 
        getTransportFactory().setDeliveryChannel(chnl);
    }


    
    
    void registerJBITransport(Bus argBus, CXFServiceUnitManager mgr) throws JBIException { 
        try { 
            getTransportFactory().setBus(argBus);
        } catch (Exception ex) {
            LOG.severe(new Message("SE.FAILED.REGISTER.TRANSPORT.FACTORY", 
                                               LOG).toString());
            throw new JBIException(new Message("SE.FAILED.REGISTER.TRANSPORT.FACTORY", 
                                               LOG).toString(), ex);
        }
    }
    
    public static CXFServiceUnitManager getSUManager() {
        return suManager;
    }
    
    /**
     * @return
     * @throws JBIException 
     * @throws BusException 
     */
    protected JBITransportFactory getTransportFactory() throws JBIException, BusException {
        assert bus != null;
        if (jbiTransportFactory == null) {
            jbiTransportFactory = (JBITransportFactory)bus.getExtension(ConduitInitiatorManager.class).
                getConduitInitiator(CXFServiceEngine.JBI_TRANSPORT_ID);
            jbiTransportFactory.setBus(bus);
            jbiTransportFactory.setDeliveryChannel(ctx.getDeliveryChannel());
            
        }
        return jbiTransportFactory;
    }


    
    
}
