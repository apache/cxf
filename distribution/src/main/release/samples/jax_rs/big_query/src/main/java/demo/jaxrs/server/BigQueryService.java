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
package demo.jaxrs.server;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.json.JsonMapObject;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext;

@Path("/search")
public class BigQueryService {

    private WebClient bigQueryClient;
    
    @GET
    @Path("/complete")
    @Produces("text/html")
    public BigQueryResponse completeBigQuery(@Context OidcClientTokenContext context) {
        
        ClientAccessToken accessToken = context.getToken();
        bigQueryClient.authorization(accessToken);
        
        MultivaluedMap<String, String> state = context.getState();
        
        String searchWord = state.getFirst("word");
        String maxResults = state.getFirst("maxResults");
        String bigQuerySelect = "SELECT corpus,corpus_date FROM publicdata:samples.shakespeare WHERE word=\\\"" 
            + searchWord + "\\\"";
        String bigQueryRequest = "{" +
            "\"kind\": \"bigquery#queryRequest\"," 
            + "\"query\": \"" + bigQuerySelect + "\","
            + "\"maxResults\": " + Integer.parseInt(maxResults)
            + "}";
        
        
        JsonMapObject jsonMap = bigQueryClient.post(bigQueryRequest, JsonMapObject.class);
        BigQueryResponse bigQueryResponse = new BigQueryResponse(context.getUserInfo().getName(),
                                                                 searchWord);
        
        List<Map<String, Object>> rows = CastUtils.cast((List<?>)jsonMap.getProperty("rows"));
        for (Map<String, Object> row : rows) {
            List<Map<String, Object>> fields = CastUtils.cast((List<?>)row.get("f"));
            ShakespeareText text = new ShakespeareText((String)fields.get(0).get("v"),
                                                       (String)fields.get(1).get("v"));
            bigQueryResponse.getTexts().add(text);
        }
        return bigQueryResponse;
    }

    public void setBigQueryClient(WebClient bigQueryClient) {
        this.bigQueryClient = bigQueryClient;
    }
}
