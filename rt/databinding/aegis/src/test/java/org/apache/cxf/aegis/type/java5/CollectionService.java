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

package org.apache.cxf.aegis.type.java5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.Stack;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CollectionService implements CollectionServiceInterface {
    
    private Map<String, Map<String, BeanWithGregorianDate>> lastComplexMap;
    
    /** {@inheritDoc}*/
    public Collection<String> getStrings() {
        return null;
    }

    /** {@inheritDoc}*/
    public void setLongs(Collection<Long> longs) {
    }

    /** {@inheritDoc}*/
    public Collection getUnannotatedStrings() {
        return null;
    }

    /** {@inheritDoc}*/
    public Collection<Collection<String>> getStringCollections() {
        return null;
    }
    
    /** {@inheritDoc}*/
    public void takeDoubleList(List<Double> doublesList) {
    }
    
    /** {@inheritDoc}*/
    public String takeSortedStrings(SortedSet<String> strings) {
        return strings.first();
    }

    public void method1(List<String> headers1) {
        // do nothing, this is purely for schema issues.
    }

    public String takeStack(Stack<String> strings) {
        return strings.firstElement();
    }

    //CHECKSTYLE:OFF
    public String takeUnsortedSet(HashSet<String> strings) {
        return Integer.toString(strings.size());
    }

    public String takeArrayList(ArrayList<String> strings) {
        return strings.get(0);
    }
    //CHECKSTYLE:ON

    public void mapOfMapWithStringAndPojo(Map<String, Map<String, BeanWithGregorianDate>> bigParam) {
        lastComplexMap = bigParam;
    }

    protected Map<String, Map<String, BeanWithGregorianDate>> getLastComplexMap() {
        return lastComplexMap;
    }

    public Collection<double[]> returnCollectionOfPrimitiveArrays() {
        List<double[]> data = new ArrayList<double[]>();
        double[] dataArray = new double[] {3.14, 2.0, -666.6 };
        data.add(dataArray);
        dataArray = new double[] {-666.6, 3.14, 2.0, 0 };
        data.add(dataArray);
        return data;
    }

    public Collection<Document[]> returnCollectionOfDOMFragments() {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document d1 = builder.newDocument();
            Element e = d1.createElement("Horse");
            e.setAttribute("cover", "feathers");
            d1.appendChild(e);
            Document d2 = builder.newDocument();
            e = d2.createElement("Cantelope");
            d2.appendChild(e);
            e.setAttribute("not-an", "ungulate");
            Document[] da = new Document[] {d1, d2};
            List<Document[]> l = new ArrayList<Document[]>();
            l.add(da);
            da = new Document[] {d2, d1};
            l.add(da);
            return l;
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

}