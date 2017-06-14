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
package org.apache.cxf.rs.security.jose.common;

public class JoseProducerConsumer {
    
    private JoseProducer producer = new JoseProducer();
    private JoseConsumer consumer = new JoseConsumer();
    
    public String processData(String data) {
        return producer.processData(data);
    }
    public String getData(String data) {
        return consumer.getData(data);
    }
    
    public void setProducer(JoseProducer producer) {
        this.producer = producer;
    }
    public void setConsumer(JoseConsumer consumer) {
        this.consumer = consumer;
    }
}
