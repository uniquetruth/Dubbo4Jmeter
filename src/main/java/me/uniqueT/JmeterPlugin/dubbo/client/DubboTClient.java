package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import org.apache.commons.net.telnet.TelnetClient;

public class DubboTClient implements ClientCI {
	private TelnetClient telnetClient;
	private InputStream inputStream;
	private OutputStream outputStream;
	private int timeout = 30000;
	private String method;
	private String argument;
	private final long HundredKB = 100 * 1024L;

	private String executeCmd(String cmd) {
		if (null == telnetClient || null == inputStream || null == outputStream) {
			throw new IllegalArgumentException("please establish connection first or connection failed");
		}
		Pattern pattern = Pattern.compile("dubbo>");  //命令提示符为dubbo>
		StringBuilder text = new StringBuilder();
		try {
			long returnLenth=0;
			outputStream.write((cmd + "\n").getBytes("GBK"));  //telnet方式的交互编码为GBK
			outputStream.flush();
			StringBuilder sb = new StringBuilder();
			long startTime = System.currentTimeMillis();
			int i = -1;
			while (System.currentTimeMillis() - startTime < timeout) {
				while ((i = inputStream.read()) > -1) {
					if (i == -1) {
						throw new IllegalArgumentException("response error");
					}
					char ch = (char) i;
					text.append(ch);
					if (ch == '\n' || ch == '\r') {
						sb.delete(0, sb.length());
						continue;
					}
					sb.append(ch);
					if (pattern.matcher(sb.toString()).find()) {  //返回字符流中找到了命令提示符
						//telnet方式的交互编码为GBK，先从StringBuilder中将每个字节还原出来（.getBytes("ISO-8859-1")）
						//再将字节序列按GBK编码方式表示成字符
						return new String(text.toString().getBytes("ISO-8859-1"), "GBK");
					}
					returnLenth++;
					if(returnLenth > HundredKB){
						return "response content is too large, there may be a file in the response, please try Generic Service implement.dubbo>";
					}
				}
			}
			throw new IllegalArgumentException("timeout while waiting for response");
		} catch (IOException e) {
			return e.toString();
		}
	}

	public void close() {
		if (telnetClient != null) {
			try {
				telnetClient.disconnect();
			} catch (IOException e) {
				e.printStackTrace(System.err);
			}
		}
	}

	/**
	 * 设置超时时间，默认为30秒
	 * 
	 * @param timesecond
	 *            - 单位毫秒
	 */
	public void setTimeout(int timesecond) {
		timeout = timesecond;
	}

	public void init(String _hostIp, int _port) throws IOException {
		telnetClient = new TelnetClient();
		telnetClient.connect(_hostIp, _port);
		inputStream = telnetClient.getInputStream();
		outputStream = telnetClient.getOutputStream();
	}

	public void build(String _infName, String _method, String _argument) throws ClassNotFoundException {
		method = _method;
		argument = _argument;
		String cmd = "cd " + _infName;
		executeCmd(cmd);
	}

	public String send() throws IOException {
		StringBuffer sb = new StringBuffer("invoke ").append(method).append("(");
		sb.append(argument).append(")");
		//System.out.println("telnet debug request +++ "+sb);
		String rawString = executeCmd(sb.toString());
		//System.out.println("telnet debug response +++ "+rawString);
		if(rawString.startsWith("Use default service")){
			return rawString.split("\n")[1];
		}else
			return rawString.substring(0, rawString.lastIndexOf("dubbo>"));
	}

}
