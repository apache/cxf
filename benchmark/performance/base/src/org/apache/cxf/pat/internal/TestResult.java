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
package org.apache.cxf.pat.internal;


public class TestResult {
    public static final String AVG_UNIT = " (ms)";
    private static final String THROUGHPUT_UNIT = " (invocations/sec)";
    
    private String name;
    private TestCaseBase testCase;
  
    private double avgResponseTime;
    private double throughput;
  
    public TestResult() {
        this("Default Result");
    }

    public TestResult(String cname) {
        this(cname, null);
    }

    public TestResult(String cname, TestCaseBase test) {
        this.name = cname;
        this.testCase = test;
    }

    public void compute(long startTime, long endTime, int numberOfInvocations) {
        double numOfInvocations = (double)numberOfInvocations;
        double duration = convertToSeconds(endTime - startTime);
      
        throughput = numOfInvocations / duration;
        avgResponseTime  = duration / numOfInvocations;
    
        System.out.println("Throughput: " + testCase.getOperationName() + " " + throughput + THROUGHPUT_UNIT);
        System.out.println("AVG. response time: " + avgResponseTime * 1000 + AVG_UNIT);
        System.out.println(numOfInvocations + " (invocations), running " + duration  + " (sec) ");

    }

    private double convertToSeconds(double ms) {
        return ms / 1000;
    }

    public String getName() {
        return this.name;
    }

    public double getAvgResponseTime() {
        return avgResponseTime;
    }

    public double getThroughput() {
        return throughput;
    }
}
