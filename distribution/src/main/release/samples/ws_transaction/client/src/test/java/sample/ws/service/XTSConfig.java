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

package sample.ws.service;

import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.txbridge.outbound.OutboundBridgeRecoveryManager;
import org.jboss.jbossts.xts.environment.XTSEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;

import jakarta.transaction.UserTransaction;

@Configuration
public class XTSConfig {
    @Bean(name = "xtsService", initMethod = "start", destroyMethod = "stop")
    public XTSService xtsService() {

        XTSEnvironmentBean xtsEnvironmentBean = XTSPropertyManager.getXTSEnvironmentBean();
        //xtsEnvironmentBean.setXtsInitialisations();

        XTSService service = new XTSService();
        return service;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn({"xtsService"})
    public OutboundBridgeRecoveryManager outboundBridgeRecoveryManager() {
        return new OutboundBridgeRecoveryManager();
    }
    
    @Bean
    public UserTransaction userTransaction() {
        return new UserTransactionImple();
    }
}
