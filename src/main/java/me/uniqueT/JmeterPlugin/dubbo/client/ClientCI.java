package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.IOException;

public interface ClientCI {
	
	/**
	 * set ip and port
	 * @param ip
	 * @param port
	 * @throws IOException
	 */
	public void init(String ip, int port) throws IOException;
	
	/**
	 * set interface name, invoke method and arguments
	 * @param infName
	 * @param method
	 * @param args
	 * @throws ClassNotFoundException
	 */
	public void build(String infName, String method, String args) throws ClassNotFoundException;
	
	/**
	 * send request to the server and get response. called after init() and build()
	 * @throws IOException
	 * @return
	 */
	public String send()  throws IOException;
	
	/**
	 * disconnect or other finalize process
	 */
	public void close();
	
	/**
	 * set the timeout milliseconds
	 * @param milliseconds
	 */
	public void setTimeout(int milliseconds);

}
