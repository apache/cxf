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
package org.apache.cxf.systest.jaxrs.sse;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;

public abstract class AbstractSseTest extends AbstractSseBaseTest {
    @Test
    public void testBooksStreamIsReturnedFromLastEventId() throws JsonProcessingException {
        Response r = createWebClient("/rest/api/bookstore/sse/100")
            .header(HttpHeaders.LAST_EVENT_ID_HEADER, 150)
            .get();
        assertEquals(Status.OK.getStatusCode(), r.getStatus());
        
        final String response = r.readEntity(String.class);
        assertThat(response, containsString("id: 151"));
        assertThat(response, containsString("data: " + toJson("New Book #151", 151)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #152", 152)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #153", 153)));
        
        assertThat(response, containsString("id: 152"));
        assertThat(response, containsString("data: " + toJson("New Book #154", 154)));
        
        r.close();
    }
}
