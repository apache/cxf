Debug mode
----------

Run application:

1) To run LogBorwser in the debug mode, you must type in the terminal:
    mvn clean gwt:run -Pdev
2) Open new browser's window and go to: 
    http://127.0.0.1:8888/logbrowser/LogBrowser.html?gwt.codesvr=127.0.0.1:9997

Configure settings:

4) Add new endpoint with URL:
    http://127.0.0.1:8888/log/logs
5) Generate entry logs by interacting with URL:
    http://127.0.0.1:8888/generate.html

Enjoy!