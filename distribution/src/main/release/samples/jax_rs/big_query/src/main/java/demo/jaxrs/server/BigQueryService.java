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

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;
import org.apache.cxf.rs.security.oauth2.common.ClientAccessToken;
import org.apache.cxf.rs.security.oidc.rp.OidcClientTokenContext;

@Path("/")
public class BigQueryService {

    private static final String BQ_SELECT =
        "SELECT corpus,corpus_date FROM publicdata:samples.shakespeare WHERE word=\\\"%s\\\"";
    private static final String BQ_REQUEST = "{"
        + "\"kind\": \"bigquery#queryRequest\","
        + "\"query\": \"%s\","
        + "\"maxResults\": %d"
        + "}";

    @Context
    private OidcClientTokenContext oidcContext;
    private WebClient bigQueryClient;

    @GET
    @Path("/start")
    @Produces("text/html")
    public BigQueryStart startBigQuerySearch() {
        return new BigQueryStart(getUserInfo());
    }

    @POST
    @Path("/complete")
    @Consumes("application/x-www-form-urlencoded")
    @Produces("text/html")
    public BigQueryResponse completeBigQuerySearch(@FormParam("word") String searchWord,
                                                   @FormParam("maxResults") String maxResults) {

        ClientAccessToken accessToken = oidcContext.getToken();

        BigQueryResponse bigQueryResponse = new BigQueryResponse(getUserInfo(), searchWord);
        bigQueryResponse.setTexts(getMatchingTexts(bigQueryClient, accessToken, searchWord, maxResults));
        return bigQueryResponse;
    }

    private String getUserInfo() {
        if (oidcContext.getUserInfo() != null) {
            return oidcContext.getUserInfo().getName();
        } else {
            return oidcContext.getIdToken().getSubject();
        }

    }

    public void setBigQueryClient(WebClient bigQueryClient) {
        this.bigQueryClient = bigQueryClient;
    }

    static List<ShakespeareText> getMatchingTexts(WebClient bqClient, ClientAccessToken accessToken,
                                                  String searchWord, String maxResults) {
        bqClient.authorization(accessToken);
        String bigQuerySelect = String.format(BQ_SELECT, searchWord);
        String bigQueryRequest = String.format(BQ_REQUEST, bigQuerySelect, Integer.parseInt(maxResults));

        JsonMapObject jsonMap = bqClient.post(bigQueryRequest, JsonMapObject.class);

        List<ShakespeareText> texts = new LinkedList<>();
        List<Map<String, Object>> rows = CastUtils.cast((List<?>)jsonMap.getProperty("rows"));
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                List<Map<String, Object>> fields = CastUtils.cast((List<?>)row.get("f"));
                ShakespeareText text = new ShakespeareText((String)fields.get(0).get("v"),
                                                           (String)fields.get(1).get("v"));
                texts.add(text);
            }
        }
        return texts;
    }
}
