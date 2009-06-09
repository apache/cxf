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

function testOpaqueURI()
{
	var r = new XMLHttpRequest();
	if(r.readyState != r.UNSENT) {
		assertionFailed("initial state not UNSENT");
	}
	r.open("GET", "uri:opaque", false);
}

function testNonAbsolute() {
	var r = new XMLHttpRequest();
	r.open("GET", "http:relative", false);
}

function testNonHttp() {
	var r = new XMLHttpRequest();
	r.open("GET", "ftp:relative", false);
}

function testSendNotOpenError() {
	var r = new XMLHttpRequest();
	r.send();
}

function testSyncHttpFetch() {
	
	var r = new XMLHttpRequest();
	
	r.open("GET", "http://localhost:8808/test.html", false);
	if (r.readyState != r.OPENED) {
		assertionFailed("state not OPENED after OPEN");
	}
	r.send();
	if (r.readyState != r.DONE) {
		assertionFailed("state not DONE after sync send.")
	}
	return r.responseText;
}

// global variable used for state checking.

var globalState = null;

function testStateNotificationSync() {
	globalState = null;
	var r = new XMLHttpRequest();
	// create closure.
	r.onreadystatechange = function() {
		globalState = r.readyState;
	}	
	r.open("GET", "http://localhost:8808/test.html", false);
	if (r.readyState != r.OPENED) {
		assertionFailed("state not OPENED after OPEN");
	}
	if(globalState != r.OPENED) {
		assertionFailed("global state not OPENED after OPEN (event handler didn't run?)");
	}
	r.send();
	if (r.readyState != r.DONE) {
		assertionFailed("state not DONE after sync send.")
	}
	if(globalState != r.DONE) {
		assertionFailed("global state not DONE after sync send (event handler didn't run?) :" + globalState);
	}
}

var asyncGotHeadersReceived = false;
var asyncGotLoading = false;
var outOfOrderError = null;
var asyncGotDone = false;
var	asyncStatus = -1;
var	asyncStatusText = null;
var	asyncResponseHeaders = null;
var globalAsyncRequest = null;

// to ensure everyone stays in sync, this does NOT call send.
function testAsyncHttpFetch1() {
	globalState = null;
	asyncGotHeadersReceived = false;
	asyncGotLoading = false;
	outOfOrderError = null;
	asyncGotDone = false;
	asyncStatus = -1;
	asyncStatusText = null;
	asyncResponseHeaders = null;
	
	var notifier = new org_apache_cxf_notifier();
	
	var r = new XMLHttpRequest();
	globalAsyncRequest = r;
	// create closure.
	r.onreadystatechange = function() {
		globalState = r.readyState;
		if(globalState == r.OPENED) {
			// no need to do anything will be checked below.
		} else if(globalState == r.HEADERS_RECEIVED) {
			asyncGotHeadersReceived = true;
		} else if(globalState == r.LOADING) {
			if(!asyncGotHeadersReceived) {
				outOfOrderError = "LOADING before HEADERS_RECEIVED";
			}
			asyncGotLoading = true;
		} else if(globalState = r.DONE) {
			if(!asyncGotLoading) {
				outOfOrderError = "DONE before LOADING";
			}
			asyncGotDone = true;
			asyncResponseText = r.responseText;
			asyncStatus = r.status;
			asyncStatusText = r.statusText;
			asyncResponseHeaders = r.getAllResponseHeaders();
			notifier.notify();
			globalAsyncRequest = null;
		}
	}	

	r.open("GET", "http://localhost:8808/test.html", true);
	if (r.readyState != r.OPENED) {
		assertionFailed("state not OPENED after OPEN");
	}
	if(globalState != r.OPENED) {
		assertionFailed("global state not OPENED after OPEN (event handler didn't run?)");
	}
	return notifier;
}

function testAsyncHttpFetch2() {
	globalAsyncRequest.send();
}
// this tests XML in both directions.
function testSyncXml(address, request) {
	
	var r = new XMLHttpRequest();
	r.open("POST", address, false);
	if (r.readyState != r.OPENED) {
		assertionFailed("state not OPENED after OPEN");
	}
	// just send it as text (or, really, whatever the Java code set up for us).
	r.setRequestHeader("Content-Type", "text/xml; charset=utf-8");
	r.send(request);
	if (r.readyState != r.DONE) {
		assertionFailed("state not DONE after sync send.")
	}
	return r.responseXML;
}
