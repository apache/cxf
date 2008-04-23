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

// aegis/simple doesn't understand 'oneway', so we have a dummy success function.
function success()
{
}

function error(httpStatus, httpStatusText) 
{
    org_apache_cxf_trace.trace("error");
	globalErrorStatus = httpStatus;
	globalStatusText = httpStatusText;
	globalNotifier.notify();
}

function testAnyNToServerRaw(url)
{
	var service = new fortest_javascript_cxf_apache_org__AegisServicePortType();
	service.url = url;
	
	var arrayItem = new fortest_javascript_cxf_apache_org__ArrayOfAnyType();
	var holderArray = [];
	holderArray.push(new org_apache_cxf_raw_any_holder("<walrus xmlns='uri:iam'>tusks</walrus>"));
	holderArray.push(new org_apache_cxf_raw_any_holder("<penguin xmlns='uri:linux'>emperor</penguin>"));
	holderArray.push(new org_apache_cxf_raw_any_holder("<moon xmlns='uri:planets'>blue</moon>"));
	arrayItem.setAnyType(holderArray);
	service.acceptAny(success, error, "before items", arrayItem);
}

function testAnyNToServerRawTyped(url)
{
	var service = new fortest_javascript_cxf_apache_org__AegisServicePortType();
	service.url = url;
	
	var arrayItem = new fortest_javascript_cxf_apache_org__ArrayOfAnyType();
	var holderArray = [];
	var holder = new org_apache_cxf_raw_typed_any_holder("http://aegis.fortest.javascript.cxf.apache.org",
														 "Mammal",
														 "<name xmlns='http://aegis.fortest.javascript.cxf.apache.org'>cat</name>");
	holderArray.push(holder);
	holder = new org_apache_cxf_raw_typed_any_holder("http://www.w3.org/2001/XMLSchema", 
													 "string",
													 "This is the cereal &lt; shot from guns");
	
	holderArray.push(holder);
	arrayItem.setAnyType(holderArray);
	service.acceptObjects(success, error, arrayItem);
}

function returnBeanWithAnyTypeArraySuccess(bean)
{
	// let the Java code sort out what we got.
	globalResponseObject = bean;
	globalNotifier.notify();
}

function testReturningBeanWithAnyTypeArray(url) 
{
    resetGlobals();
	globalNotifier = new org_apache_cxf_notifier();

	var service = new fortest_javascript_cxf_apache_org__AegisServicePortType();
	service.url = url;
	service.returnBeanWithAnyTypeArray(returnBeanWithAnyTypeArraySuccess, error);
	return globalNotifier; 
}

