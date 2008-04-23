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

function beanFunctionTest(url, beanArg, beansArg)
{
	org_apache_cxf_trace.trace("Enter beanFunctionTest.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitBare();
	  
	intf.url = url;
	// provide the extra layer of object for the array part, save the Java code the trouble.
	var beanArrayHolder = new org_apache_cxf_javascript_testns_testBean1Array();
	beanArrayHolder.setItem(beansArg);
	// param order from the interface
    intf.beanFunction(test1SuccessCallback, test1ErrorCallback, beanArg, beanArrayHolder); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function compliantTest(url, beanArg)
{
	org_apache_cxf_trace.trace("Enter compliantTest.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitBare();
	  
	intf.url = url;
	// param order from the interface
    intf.compliant(test1SuccessCallback, test1ErrorCallback, beanArg); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function compliantNoArgsTest(url)
{
	org_apache_cxf_trace.trace("Enter compliantArgsTest.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitBare();
	  
	intf.url = url;
	// param order from the interface
    intf.compliantNoArgs(test1SuccessCallback, test1ErrorCallback); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function actionMethodTest(url, param)
{
	org_apache_cxf_trace.trace("Enter actionMethodTest.");
	resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();
	
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitBare();
	  
	intf.url = url;

    intf.actionMethod(test1SuccessCallback, test1ErrorCallback, param); 
    // Return the notifier as a convenience to the Java code.
	return globalNotifier;
}

function onewayTest(url, param)
{
	org_apache_cxf_trace.trace("Enter onewayTest.");
	resetGlobals();
	globalNotifier = null; // no notifications.
	var intf;
    intf = new org_apache_cxf_javascript_fortest_SimpleDocLitBare();
	  
	intf.url = url;

    intf.oneWay(param); 
}
