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

package org.apache.cxf.clustering;

import java.util.List;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.message.Exchange;

/**
 * This strategy simply retries the invocation using the same Endpoint (CXF-2036).
 * 
 * @author Dennis Kieselhorst
 *
 */
public class RetryStrategy extends SequentialStrategy {

    private int maxNumberOfRetries;
    private int counter;
    
    /* (non-Javadoc)
     * @see org.apache.cxf.clustering.AbstractStaticFailoverStrategy#getAlternateEndpoints(
     * org.apache.cxf.message.Exchange)
     */
    @Override
    public List<Endpoint> getAlternateEndpoints(Exchange exchange) {
        return getEndpoints(exchange, stillTheSameAddress());
    }
    
    protected <T> T getNextAlternate(List<T> alternates) {
        return stillTheSameAddress() ? alternates.get(0) : alternates.remove(0);
    }

    protected boolean stillTheSameAddress() {
        if (maxNumberOfRetries == 0) {
            return true;
        }
        // let the target selector move to the next address
        // and then stay on the same address for maxNumberOfRetries
        if (++counter <= maxNumberOfRetries) {
            return true;    
        } else {
            counter = 0;
            return false;
        }
    }
    

    public void setMaxNumberOfRetries(int maxNumberOfRetries) {
        if (maxNumberOfRetries < 0) {
            throw new IllegalArgumentException();
        }
        this.maxNumberOfRetries = maxNumberOfRetries;
    }


    public int getMaxNumberOfRetries() {
        return maxNumberOfRetries;
    }

}