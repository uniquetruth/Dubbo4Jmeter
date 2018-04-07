# Dubbo4Jmeter
A simple sampler for Apache Jmeter(v3.0+) which can test dubbo interface. Recommend for function tests only. For load tests, java sampler is more accurate.

What is dubbo: http://dubbo.incubator.apache.org/

This sampler can be used as a dubbo client. Usually a dubbo client should work with a business jar provided by dubbo server, but for testers(like me), sometimes the business jars change in high frequency. So I specifically wrote this Jmeter plugin for generice invoking.

Usage:
1. Compile and pack with command "mvn install".
2. Put target/dubboSampler.jar into %JMETER_HOME%/lib/ext directory.
3. Put the following jar files in the target/lib into %JMETER_HOME%/lib/ext directory.
avalon-framework-4.1.5.jar, dubbo-2.5.3.jar, gson-2.8.2<Integer>ar, javassist-3.4.ga.jar, netty-3.2.9.Final.jar
4. Start Jmeter, add DubboSampler into your script.
![dubbo sampler example](https://github.com/uniquetruth/Dubbo4Jmeter/blob/master/resource/ds.png)

How to fill the dubbo sampler's input areas?

**ip and port**: Whether use zookeeper or not, you can find ip and port in the service page of dubbo monitor center.<br/>
**interface name**: Full name of interface class(including package name).<br/>
**invoke method**: The method to be tested.<br/>
**implement**: Two choices: Telnet Client and Generic Service. Telnet Client means using the debugging interface provided by dubbo to exchange data. It relies very much on the implmentation of dubbo's debugging interface. It's unreliable when testing the method that includes some complex arguments' types, such as List\<Integer\>, but it's very stable for most interfaces I've tested. Generic Service refers to generic invoking.<br/>
**arguments type**: If Generic Service is chosen, arguments type is required here. The argument type should be the same as types in the method signature.(eg. "String" should be written as "java.lang.String"). Multiple arguments should be separated by commas. Arguments type can be left blank if you put your business jar into %JMETER_HOME%/lib/ext directory.<br/>
**timeout**: Set the test timeout.<br/>
**arguments**: Arguments basically writes in json syntax, numbers directly, string use quotes around, Collecyion type, array and complex objects writes in json string. Multiple arguments should be separated by commas.<br/>
example:<br/>
int 2<br/>
double 3.5<br/>
List\<String\> ["a","b","c"]<br/>
com.xxx.XXXvo {"string field":"String value", "number field":3}<br/>
boolean 1 or 0<br/>

Others:

1.beautify button is only used for beautifying single json string currently. If multiple arguments exists, a certain json string is required to be chosen.<br/>
2.Downloading and Uploading: If response contains a large byte array (normally exists in file downloading interface), this sampler will create a file under %JMETER_HOME%/tmp and replace the byte array with the absolute pass of the file. This sampler does NOT support file uploading tempprarily.
