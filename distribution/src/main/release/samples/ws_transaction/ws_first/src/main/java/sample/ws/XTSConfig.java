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

package sample.ws;

import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.txbridge.inbound.InboundBridgeRecoveryManager;
import org.jboss.jbossts.xts.environment.WSCEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.transaction.jta.JtaTransactionManager;

import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionSynchronizationRegistryImple;

import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;
import jakarta.transaction.UserTransaction;

@Configuration
public class XTSConfig {
    @Value( "${server.port}" )
    private int port;

    @Bean(name = "xtsService", initMethod = "start", destroyMethod = "stop")
    public XTSService xtsService() {
        WSCEnvironmentBean wscEnvironmentBean = XTSPropertyManager.getWSCEnvironmentBean();
        wscEnvironmentBean.setBindPort11(port);

        XTSService service = new XTSService();
        return service;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn({"xtsService"})
    public InboundBridgeRecoveryManager inboundBridgeRecoveryManager() {
        return new InboundBridgeRecoveryManager();
    }
    
    @Bean
    public UserTransaction narayanaUserTransaction() {
        return com.arjuna.ats.jta.UserTransaction.userTransaction();
    }

    @Bean
    public TransactionManager narayanaTransactionManager() {
        return com.arjuna.ats.jta.TransactionManager.transactionManager();
    }

    @Bean
    public TransactionSynchronizationRegistry narayanaTransactionSynchronizationRegistry() {
        return new TransactionSynchronizationRegistryImple();
    }

    @Bean
    public JtaTransactionManager transactionManager(UserTransaction userTransaction,
            TransactionManager transactionManager,
            TransactionSynchronizationRegistry transactionSynchronizationRegistry) {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager(userTransaction, transactionManager);
        jtaTransactionManager.setTransactionSynchronizationRegistry(transactionSynchronizationRegistry);
        return jtaTransactionManager;
    }
}
