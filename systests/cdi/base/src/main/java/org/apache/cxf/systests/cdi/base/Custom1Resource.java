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
package org.apache.cxf.systests.cdi.base;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.systests.cdi.base.provider.Custom1ReaderWriter;

@ApplicationScoped
@Path("/custom/1")
public class Custom1Resource {
    @GET
    @Produces("custom1/default")
    public Payload custom1Default() {
        return new Payload();
    }

    @GET
    @Path("override")
    @Produces("custom1/overriden")
    public Payload custom1Overriden() {
        return new Payload();
    }

    @javax.enterprise.inject.Produces
    @Consumes("custom1/overriden")
    @Produces("custom1/overriden")
    public Custom1ReaderWriter overriden() {
        return new Custom1ReaderWriter();
    }

    public static class Payload {
    }
}
