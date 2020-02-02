package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.IOException;

public interface ClientCI {
	
	/**
	 * 初始化
	 * @param ip
	 * @param port
	 * @throws IOException
	 */
	public void init(String ip, int port) throws IOException;
	
	/**
	 * 设置接口名和方法名
	 * @param infName
	 * @param method
	 * @param arguments
	 * @throws ClassNotFoundException
	 */
	public void build(String infName, String method, String arguments) throws ClassNotFoundException, IOException;
	
	/**
	 * 发消息与服务器进行交互
	 * @throws IOException
	 * @return
	 */
	public String send()  throws IOException;
	
	/**
	 * 断开连接或其它finalize处理
	 */
	public void close();
	
	/**
	 * 设置调用的最大超时时间
	 * @param timesecond
	 */
	public void setTime(int timesecond);

}
