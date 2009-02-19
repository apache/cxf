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

package org.apache.cxf.javascript.fortest;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import org.w3c.dom.Document;

import org.apache.cxf.javascript.fortest.aegis.BeanWithAnyTypeArray;
import org.apache.cxf.javascript.fortest.aegis.Mammal;
import org.apache.cxf.javascript.fortest.aegis.Vegetable;

/**
 * Service used to test out JavaScript talking to Aegis.
 */
public class AegisServiceImpl implements AegisService {
    private String acceptedString;
    private Collection<Document> acceptedCollection;
    private Collection<String> acceptedStrings;
    private Collection<Object> acceptedObjects;
    private CountDownLatch oneWayLatch;
    
    public Collection<Object> getAcceptedObjects() {
        return acceptedObjects;
    }

    public void reset() {
        acceptedString = null;
        acceptedCollection = null;
    }
    
    /** {@inheritDoc}*/
    public void acceptAny(String before, Collection<Document> anything) {
        acceptedString = before;
        acceptedCollection = anything;
        if (oneWayLatch != null) {
            oneWayLatch.countDown();
        }
    }

    /**
     * @return Returns the acceptedCollection.
     */
    public Collection<Document> getAcceptedCollection() {
        return acceptedCollection;
    }

    /**
     * @return Returns the acceptedString.
     */
    public String getAcceptedString() {
        return acceptedString;
    }

    public void acceptStrings(Collection<String> someStrings) {
        acceptedStrings = someStrings;
    }

    /**
     * @return Returns the acceptedStrings.
     */
    public Collection<String> getAcceptedStrings() {
        return acceptedStrings;
    }

    public void acceptObjects(Collection<Object> anything) {
        acceptedObjects = anything;
        if (oneWayLatch != null) {
            oneWayLatch.countDown();
        }
    }

    public BeanWithAnyTypeArray returnBeanWithAnyTypeArray() {
        BeanWithAnyTypeArray bwata = new BeanWithAnyTypeArray();
        bwata.setString("lima");
        Object[] obs = new Object[3];
        obs[0] = new Mammal();
        obs[1] = new Integer(42);
        obs[2] = new Vegetable(); // this is NOT in the WSDL.
        bwata.setObjects(obs);
        return bwata;
    }
    
    public void prepareToWaitForOneWay() {
        oneWayLatch = new CountDownLatch(1);
    }
    
    public void waitForOneWay() {
        if (oneWayLatch != null) {
            try {
                oneWayLatch.await();
            } catch (InterruptedException e) {
                // 
            }
            oneWayLatch = null;
        }
    }

}
