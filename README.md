# Dubbo4Jmeter

这是一个Apache Jmeter(已适配5.2.1版本)使用的dubbo协议接口测试插件，可用于接口的功能测试，不建议用于压力测试。对于压力测试，使用Jmeter自带的java sampler更为合适。

什么是Jmeter: http://jmeter.apache.org/
<br/>
什么是Dubbo: http://dubbo.incubator.apache.org/

本插件可以像dubbo客户端一样工作。通常dubbo协议的客户端会结合服务端提供的业务jar包进行工作，但对于某些测试人员（比如我自己）来说，业务jar包的变化频率难或生命周期会对测试工作造成一些不必要的麻烦，因此我编写了这个插件用于脱离服务端的业务jar包来进行dubbo协议接口的功能测试。

使用方法：
1. 用 mvn install 命令编译并打出jar包。
2. 将打出的jar包 target/dubboSampler.jar 放到 %JMETER_HOME%/lib/ext 目录中。
3. 将 target/lib 目录中的以下jar包放入 %JMETER_HOME%/lib/ext 目录中。
avalon-framework-4.1.5.jar, dubbo-2.5.3.jar, gson-2.8.2.jar, javassist-3.4.ga.jar, netty-3.2.9.Final.jar
4. 启动Jmeter，添加DubboSampler到你的测试脚本中。
![dubbo sampler example](https://github.com/uniquetruth/Dubbo4Jmeter/blob/master/resource/ds.png)

关于dubbo采样器中的参数填写：

**ip and port**: 本工具采用直连的方式调用服务端，ip和端口处直接填写服务端注册到注册中心的ip和端口即可。如果你使用了dubbo自带的monitor插件，你可以很方便的在监视中心的页面上看到服务端的ip地址与端口。<br/>
**interface name**: 接口类的全名(包括包名).<br/>
**invoke method**: 调用的方法名称.<br/>
**implement**: 可选 Telnet Client 和 Generic Service 2个值。Telnet Client 表示使用dubbo本身提供的一个调试接口来交换数据。它对dubbo本身对这个调试接口的实现依赖较大，可能在处理一些复杂类型的入参时不太可靠，比如 List\<Integer\>，当然在我实际测试过的大多数接口上这种调用方式都是可靠且有效的。Generic Service也就是dubbo本身所支持的泛化调用模式。通常两种方式基本效果是一样的，具体差异请参考dubbo官网上的介绍。<br/>
**timeout**: 设置最大超时时间。默认超时时间为30秒，你也可以在jmeter.properties文件中增加dubbo.timeout参数来更改默认的超时时间。<br/>
**arguments**: 请求参数基本上可基于json语法书写，即数字直接写1、2、3，字符串用双引号包围，集合类型、数组类型及复杂的java对象类型用json语法书写，参数之间用英文逗号分隔。<br/>
几个例子:<br/>
int 2<br/>
double 3.5<br/>
List\<String\> ["a","b","c"]<br/>
com.xxx.XXXvo {"string field":"String value", "number field":3}<br/>
boolean 1 or 0<br/>
java.util.Date "1999-09-09T19:09:09"<br/>
此外当实现方式选择为 Generic Service 时，可在参数区域的format input面板中构造请求参数，该方式可以应对服务端提供的接口中存在重载方法，且参数个数相同，类型不同的情况。<br/>

其它说明：

1. 请求格式化按钮用于格式化单个json字符串，如果请求参数区域已填写了多个json对象的参数，需要选中单一的一个json串才能对其格式化。
2. 关于文件的上传与下载：如果测试的是下载接口（服务器返回一个文件）,本插件会在 %JMETER_HOME%/tmp 目录中生成文件，并在请求响应中用文件名代替实际的字节流。如果是上传接口，直接用文件路径的字符串作为参数即可，插件会在检测到字符串中有路径分隔符时尝试将该参数作为文件进行处理。(e.g. "D:\\\\foo\\\\bar.jpg") 
3. 如果你有服务端提供的业务jar包，将其放入 %JMETER_HOME%/lib/ext 目录后即可使用生成模板按钮，能根据你填写的interface name和invoke method生成请求参数模板。

=======================================================================================================================

A simple sampler for Apache Jmeter(v5.0+) which can test dubbo interface. Recommend for function tests only. For load tests, java sampler is more accurate.

What is Jmeter: http://jmeter.apache.org/
<br/>
What is Dubbo: http://dubbo.incubator.apache.org/

This sampler can be used as a dubbo client. Usually a dubbo client should work with a business jar provided by dubbo server, but for testers(like me), sometimes the business jars change in high frequency. So I specifically wrote this Jmeter plugin for generice invoking.

Usage:
1. Compile and pack with command "mvn install".
2. Put target/dubboSampler.jar into %JMETER_HOME%/lib/ext directory.
3. Put the following jar files in the target/lib into %JMETER_HOME%/lib/ext directory.
avalon-framework-4.1.5.jar, dubbo-2.5.3.jar, gson-2.8.2.jar, javassist-3.4.ga.jar, netty-3.2.9.Final.jar
4. Start Jmeter, add DubboSampler into your script.

How to fill the dubbo sampler's input areas?

**ip and port**: Whether use zookeeper or not, you can find ip and port in the service page of dubbo monitor center.<br/>
**interface name**: Full name of interface class(including package name).<br/>
**invoke method**: The method to be tested.<br/>
**implement**: Two choices: Telnet Client and Generic Service. Telnet Client means using the debugging interface provided by dubbo to exchange data. It relies very much on the implmentation of dubbo's debugging interface. It's unreliable when testing the method that includes some complex arguments' types, such as List\<Integer\>, but it's very stable for most interfaces I've tested. Generic Service refers to generic invoking.<br/>
**timeout**: Set the test timeout.<br/>
**arguments**: Arguments basically writes in json syntax, numbers directly, string use quotes around, Collecyion type, array and complex objects writes in json string. Multiple arguments should be separated by commas.<br/>
example:<br/>
int 2<br/>
double 3.5<br/>
List\<String\> ["a","b","c"]<br/>
com.xxx.XXXvo {"string field":"String value", "number field":3}<br/>
boolean 1 or 0<br/>
java.util.Date "1999-09-09T19:09:09"<br/>
If implement is setted to Generic Service, you can fill aguments in the format input panel, this is designed to fit the situation which dubbo server applies overload methods.

Others:

1.beautify button is only used for beautifying single json string currently. If multiple arguments exists, a certain json string is required to be chosen.<br/>
2.Downloading and Uploading: If response contains a large byte array (normally exists in file downloading interface), this sampler will create a file under %JMETER_HOME%/tmp and replace the byte array with the absolute pass of the file. If you're testing an uploading interface, use the file path string as the parameter. (e.g. "D:\\\\foo\\\\bar.jpg") <br/>
3. If you have business jar supplied by server side, put it into %JMETER_HOME%/lib/ext directory, so that you can click templete button to create a request templete according to the interface name and invoke method values.
