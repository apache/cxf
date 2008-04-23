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
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
//import org.apache.cxf.jbi.se.state.ServiceEngineStateMachine.SEOperation;

public class ServiceEngineStop extends AbstractServiceEngineStateMachine {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceEngineStop.class);
    
    public void changeState(SEOperation operation, ComponentContext context) throws JBIException {
        LOG.info("in stop state");
        if (operation == SEOperation.start) {
            startSE();
            ServiceEngineStateFactory.getInstance().setCurrentState(
                ServiceEngineStateFactory.getInstance().getStartState());
        } else if (operation == SEOperation.shutdown) {
            ServiceEngineStateFactory.getInstance().setCurrentState(
                ServiceEngineStateFactory.getInstance().getShutdownState());
        } else if (operation == SEOperation.init) {
            throw new JBIException("This operation is unsupported, cannot init a stopped JBI component");
        } else if (operation == SEOperation.stop) {
            throw new JBIException("Cannot stop a JBI component which is already stopped");
        }
    }

    private void startSE() throws JBIException {
        try {
            if (ctx == null) {
                return;
            }
            DeliveryChannel chnl = ctx.getDeliveryChannel();
            configureJBITransportFactory(chnl, suManager); 
            LOG.info(new Message("SE.STARTUP.COMPLETE", LOG).toString());
            
        } catch (Throwable e) {
            throw new JBIException(e);
        }
    }

}
