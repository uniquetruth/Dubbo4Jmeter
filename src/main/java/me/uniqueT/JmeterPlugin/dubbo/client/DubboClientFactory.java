package me.uniqueT.JmeterPlugin.dubbo.client;

public class DubboClientFactory {
	
	public static ClientCI getDubboClient(String impName){
		ClientCI c = null;
		if("Telnet Client".equals(impName))
			c = new DubboTClient();
		else if("Generic Service".equals(impName))
			c = new GenericSClient();
		else
			throw new IllegalArgumentException("invalid implement :\"" + impName + "\"");
		return c;
	}

}
