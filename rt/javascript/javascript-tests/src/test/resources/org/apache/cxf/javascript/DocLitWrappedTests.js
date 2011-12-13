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

function resetGlobals() {
	globalNotifier = null;
	globalErrorStatus = null;
	globalErrorStatusText = null;
	globalResponseObject = null;
}

function test1ErrorCallback(httpStatus, httpStatusText) 
{
    org_apache_cxf_trace.trace("test1/2/3 error");
	globalErrorStatus = httpStatus;
	globalStatusText = httpStatusText;
	globalNotifier.notify();
}

// Because there is an explicit response wrapper declared, we have a JavaScript
// object here that wraps up the simple 'string'. It is easier to validate it
// from Java, I think.
function test1SuccessCallback(responseObject) 
{
    org_apache_cxf_trace.trace("test1/2/3 success");
	globalResponseObject = responseObject;
	globalNotifier.notify();
}

function test1(url, doubleArg, floatArg, intArg, longArg, stringArg) 
{
	org_apache_cxf_trace.trace("Enter test1.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	intf.url = url;
	// param order is from propOrder on the wrapper class.
	intf.basicTypeFunctionReturnString(test1SuccessCallback, test1ErrorCallback,
									   stringArg, intArg, longArg, floatArg, doubleArg 
									   ); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function test2(url, doubleArg, floatArg, intArg, longArg, stringArg) 
{
	org_apache_cxf_trace.trace("Enter test2.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	intf.url = url;
	// param order from the interface
	intf.basicTypeFunctionReturnStringNoWrappers(test1SuccessCallback, test1ErrorCallback, 
	                                   			 stringArg, intArg, longArg, floatArg, doubleArg);
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function test3(url, doubleArg, floatArg, intArg, longArg, stringArg) 
{
	org_apache_cxf_trace.trace("Enter test3.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	intf.url = url;
	// param order from the interface
	intf.basicTypeFunctionReturnInt(test1SuccessCallback, test1ErrorCallback, 
	                                stringArg, intArg, longArg, floatArg, doubleArg);
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function test4(url, wrap, beanArg, beansArg)
{
	org_apache_cxf_trace.trace("Enter test4.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	  
	intf.url = url;
	// param order from the interface
	if(wrap)
		intf.beanFunctionWithWrapper(test1SuccessCallback, test1ErrorCallback, beanArg, beansArg); 
    else	
	  intf.beanFunction(test1SuccessCallback, test1ErrorCallback, beanArg, beansArg); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function testInheritance(url) {
	org_apache_cxf_trace.trace("inheritance test.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
	
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	intf.url = url;

	var derived = new org_apache_cxf_javascript_testns_inheritanceTestDerived();
	derived.setId(33);
	derived.setDerived("arrived");
	derived.setName("less");
	intf.inheritanceTestFunction(test1SuccessCallback, test1ErrorCallback, derived);

	return globalNotifier;
}

function testDummyHeader(url, stringArg) 
{
	org_apache_cxf_trace.trace("Enter testDummyHeader.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf = new org_apache_cxf_javascript_fortest_SimpleDocLitWrapped();
	intf.url = url;
	// param order from the interface
	intf.echoWithHeader(test1SuccessCallback, test1ErrorCallback, stringArg);
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}