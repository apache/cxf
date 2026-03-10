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

package org.apache.cxf.systest.jaxrs.failover;

import java.util.List;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.apache.cxf.clustering.FailoverFeature;
import org.apache.cxf.clustering.LoadDistributorFeature;
import org.apache.cxf.clustering.SequentialStrategy;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;

import org.junit.Assert;
import org.junit.Test;
public class CXF9052Test {
    @Test
    public void noClustering() {
        makeRequest(List.of());
    }
    
    @Test
    public void failover() {
        var failover = new FailoverFeature();
        failover.setStrategy(makeStrategy());
        makeRequest(List.of(failover));
    }
    
    @Test
    public void loadDistributor() {
        var distro = new LoadDistributorFeature();
        distro.setStrategy(makeStrategy());
        makeRequest(List.of(distro));
    }

    private static SequentialStrategy makeStrategy() {
        var s = new SequentialStrategy();
        s.setAlternateAddresses(List.of("http://localhost:1234/test"));
        return s;
    }

    private static void makeRequest(List<Feature> features) {
        var fct = new JAXRSClientFactoryBean();
        fct.setFeatures(features);
        fct.setServiceClass(Root.class);
        fct.setAddress("http://localhost:1234/test");
        try {
            fct.create(Root.class).sub().name();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("/sub/name"));
        }
    }
    
    interface Root {
        @Path("/sub") Sub sub();
    }
    
    interface Sub {
        @GET @Path("/name") String name();
    }
}
