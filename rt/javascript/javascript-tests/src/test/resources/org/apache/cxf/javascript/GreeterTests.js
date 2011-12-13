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

function assertionFailed(explanation)
{
 	var assert = new Assert(explanation); // this will throw out in Java.
}

var globalNotifier = null;
var globalErrorStatus = null;
var globalErrorStatusText = null;
var globalResponseObject = null;
var globalSecondResponseObject = null;

function resetGlobals() {
	globalNotifier = null;
	globalErrorStatus = null;
	globalErrorStatusText = null;
	globalResponseObject = null;
}

function testErrorCallback(httpStatus, httpStatusText) 
{
    org_apache_cxf_trace.trace("test error");
	globalErrorStatus = httpStatus;
	globalStatusText = httpStatusText;
	globalNotifier.notify();
}

// Because there is an explicit response wrapper declared, we have a JavaScript
// object here that wraps up the simple 'string'. It is easier to validate it
// from Java, I think.
function testSuccessCallback(responseObject) 
{
    org_apache_cxf_trace.trace("test success");
	globalResponseObject = responseObject;
	globalNotifier.notify();
}

function success1(responseObject)
{
	globalResponseObject = responseObject;
}

function closure_success1(responseObject)
{
	globalResponseObject = responseObject;
	globalNotifier.count();
}

function closure_success2(responseObject)
{
	globalSecondResponseObject = responseObject;
	globalNotifier.count();
}

function sayHiTest(url)
{
	org_apache_cxf_trace.trace("Enter sayHi.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new cxf_apache_org_jstest_Greeter();
	  
	intf.url = url;
    intf.sayHi(testSuccessCallback, testErrorCallback);
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function requestClosureTest(url)
{
	org_apache_cxf_trace.trace("Enter sayHi.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_count_down_notifier(2);
	
	var intf;
    intf = new cxf_apache_org_jstest_Greeter();
	  
	intf.url = url;
    intf.sayHi(closure_success1, testErrorCallback);
    intf.sayHi(closure_success2, testErrorCallback);
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;

	
}

