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
package org.apache.cxf.jaxrs.ext.search.lucene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.lucene.search.Query;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LuceneQueryVisitorFiqlTest extends AbstractLuceneQueryVisitorTest {
    @Test
    public void testTextContentMatchEqual() throws Exception {

        doTestTextContentMatch("ct==text");
    }

    @Test
    public void testTextContentMatchNotEqual() throws Exception {

        Query query = createTermQuery("contents", "ct!=text");
        doTestNoMatch(query);

    }

    @Test
    public void testTextContentMatchNotEqualPositive() throws Exception {

        Query query = createTermQuery("contents", "ct!=bar");
        doTestNoMatch(query);

    }

    @Test
    public void testTextContentMatchWildcardEnd() throws Exception {
        doTestTextContentMatch("ct==tex*");
    }

    @Test
    public void testTextContentMatchWildcardStart() throws Exception {
        doTestTextContentMatch("ct==*ext");
    }

    @Test
    public void testIntContentMatchGreater() throws Exception {
        doTestIntContentMatch("ct=gt=3");
    }

    @Test
    public void testIntContentMatchGreaterWithClassFiled() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct=gt=3", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }

    @Test
    public void testIntContentMatchGreaterNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=gt=5");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchGreaterOrEqual() throws Exception {
        doTestIntContentMatch("ct=ge=4");
        doTestIntContentMatch("ct=ge=3");
    }

    @Test
    public void testIntContentMatchGreaterOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=ge=5");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchLess() throws Exception {
        doTestIntContentMatch("ct=lt=5");
    }

    @Test
    public void testIntContentMatchLessNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=lt=3");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchLessOrEqual() throws Exception {
        doTestIntContentMatch("ct=le=4");
        doTestIntContentMatch("ct=le=5");
    }

    @Test
    public void testIntContentMatchLessOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct=le=3");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchEquals() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct==4", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }

    @Test
    public void testTextAndContentMatch() throws Exception {
        Query query = createTermQuery("contents==name;contents==text");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testTextAndContentNoMatch() throws Exception {
        Query query = createTermQuery("contents==bar;contents==text");
        doTestNoMatch(query);
    }

    @Test
    public void testTextOrContentMatch() throws Exception {
        Query query = createTermQuery("contents==bar,contents==text");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testTextOrContentNoMatch() throws Exception {
        Query query = createTermQuery("contents==bar,contents==foo");
        doTestNoMatch(query);
    }

    @Test
    public void testIntAndTextContentMatch() throws Exception {

        Query query = createTermQueryWithFieldClass("intfield==4;contents==text", Integer.class);
        doTestIntContentMatchWithQuery(query);
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testIntAndTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield==3;contents==text");
        doTestNoMatch(query);
    }

    @Test
    public void testIntOrTextContentMatch() throws Exception {
        Query query = createTermQuery("intfield==3,contents==text");
        doTestTextContentMatchWithQuery(query);
        doTestIntContentMatchWithQuery(query);

    }

    @Test
    public void testIntOrTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield==3,contents==bar");
        doTestNoMatch(query);
    }

    @Test
    public void testTextContentMatchEqualPhrase() throws Exception {
        Query query = createPhraseQuery("contents", "name==text");
        doTestTextContentMatchWithQuery(query);
    }

    @Test
    public void testTextContentMatchNotEqualPhrase() throws Exception {

        Query query = createPhraseQuery("contents", "name!=text");
        doTestNoMatch(query);
    }

    @Test
    public void testTextContentMatchEqualPhraseWildcard() throws Exception {
        Query query = createPhraseQuery("contents", "name==tex*");
        doTestTextContentMatchWithQuery(query);
    }

    @Test
    public void testThatMultipleQueriesForTheSameFieldAreHandledProperly() {
        final SearchCondition<SearchBean> filter1 = getParser().parse("name==text");
        final SearchCondition<SearchBean> filter2 = getParser().parse("name==word");
        final LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<>();

        visitor.visit(filter1);
        assertThat(visitor.getQuery().toString(), equalTo("name:text"));

        visitor.reset();
        visitor.visit(filter2);
        assertThat(visitor.getQuery().toString(), equalTo("name:word"));
    }

    @Test
    public void testThatMultipleQueriesForTheSameFieldAreThreadSafe() throws InterruptedException, ExecutionException {
        final LuceneQueryVisitor<SearchBean> visitor = new LuceneQueryVisitor<>();
        final ExecutorService executorService = Executors.newFixedThreadPool(5);

        final Collection< Future< ? > > futures = new ArrayList<>();
        for (int i = 0; i < 5; ++i) {
            final int index = i;

            futures.add(
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        final SearchCondition<SearchBean> filter = getParser().parse("name==text" + index);
                        visitor.reset();
                        visitor.visit(filter);

                        assertNotNull("Query should not be null", visitor.getQuery());
                        assertThat(visitor.getQuery().toString(), equalTo("name:text" + index));
                    }
                })
            );
        }

        executorService.shutdown();
        assertTrue("All threads should be terminated",
            executorService.awaitTermination(5, TimeUnit.SECONDS));

        for (final Future< ? > future: futures) {
            // The exception will be raised if queries are messed up
            future.get();
        }
    }


    @Override
    protected SearchConditionParser<SearchBean> getParser() {
        return new FiqlParser<SearchBean>(SearchBean.class);
    }
}
