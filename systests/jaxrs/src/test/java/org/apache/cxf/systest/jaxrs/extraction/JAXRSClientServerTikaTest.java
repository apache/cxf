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

package org.apache.cxf.systest.jaxrs.extraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.MultipartProvider;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;
import org.apache.lucene.search.ScoreDoc;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JAXRSClientServerTikaTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(JAXRSClientServerTikaTest.class);

    @Ignore
    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

            final Map< String, Object > properties = new HashMap<>();
            properties.put("search.query.parameter.name", "$filter");
            properties.put("search.parser", new FiqlParser< SearchBean >(SearchBean.class));
            properties.put(SearchUtils.DATE_FORMAT_PROPERTY, "yyyy/MM/dd");

            sf.setResourceClasses(BookCatalog.class);
            sf.setResourceProvider(BookCatalog.class, new SingletonResourceProvider(new BookCatalog()));
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.setProperties(properties);
            sf.setProvider(new MultipartProvider());
            sf.setProvider(new SearchContextProvider());
            sf.setProvider(new JacksonJsonProvider());

            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        //keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Before
    public void setUp() {
        createWebClient("/catalog").delete();
    }

    @Test
    public void testUploadIndexAndSearchPdfFile() {
        final WebClient wc = createWebClient("/catalog").type(MediaType.MULTIPART_FORM_DATA);

        final ContentDisposition disposition = new ContentDisposition("attachment;filename=testPDF.pdf");
        final Attachment attachment = new Attachment("root",
            getClass().getResourceAsStream("/files/testPDF.pdf"), disposition);
        wc.post(new MultipartBody(attachment));

        final Collection<ScoreDoc> hits = search("dcterms:modified=le=2007-09-16T09:00:00");
        assertEquals(hits.size(), 1);
    }

    @Test
    public void testUploadIndexAndSearchPdfFileUsingUserDefinedDatePattern() {
        final WebClient wc = createWebClient("/catalog").type(MediaType.MULTIPART_FORM_DATA);

        final ContentDisposition disposition = new ContentDisposition("attachment;filename=testPDF.pdf");
        final Attachment attachment = new Attachment("root",
            getClass().getResourceAsStream("/files/testPDF.pdf"), disposition);
        wc.post(new MultipartBody(attachment));

        // Use user-defined date pattern
        final Collection<ScoreDoc> hits = search("dcterms:modified=le=2007/09/16");
        assertEquals(hits.size(), 1);
    }

    @SuppressWarnings("unchecked")
    private Collection<ScoreDoc> search(final String expression) {
        return createWebClient("/catalog")
            .accept(MediaType.APPLICATION_JSON)
            .query("$filter", expression)
            .get(Collection.class);
    }

    private WebClient createWebClient(final String url) {
        WebClient wc = WebClient.create("http://localhost:" + PORT + url,
            Arrays.asList(new MultipartProvider(), new JacksonJsonProvider()));
        WebClient.getConfig(wc).getHttpConduit().getClient().setReceiveTimeout(10000000L);
        return wc;
    }
}
