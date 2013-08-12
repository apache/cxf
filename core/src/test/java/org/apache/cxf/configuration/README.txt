NOTE: subdirectory foo contains code generated from src/test/resources/schemas/configuration/foo.xsd
using Sun's maven2 jxc plugin.
The code is checked in to avoid using the maven xjc plugin here as this conflicts with its subsequent 
use in modules where it will be run with extensions, and the dependency on these cannot (and should not)
be declared here.
It seems that the classpath for the first execution is used for all subsequent executions.

