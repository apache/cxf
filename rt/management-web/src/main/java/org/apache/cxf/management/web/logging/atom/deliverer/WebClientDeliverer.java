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
package org.apache.cxf.management.web.logging.atom.deliverer;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.abdera.model.Element;
import org.apache.abdera.model.Feed;
import org.apache.commons.lang.Validate;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.atom.AtomEntryProvider;
import org.apache.cxf.jaxrs.provider.atom.AtomFeedProvider;

/**
 * Marshaling and delivering based on JAXRS' WebClient.
 */
public final class WebClientDeliverer implements Deliverer {
    private WebClient wc;

    @SuppressWarnings("unchecked")
    public WebClientDeliverer(String deliveryAddress) {
        Validate.notEmpty(deliveryAddress, "deliveryAddress is empty or null");
        List<?> providers = Arrays.asList(new AtomFeedProvider(), new AtomEntryProvider());
        wc = WebClient.create(deliveryAddress, providers);
    }

    public WebClientDeliverer(WebClient wc) {
        Validate.notNull(wc, "wc is null");
        this.wc = wc;
    }

    public boolean deliver(Element element) {
        String type = element instanceof Feed ? "application/atom+xml" : "application/atom+xml;type=entry";
        wc.type(type);
        Response res = wc.post(element);
        int status = res.getStatus();
        return status >= 200 && status <= 299;
    }
    
    public String getEndpointAddress() {
        return wc.getBaseURI().toString();
    }
}
