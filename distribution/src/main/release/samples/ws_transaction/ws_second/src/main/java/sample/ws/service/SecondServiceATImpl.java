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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.jws.HandlerChain;
import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.logging.Logger;

@Service
@WebService(serviceName = "SecondServiceATService", portName = "SecondServiceAT", name = "SecondServiceAT", targetNamespace = "http://service.ws.sample")
@SOAPBinding(style = SOAPBinding.Style.RPC)
@HandlerChain(file = "/wstx_handlers.xml")
@Transactional(Transactional.TxType.MANDATORY) // default is REQUIRED
public class SecondServiceATImpl implements SecondServiceAT {

    private static final Integer ENTITY_ID = 1;

    private static final Logger LOG = Logger.getLogger(SecondServiceATImpl.class.getName());

    @Autowired
    private SecondCounterRepository service;

    /**
     * Incriment the first counter. This is done by updating the counter within a JTA transaction. The JTA transaction
     * was automatically bridged from the WS-AT transaction.
     */
    @WebMethod
    public void incrementCounter(int num) {

        LOG.info("[SERVICE] Second service invoked to increment the counter by '" + num + "'");

        // invoke the backend business logic:
        LOG.info("[SERVICE] Using the JPA Entity Manager to update the counter within a JTA transaction");

        SecondCounter counter = lookupCounterEntity();
        counter.incrementCounter(num);
        service.save(counter);
    }

    @WebMethod
    public int getCounter() {
        LOG.info("[SERVICE] getCounter() invoked");
        SecondCounter counter = lookupCounterEntity();

        return counter.getCounter();
    }

    @WebMethod
    public void resetCounter() {
        SecondCounter counter = lookupCounterEntity();
        counter.setCounter(0);
        service.save(counter);
    }

    private SecondCounter lookupCounterEntity() {
        Optional<SecondCounter> counter = service.findById(ENTITY_ID);
        if (counter.isPresent()) {
            return counter.get();
        } else {
            SecondCounter second = new SecondCounter(ENTITY_ID, 0);
            service.save(second);
            return second;
        }
    }
}
