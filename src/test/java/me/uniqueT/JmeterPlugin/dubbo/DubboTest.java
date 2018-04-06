package me.uniqueT.JmeterPlugin.dubbo;

import java.io.IOException;
import java.net.SocketException;

import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

import junit.framework.TestCase;

public class DubboTest extends TestCase{
	
	public void testTelnetClient() throws SocketException, IOException, ClassNotFoundException {
		/*JMeterContext context = JMeterContextService.getContext();
		JMeterVariables vars = new JMeterVariables();
		vars.put("ip", "127.0.0.1");
		vars.put("port", "20880");
		vars.put("infName", "me.uniqueT.dubbo.server.service.HelloDubbo");
		vars.put("method", "sayHello");
		vars.put("invokeCMD", "\"Candy\"");
		vars.put("impClass", "Telnet Client");
		context.setVariables(vars);
		DubboSampler ds = new DubboSampler();
		context.setCurrentSampler(ds);
		context.getThread().run();
		SampleResult res = context.getPreviousResult();
		//System.out.println(res);
		assertFalse(res.getSamplerData().contains("Exception"));*/
	}

}
