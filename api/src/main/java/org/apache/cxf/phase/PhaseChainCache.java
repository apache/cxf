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

package org.apache.cxf.phase;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.cxf.common.util.ModCountCopyOnWriteArrayList;
import org.apache.cxf.interceptor.Interceptor;

/**
 * The PhaseChainCache provides default interceptor chains for SOAP requests
 * and responses, both from the client and web service side.  The list of
 * phases supplied in the get() methods of this class are defined by default
 * within org.apache.cxf.phase.PhaseManagerImpl.  For an example of this class 
 * in use, check the sourcecode of org.apache.cxf.endpoint.ClientImpl.
 */
public final class PhaseChainCache {
    AtomicReference<ChainHolder> lastData = new AtomicReference<ChainHolder>();
    
    
    @SuppressWarnings("unchecked")
    public PhaseInterceptorChain get(SortedSet<Phase> phaseList,
                                     List<Interceptor> p1) {
        return getChain(phaseList, p1);
    }

    @SuppressWarnings("unchecked")
    public PhaseInterceptorChain get(SortedSet<Phase> phaseList,
                                     List<Interceptor> p1,
                                     List<Interceptor> p2) {
        return getChain(phaseList, p1, p2);
    }
    @SuppressWarnings("unchecked")
    public PhaseInterceptorChain get(SortedSet<Phase> phaseList,
                                     List<Interceptor> p1,
                                     List<Interceptor> p2,
                                     List<Interceptor> p3) {
        return getChain(phaseList, p1, p2, p3);
    }
    @SuppressWarnings("unchecked")
    public PhaseInterceptorChain get(SortedSet<Phase> phaseList,
                                     List<Interceptor> p1,
                                     List<Interceptor> p2,
                                     List<Interceptor> p3,
                                     List<Interceptor> p4) {
        return getChain(phaseList, p1, p2, p3, p4);
    }
    @SuppressWarnings("unchecked")
    public PhaseInterceptorChain get(SortedSet<Phase> phaseList,
                                     List<Interceptor> p1,
                                     List<Interceptor> p2,
                                     List<Interceptor> p3,
                                     List<Interceptor> p4,
                                     List<Interceptor> p5) {
        return getChain(phaseList, p1, p2, p3, p4, p5);
    }
    
    private PhaseInterceptorChain getChain(SortedSet<Phase> phaseList, List<Interceptor> ... providers) {
        ChainHolder last = lastData.get();
        
        if (last == null 
            || !last.matches(providers)) {
            
            PhaseInterceptorChain chain = new PhaseInterceptorChain(phaseList);
            List<ModCountCopyOnWriteArrayList<Interceptor>> copy 
                = new ArrayList<ModCountCopyOnWriteArrayList<Interceptor>>(providers.length);
            for (List<Interceptor> p : providers) {
                copy.add(new ModCountCopyOnWriteArrayList<Interceptor>(p));
                chain.add(p);
            }
            last = new ChainHolder(chain, copy);
            lastData.set(last);
        }
        
        
        return last.chain.cloneChain();
    }
    
    private static class ChainHolder {
        List<ModCountCopyOnWriteArrayList<Interceptor>> lists;
        PhaseInterceptorChain chain;
        
        ChainHolder(PhaseInterceptorChain c, List<ModCountCopyOnWriteArrayList<Interceptor>> l) {
            lists = l;
            chain = c;
        }
        
        boolean matches(List<Interceptor> ... providers) {
            if (lists.size() == providers.length) {
                for (int x = 0; x < providers.length; x++) {
                    if (lists.get(x).size() != providers[x].size()) {
                        return false;
                    }
                    
                    if (providers[x].getClass() == ModCountCopyOnWriteArrayList.class) {
                        if (((ModCountCopyOnWriteArrayList)providers[x]).getModCount()
                            != lists.get(x).getModCount()) {
                            return false;
                        }
                    } else {
                        ListIterator<Interceptor> i1 = lists.get(x).listIterator();
                        ListIterator<Interceptor> i2 = providers[x].listIterator();
                        
                        while (i1.hasNext()) {
                            if (i1.next() != i2.next()) {
                                return false;
                            }
                        }
                    }
                }
                return true;
            }
            return false;
        }
    }
}
