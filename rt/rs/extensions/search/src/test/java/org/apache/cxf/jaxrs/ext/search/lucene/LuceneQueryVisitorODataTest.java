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
import org.apache.cxf.jaxrs.ext.search.odata.ODataParser;
import org.apache.lucene.search.Query;

import org.junit.Test;

public class LuceneQueryVisitorODataTest extends AbstractLuceneQueryVisitorTest {
    @Test
    public void testTextContentMatchEqual() throws Exception {

        doTestTextContentMatch("ct eq 'text'");
    }

    @Test
    public void testTextContentMatchNotEqual() throws Exception {

        Query query = createTermQuery("contents", "ct ne 'text'");
        doTestNoMatch(query);

    }

    @Test
    public void testTextContentMatchNotEqualPositive() throws Exception {

        Query query = createTermQuery("contents", "ct ne 'bar'");
        doTestNoMatch(query);

    }

    @Test
    public void testTextContentMatchWildcardEnd() throws Exception {
        doTestTextContentMatch("ct eq 'tex*'");
    }

    @Test
    public void testTextContentMatchWildcardStart() throws Exception {
        doTestTextContentMatch("ct eq '*ext'");
    }

    @Test
    public void testIntContentMatchGreater() throws Exception {
        doTestIntContentMatch("ct gt 3");
    }

    @Test
    public void testIntContentMatchGreaterWithClassFiled() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct gt 3", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }

    @Test
    public void testIntContentMatchGreaterNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct gt 5");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchGreaterOrEqual() throws Exception {
        doTestIntContentMatch("ct ge 4");
        doTestIntContentMatch("ct ge 3");
    }

    @Test
    public void testIntContentMatchGreaterOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct ge 5");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchLess() throws Exception {
        doTestIntContentMatch("ct lt 5");
    }

    @Test
    public void testIntContentMatchLessNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct lt 3");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchLessOrEqual() throws Exception {
        doTestIntContentMatch("ct le 4");
        doTestIntContentMatch("ct le 5");
    }

    @Test
    public void testIntContentMatchLessOrEqualNoMatch() throws Exception {
        Query query = createTermQuery("intfield", "ct le 3");
        doTestNoMatch(query);
    }

    @Test
    public void testIntContentMatchEquals() throws Exception {
        Query query = createTermQueryWithFieldClass("intfield", "ct eq 4", Integer.class);
        doTestIntContentMatchWithQuery(query);
    }

    @Test
    public void testTextAndContentMatch() throws Exception {
        Query query = createTermQuery("contents eq 'name' and contents eq 'text'");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testTextAndContentNoMatch() throws Exception {
        Query query = createTermQuery("contents eq 'bar' and contents eq 'text'");
        doTestNoMatch(query);
    }

    @Test
    public void testTextOrContentMatch() throws Exception {
        Query query = createTermQuery("contents eq 'bar' or contents eq 'text'");
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testTextOrContentNoMatch() throws Exception {
        Query query = createTermQuery("contents eq 'bar' or contents eq 'foo'");
        doTestNoMatch(query);
    }

    @Test
    public void testIntAndTextContentMatch() throws Exception {

        Query query = createTermQueryWithFieldClass("intfield eq 4 and contents eq 'text'", Integer.class);
        doTestIntContentMatchWithQuery(query);
        doTestTextContentMatchWithQuery(query);

    }

    @Test
    public void testIntAndTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield eq 3 and contents eq 'text'");
        doTestNoMatch(query);
    }

    @Test
    public void testIntOrTextContentMatch() throws Exception {
        Query query = createTermQuery("intfield eq 3 or contents eq 'text'");
        doTestTextContentMatchWithQuery(query);
        doTestIntContentMatchWithQuery(query);

    }

    @Test
    public void testIntOrTextContentNoMatch() throws Exception {
        Query query = createTermQuery("intfield eq 3 or contents eq 'bar'");
        doTestNoMatch(query);
    }

    @Test
    public void testTextContentMatchEqualPhrase() throws Exception {
        Query query = createPhraseQuery("contents", "name eq 'text'");
        doTestTextContentMatchWithQuery(query);
    }

    @Test
    public void testTextContentMatchNotEqualPhrase() throws Exception {

        Query query = createPhraseQuery("contents", "name ne 'text'");
        doTestNoMatch(query);
    }

    @Test
    public void testTextContentMatchEqualPhraseWildcard() throws Exception {
        Query query = createPhraseQuery("contents", "name eq 'tex*'");
        doTestTextContentMatchWithQuery(query);
    }

    @Override
    protected SearchConditionParser<SearchBean> getParser() {
        return new ODataParser<SearchBean>(SearchBean.class);
    }
}
