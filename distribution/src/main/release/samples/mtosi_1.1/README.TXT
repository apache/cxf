MTOSI 1.1 Samples
=================

mtosi_1.1 contains the following subdirectories:

wsdl            - contains the mtosi 1.1 wsdls
xsd             - contains the mtosi 1.1 schemas
alarm_retrieval - contains a simple sample implementation of the 
                  alarm_retrieval mtosi interface.  Please refer to
                  the alarm_retrieval/README.txt for instructions to
                  run that demo.
code_gen        - contains targets for generating code for mtosi
                  interfaces which do not have a sample implementation


Going beyond the basic sample
=============================

You can use the alarm_retrieval demo as a template for implementing
any of the other mtosi interfaces.  The code_gen directory
demonstrates the wsdl2java targets needed to generate code for the
remaining mtosi interfaces.

