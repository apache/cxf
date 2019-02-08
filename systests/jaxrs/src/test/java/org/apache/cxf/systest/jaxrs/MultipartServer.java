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

package org.apache.cxf.systest.jaxrs;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.attachment.AttachmentDeserializer;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;


public class MultipartServer extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(MultipartServer.class);
    Server server;

    protected void run() {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(MultipartStore.class);

        Map<String, Object> props = new HashMap<>();
        props.put(AttachmentDeserializer.ATTACHMENT_MAX_SIZE, String.valueOf(1024 * 10));
        props.put(AttachmentDeserializer.ATTACHMENT_MEMORY_THRESHOLD, String.valueOf(1024 * 5));
        props.put(AttachmentDeserializer.ATTACHMENT_MAX_HEADER_SIZE, String.valueOf(400));
        sf.setProperties(props);
        //default lifecycle is per-request, change it to singleton
        sf.setResourceProvider(MultipartStore.class,
                               new SingletonResourceProvider(new MultipartStore()));
        sf.setAddress("http://localhost:" + PORT + "/");

        server = sf.create();
    }
    public void tearDown() throws Exception {
        server.stop();
        server.destroy();
        server = null;
    }
    public static void main(String[] args) {
        try {
            MultipartServer s = new MultipartServer();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }

}
