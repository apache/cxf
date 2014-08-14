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

package demo.jaxrs.search.server;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.jaxrs.provider.jsrjsonp.JsrJsonpProvider;

public class Server {

    protected Server() throws Exception {
        final Storage storage = new Storage();
        final Map< String, Object > properties = new HashMap< String, Object >();        
        properties.put("search.query.parameter.name", "$filter");
        properties.put("search.parser", new FiqlParser< SearchBean >(SearchBean.class));
        properties.put(SearchUtils.DATE_FORMAT_PROPERTY, "yyyy/MM/dd");

        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setProperties(properties);
        sf.setResourceClasses(Catalog.class);
        sf.setResourceProvider(Catalog.class, new SingletonResourceProvider(new Catalog(storage)));
        sf.setAddress("http://localhost:9000/");
        sf.setProvider(new MultipartProvider());
        sf.setProvider(new SearchContextProvider());
        sf.setProvider(new JsrJsonpProvider());
        sf.create();
    }

    public static void main(String args[]) throws Exception {
        new Server();
        System.out.println("Server ready...");

        Thread.sleep(5 * 6000 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }
}
