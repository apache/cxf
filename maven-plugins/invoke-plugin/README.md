# cxf-invoke-plugin
Maven plugin to invoke SOAP service from Maven.

To use tie the plugin to some execution phase and give it configuration options.

Here is an example -- place it in your `<build>` `<plugins>` section of your `pom.xml`:

    <plugin>
      <groupId>org.apache.cxf</groupId>
      <artifactId>cxf-invoke-plugin</artifactId>
      <version>3.1.8</version>
      <executions>
        <execution>
          <id>get-weather</id>
          <phase>package</phase>
          <goals>
            <goal>invoke-soap</goal>
          </goals>
          <configuration>
            <wsdl>http://webservicex.net/globalweather.asmx?wsdl</wsdl>
            <namespace>http://www.webserviceX.NET</namespace>
            <serviceName>GlobalWeather</serviceName>
            <portName>GlobalWeatherSoap12</portName>
            <operation>GetWeather</operation>
            <request>
              <GetWeather xmlns="http://www.webserviceX.NET">
                <CityName>Berlin-Tegel</CityName>
                <CountryName>Germany</CountryName>
              </GetWeather>
            </request>
            <properties>
              <weather>//*[local-name() = 'GetWeatherResult']</weather>
            <properties>
          </configuration>
        </execution>
      </executions>
    </plugin>

In this example, during `package` phase plugin will download the WSDL file from `http://webservicex.net/globalweather.asmx?wsdl`,
and invoke the service `http://www.webserviceX.NET:GlobalWeather` over `http://www.webserviceX.NET:GlobalWeatherSoap12` port, using
`http://www.webserviceX.NET:GetWeather` operation with the specified raw SOAP request (it will be placed in the `Body` of SOAP
`Envelope`).

The response of the SOAP service is parsed and using XPATH expression property named `weather` is extracted, this can
be used in the rest of the Maven execution.

The SOAP request and the SOAP response are written into files called `request.xml` and `response.xml` in the projects
`target/get-weather` directory, named afer the execution id.

## More advanced options

SOAP headers can be now specified using `headers` property.

Request will be repeated if `repeatUntil` XPath expression is defined and returns `true`, `repeatInterval` (default 
5 sec) can be used to change the frequency of the repetition.

For example:

    <plugin>
      <groupId>org.apache.cxf</groupId>
      <artifactId>cxf-invoke-plugin</artifactId>
      <version>3.1.8</version>
      <executions>
        <execution>
          <id>get-weather</id>
          <phase>package</phase>
          <goals>
            <goal>invoke-soap</goal>
          </goals>
          <configuration>
            <wsdl>http://webservicex.net/globalweather.asmx?wsdl</wsdl>
            <namespace>http://www.webserviceX.NET</namespace>
            <serviceName>GlobalWeather</serviceName>
            <portName>GlobalWeatherSoap12</portName>
            <operation>GetWeather</operation>
            <headers>
                <custom xmlns="http://my.custom.header">hello</custom>
            </headers>
            <request>
              <GetWeather xmlns="http://www.webserviceX.NET">
                <CityName>Berlin-Tegel</CityName>
                <CountryName>Germany</CountryName>
              </GetWeather>
            </request>
            <repeatUntil>boolean(//*[local-name() = 'GetWeatherResult' and text() != 'Data Not Found')<repeatUntil>
            <repeatInterval>10000<repeatInterval>
          </configuration>
        </execution>
      </executions>
    </plugin>
