/**
 * client.js
 * 
 * A client program to interact with samples/jax_rs/websocket's server.
 * 
 * 
 */

"use strict";

// set the host url and path if the service runs at a different location
var HOST_URL = 'http://localhost:9000';
var CONTEXT_PATH = "/demo"

var reader = require('readline');
var prompt = reader.createInterface(process.stdin, process.stdout);

var atmosphere = require('atmosphere.js');

var request = { url: HOST_URL + CONTEXT_PATH,
                transport : 'websocket',
                trackMessageLength: false,
                dropHeaders: false,
                reconnectInterval : 5000};
var isopen = false;

const TRANSPORT_NAMES = ["websocket", "sse"];

const COMMAND_LIST = 
    [["add name",       "Add a new consumer and return the customer instance. (e.g., add green)"],
     ["delete id",      "Delete the customer. (e.g., delete 124)"],
     ["get id",         "Return the customere. (e.g., get 123)"],
     ["quit",           "Quit the application."],
     ["subscribe",      "Subscribe to the customer queries."],
     ["unsubscribe",    "Unsubscribe from the customer queries."],
     ["update id name", "Update the customer. (e.g., update 125 red)"]];

function selectOption(c, opts) {
    var i = parseInt(c);
    if (!(i >= 0 && i < opts.length)) {
        console.log('Invalid selection: ' + c + '; Using ' + opts[0]);
        i = 0;
    }
    return opts[i];
}

function getArgs(name, msg) {
    var sp = name.length;
    if (msg.length > name.length && msg.charAt(name.length) != ' ') {
        // remove the command suffix
        sp = msg.indexOf(' ', name.length);
        if (sp < 0) {
            sp = msg.length;
        }
    }
    return msg.substring(sp).trim().split(' ');
}

function createAddCustomerPayload(name) {
    return "<?xml version=\"1.0\"?>\n<Customer>\n    <name>" + name + "</name>\n</Customer>\n";
}

function createUpdateCustomerPayload(id, name) {
    return "<?xml version=\"1.0\"?>\n<Customer>\n    <name>" + name + "</name>\n    <id>" + id + "</id>\n</Customer>\n";
}

///

function doHelp() {
    console.log('Available commands');
    for (var i = 0; i < COMMAND_LIST.length; i++) { 
        var c = COMMAND_LIST[i][0];
        console.log(c + "                    ".substring(0, 20 - c.length) + COMMAND_LIST[i][1]);
    }
}

function doAdd(v) {
    var req;
    if (transport == 'websocket') {
        req = "POST " + CONTEXT_PATH + "/customerservice/customers\r\nContent-Type: text/xml; charset='utf-8'\r\nAccept: text/xml\r\n\r\n" 
            + createAddCustomerPayload(v[0]);
    } else if (transport == 'sse') {
        req = {"method": "POST", "url": HOST_URL + CONTEXT_PATH + "/customerservice/customers", "headers": {"content-type": "text/xml; charset=utf-8", "accept": "text/xml"}, "data": createAddCustomerPayload(v[0])}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doDelete(v) {
    var req;
    if (transport == 'websocket') {
        req = "DELETE" + CONTEXT_PATH + "/customerservice/customers/" + v[0];
    } else if (transport == 'sse') {
        req = {"method": "DELETE", "url": HOST_URL + CONTEXT_PATH + "/customerservice/customers/" + v[0]}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doGet(v) {
    var req;
    if (transport == 'websocket') {
        req = "GET " + CONTEXT_PATH + "/customerservice/customers/" + v[0];
    } else if (transport == 'sse') {
        req = {"method": "GET", "url": HOST_URL + CONTEXT_PATH + "/customerservice/customers/" + v[0]}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doSubscribe() {
    var req;
    if (transport == 'websocket') {
        req = "GET " + CONTEXT_PATH + "/customerservice/monitor\r\nAccept: text/plain\r\n";
    } else if (transport == 'sse') {
        req = {"method": "GET", "url": HOST_URL + CONTEXT_PATH + "/customerservice/monitor", "headers": {"accept": "text/plain"}}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doUnsubscribe() {
    var req;
    if (transport == 'websocket') {
        req = "GET " + CONTEXT_PATH + "/customerservice/unmonitor/*\r\nAccept: text/plain\r\n";
    } else if (transport == 'sse') {
        req = {"method": "GET", "url": HOST_URL + CONTEXT_PATH + "/customerservice/unmonitor/*", "headers": {"accept": "text/plain"}}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doUpdate(v) {
    var req;
    if (transport == 'websocket') {
        req = "PUT " + CONTEXT_PATH + "/customerservice/customers\r\nContent-Type: text/xml; charset='utf-8'\r\nAccept: text/xml\r\n\r\n" 
            + createUpdateCustomerPayload(v[0], v[1]);
    } else if (transport == 'sse') {
        req = {"method": "PUT", "url": HOST_URL + CONTEXT_PATH + "/customerservice/customers", "headers": {"content-type": "text/xml; charset=utf-8", "accept": "text/xml"}, "data": createUpdateCustomerPayload(v[0], v[1])}
    }
    console.log("TRACE: sending ", req);
    subSocket.push(req);
}

function doQuit() {
    subSocket.close();
    process.exit(0);
}

///

request.onOpen = function(response) {
    isopen = true;
    console.log('Connected using ' + response.transport);
    prompt.setPrompt("> ", 2);
    prompt.prompt();
};

request.onMessage = function (response) {
    var message = response.responseBody;
    console.log('Received: ', message);
    console.log('------------------------------------------------------------------------');
    prompt.setPrompt("> ", 2);
    prompt.prompt();
};

request.onReconnect = function(response) {
    console.log('Reconnecting ...');
}

request.onReopen = function(response) {
    isopen = true;
    console.log('Reconnected using ' + response.transport);
    prompt.setPrompt("> ", 2);
    prompt.prompt();
}

request.onClose = function(response) {
    isopen = false;
}

request.onError = function(response) {
    console.log("Sorry, something went wrong: " + response.responseBody);
};

var transport = null;
var subSocket = null;
var author = null;

console.log("Select transport ...");
for (var i = 0; i < TRANSPORT_NAMES.length; i++) { 
    console.log(i + ": " + TRANSPORT_NAMES[i]);
}
prompt.setPrompt("select: ", 6);
prompt.prompt();

prompt.
on('line', function(line) {
    var msg = line.trim();
    if (transport == null) {
        transport = selectOption(msg, TRANSPORT_NAMES);
        request.transport = transport;
        subSocket = atmosphere.subscribe(request);
        console.log("Connecting using " + transport);
        setTimeout(function() {
            if (!isopen) {
                console.log("Unable to open a connection. Terminated.");
                process.exit(0);
            }
        }, 3000);
    } else if (msg.length == 0) {
        doHelp();
    } else if (msg.indexOf("add") == 0) {
        doAdd(getArgs("add", msg));
    } else if (msg.indexOf("del") == 0) {
        doDelete(getArgs("del", msg));
    } else if (msg.indexOf("get") == 0) {
        doGet(getArgs("get", msg));
    } else if (msg.indexOf("quit") == 0) {
        doQuit();
    } else if (msg.indexOf("sub") == 0) {
        doSubscribe(getArgs("sub", msg));
    } else if (msg.indexOf("unsub") == 0) {
        doUnsubscribe(getArgs("unsub", msg));
    } else if (msg.indexOf("update") == 0) {
        doUpdate(getArgs("update", msg));
    }
    prompt.setPrompt("> ", 2);
    prompt.prompt();
}).
on('close', function() {
    console.log("close");
    process.exit(0);
});
