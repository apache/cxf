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

package demo.jaxrs.server.simple;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;


public class Server {

    protected Server(String[] args) throws Exception {
        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setResourceClasses(StreamingService.class);

        String receiverType = args.length == 1 && "-receiverType=queue".equals(args[0])
            ? "queue" : "string";
        sf.setResourceProvider(StreamingService.class,
            new SingletonResourceProvider(new StreamingService(receiverType)));
        sf.setAddress("http://localhost:9000/spark");

        sf.create();
    }

    public static void main(String[] args) throws Exception {
        new Server(args);
        System.out.println("Server ready...");
        Thread.sleep(60 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }


}
