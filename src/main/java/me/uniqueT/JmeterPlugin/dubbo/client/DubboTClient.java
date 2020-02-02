package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.net.telnet.TelnetClient;

import com.google.gson.Gson;
import me.uniqueT.JmeterPlugin.dubbo.util.SortTool;

public class DubboTClient implements ClientCI {
	private TelnetClient telnetClient;
	private InputStream inputStream;
	private OutputStream outputStream;
	private String arguments;
	private int timeout = 30000;
	private String method;
	private final long HundredKB = 100 * 1024L;

	private String executeCmd(String cmd) {
		if (null == telnetClient || null == inputStream || null == outputStream) {
			throw new IllegalArgumentException("请先 建立连接 或建立连接失败");
		}
		Pattern pattern = Pattern.compile("dubbo>");  //命令提示符为dubbo>
		StringBuilder text = new StringBuilder();
		try {
			long returnLenth=0;
			outputStream.write((cmd + "\n").getBytes("GBK"));  //telnet方式的交互编码为GBK
			outputStream.flush();
			StringBuilder sb = new StringBuilder();
			//long startTime = System.currentTimeMillis();
			int i = -1;
			//while (System.currentTimeMillis() - startTime < timeout) {
			while ((i = inputStream.read()) > -1) {
				if (i == -1) {
					throw new IllegalArgumentException("接收不到消息");
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
					return "返回内容过长，可能包含文件，请尝试使用Generic Service方式发送请求。dubbo>";
				}
			}
			//}
			throw new IllegalArgumentException("超时收不到提示符");
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
	public void setTime(int timesecond) {
		timeout = timesecond;
	}

	@Override
	public void init(String _hostIp, int _port) throws IOException {
		telnetClient = new TelnetClient();
		telnetClient.connect(_hostIp, _port);
		telnetClient.setSoTimeout(timeout);
		inputStream = telnetClient.getInputStream();
		outputStream = telnetClient.getOutputStream();
	}

	@Override
	public void build(String _infName, String _method, String _arguments) throws ClassNotFoundException {
		method = _method;
		arguments = _arguments;
		String cmd = "cd " + _infName;
		executeCmd(cmd);
	}

	@Override
	public String send() throws IOException {
		StringBuffer sb = new StringBuffer("invoke ").append(method).append("(");
		sb.append(arguments).append(")");
		//System.out.println("telnet debug request +++ "+sb);
		String rawString = executeCmd(sb.toString());
		//System.out.println("telnet debug response +++ "+rawString);
		String dataString;
		if(rawString.startsWith("Use default service")){
			dataString = rawString.split("\n")[1];
		}else
			dataString = rawString;
		
		return sortString(dataString);
	}

	@SuppressWarnings("unchecked")
	private String sortString(String dataString) {
		Gson gson;
		String resultStr=dataString;
		try {
			if (dataString.startsWith("{")) {
				gson = new Gson();
				Map<String, Object> m = gson.fromJson(dataString, Map.class);
				m = SortTool.sortMap(m);
				resultStr = gson.toJson(m, Map.class);
			} else if (dataString.startsWith("[") && dataString.indexOf("{") > 0) {
				gson = new Gson();
				List<Map<String, Object>> l = gson.fromJson(dataString, ArrayList.class);
				l = SortTool.sortMapInList(l);
				resultStr = gson.toJson(l, ArrayList.class);
			}
		} catch (ClassCastException e) {
			return dataString;
		}
		return resultStr;
	}

}
