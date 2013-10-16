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

package org.apache.cxf.jaxrs.interceptor;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxrs.lifecycle.ResourceProvider;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.logging.FaultListener;
import org.apache.cxf.logging.NoOpFaultListener;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;

public class JAXRSOutExceptionMapperInterceptor extends AbstractOutDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSOutExceptionMapperInterceptor.class);
    
    public JAXRSOutExceptionMapperInterceptor() {
        super(Phase.SETUP);
    }
    public JAXRSOutExceptionMapperInterceptor(String phase) {
        super(phase);
    }
    @Override
    public void handleFault(Message message) {
        if (Boolean.TRUE == message.get("jaxrs.out.fault")) {
            // Exception comes from JAXRSOutInterceptor or the one which follows it
            return;
        }
        Throwable ex = message.getContent(Exception.class);
        if (ex instanceof Fault) {
            ex = ((Fault)ex).getCause();
        }
        Response r = JAXRSUtils.convertFaultToResponse(ex, message);
        if (r != null) {
            message.removeContent(Exception.class);
            message.getInterceptorChain().setFaultObserver(null);
            message.setContent(List.class, new MessageContentsList(r));
            if (message.getContextualProperty(FaultListener.class.getName()) == null) {
                message.put(FaultListener.class.getName(), new NoOpFaultListener());
            }
            
            PhaseManager pm = message.getExchange().get(Bus.class).getExtension(PhaseManager.class);
            PhaseInterceptorChain chain = new PhaseInterceptorChain(pm.getOutPhases());
            
            boolean jaxrsOutOrAfter = false;
            Iterator<Interceptor<? extends Message>> iterator = message.getInterceptorChain().iterator();
            while (iterator.hasNext()) {
                Interceptor<? extends Message> outInterceptor = iterator.next();
                if (outInterceptor.getClass() == JAXRSOutInterceptor.class) {
                    jaxrsOutOrAfter = true;
                }
                if (jaxrsOutOrAfter) {
                    chain.add(outInterceptor);
                }
            }
            
            message.setInterceptorChain(chain);
            message.getInterceptorChain().doInterceptStartingAt(message, JAXRSOutInterceptor.class.getName());
            return;
        }
        
        
        LOG.fine("Cleanup thread local variables");
        
        Object rootInstance = message.getExchange().remove(JAXRSUtils.ROOT_INSTANCE);
        Object rootProvider = message.getExchange().remove(JAXRSUtils.ROOT_PROVIDER);
        if (rootInstance != null && rootProvider != null) {
            try {
                ((ResourceProvider)rootProvider).releaseInstance(message, rootInstance);
            } catch (Throwable tex) {
                LOG.warning("Exception occurred during releasing the service instance, " + tex.getMessage());
            }
        }
        ServerProviderFactory.getInstance(message).clearThreadLocalProxies();
        ClassResourceInfo cri = (ClassResourceInfo)message.getExchange().get(JAXRSUtils.ROOT_RESOURCE_CLASS);
        if (cri != null) {
            cri.clearThreadLocalProxies();
        }
    }
    @Override
    public void handleMessage(Message message) throws Fault {
    }
}
