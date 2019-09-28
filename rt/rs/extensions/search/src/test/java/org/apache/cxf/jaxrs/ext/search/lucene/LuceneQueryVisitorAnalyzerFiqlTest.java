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

import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.apache.lucene.search.Query;

import org.junit.Test;

public class LuceneQueryVisitorAnalyzerFiqlTest extends AbstractLuceneQueryVisitorTest {
    @Test
    public void testTextContentMatchEqual() throws Exception {
        doTestTextContentMatchWithAnalyzer("ct==tEXt");
    }

    @Test
    public void testTextAndContentMatch() throws Exception {
        Query query = createTermQueryWithAnalyzer("contents==namE;contents==tExt");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testTextOrContentMatch() throws Exception {
        Query query = createTermQueryWithAnalyzer("contents==BAR,contents==TEXT");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testIntAndTextContentMatch() throws Exception {
        Query query = createTermQueryWithFieldClassWithAnalyzer("intfield==4;contents==teXt", Integer.class);
        doTestIntContentMatchWithQuery(query);
        doTestTextContentMatchWithQuery(query);
    }

    @Test
    public void testIntOrTextContentMatch() throws Exception {
        Query query = createTermQueryWithAnalyzer("intfield==3,contents==tExt");
        doTestTextContentMatchWithQuery(query);
        doTestIntContentMatchWithQuery(query);

    }

    @Test
    public void testTextContentMatchEqualPhrase() throws Exception {
        Query query = createPhraseQueryWithAnalyzer("contents", "name==TEXT");
        doTestTextContentMatchWithQuery(query);
    }

    @Override
    protected SearchConditionParser<SearchBean> getParser() {
        return new FiqlParser<SearchBean>(SearchBean.class);
    }
}
