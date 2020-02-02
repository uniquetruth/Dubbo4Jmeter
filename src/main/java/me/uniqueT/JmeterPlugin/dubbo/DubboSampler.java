package me.uniqueT.JmeterPlugin.dubbo;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.util.JMeterUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.uniqueT.JmeterPlugin.dubbo.client.ClientCI;
import me.uniqueT.JmeterPlugin.dubbo.client.DubboClientFactory;
import me.uniqueT.JmeterPlugin.dubbo.client.GenericSClient;
import me.uniqueT.JmeterPlugin.dubbo.util.JsonFormatTool;

public class DubboSampler extends AbstractSampler {
	
	private static final String IP="ip";
	private static final String PORT="port";
	private static final String INF_NAME="infName";
	private static final String INVOKE_CMD="invokeCMD";
	private static final String METHOD="method";
	private static final String IMP_CLASS="impClass";
	private static final String ARG_TABLE="argTable";
	private static final String ARG_TYPE="argType";
	private static final String TIMEOUT="timeout";
	
	//支持-J参数指定的默认超时时间
	private static int sysTimeout = JMeterUtils.getPropDefault("dubbo.timeout", 30000);

	/**
	 * 
	 */
	private static final long serialVersionUID = 2884657011121298989L;

	//采样器执行的主方法
	public SampleResult sample(Entry arg0) {
		SampleResult result = new SampleResult();
        result.setSampleLabel(getName());
        ClientCI client = null;
        String res = "";
        try {
            //System.out.println(impClass);
    		client = DubboClientFactory.getDubboClient(getImpClass());
    		result.setSamplerData(getSamplerData());
    		result.sampleStart();
    		if(getTimeout()!=null && !"".equals(getTimeout())){
    			client.setTime(Integer.parseInt(getTimeout()));
    		}else{
    			//System.out.println("sysTimeout is "+sysTimeout);
    			client.setTime(sysTimeout);
    		}
    		client.init(getIp(), Integer.valueOf(getPort()));
    		if(getArgType()==0){
    			client.build(getInfName(), getMethod(), getInvokeCMD().replace("\n", ""));
    		}else{
				((GenericSClient)client).gsBuild(getInfName(), getMethod(), getArgTable());
			}
    		res = client.send();
            result.sampleEnd();
    		result.setDataType(SampleResult.TEXT);
            result.setSuccessful(true);
            result.setResponseCodeOK();
            
            //下面这段程序可能抛出JsonSyntaxException，但属于执行成功的逻辑
            Gson gson = new Gson();
			JsonParser jp = new JsonParser();
			JsonFormatTool jft = new JsonFormatTool();
			JsonElement je = jp.parse(res);
			String beautyJSON = gson.toJson(je);
			beautyJSON = jft.formatJson(beautyJSON);
			
			result.setResponseData(beautyJSON, null);
        }catch (JsonSyntaxException je){
            result.setResponseData(res, null);
        } catch (Exception e) {
            result.sampleEnd(); // stop stopwatch
            result.setSuccessful(false);
            result.setResponseMessage("Exception: " + e);
            // get stack trace as a String to return as document data
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString(), null);
            result.setDataType(SampleResult.TEXT);
            result.setResponseCode("FAILED");
        }finally{
			if(client!=null)
				client.close();
		}
        return result;
	}
	
	private String getSamplerData() {
		StringBuilder s = new StringBuilder();
		s.append("ip : ").append(getIp()).append("\nport : ").append(getPort())
				.append("\ninterface name : ").append(getInfName())
				.append("\nmethod : ").append(getMethod());
		if(getArgType()==0){
			s.append("\nargument : ").append(getInvokeCMD());
		}else{
			s.append("\nargument : ").append(getTableDataString());
		}
		s.append("\nimplment : ").append(getImpClass());
		return s.toString();
	}

	public String getInvokeCMD() {
		return this.getPropertyAsString(INVOKE_CMD);
	}

	public void setInvokeCMD(String invokeCMD) {
		this.setProperty(INVOKE_CMD, invokeCMD);
	}

	public String getInfName() {
		return this.getPropertyAsString(INF_NAME);
	}

	public void setInfName(String infName) {
		this.setProperty(INF_NAME, infName);
	}

	public String getPort() {
		return this.getPropertyAsString(PORT);
	}

	public void setPort(String _port) {
		this.setProperty(PORT, _port);
	}

	public String getIp() {
		return this.getPropertyAsString(IP);
	}

	public void setIp(String ip) {
		this.setProperty(IP, ip);
	}

	public String getImpClass() {
		return this.getPropertyAsString(IMP_CLASS);
	}

	public void setImpClass(String impClass) {
		this.setProperty(IMP_CLASS, impClass);
	}

	public String getMethod() {
		return this.getPropertyAsString(METHOD);
	}

	public void setMethod(String method) {
		this.setProperty(METHOD, method);
	}
	
	//由于jmx文档不能存储数组结构，使用LinkedList<LinkedList<String>>类型来实现二维数组效果
	public String[][] getArgTable() {
		String rawStr = this.getPropertyAsString(ARG_TABLE);
		if(rawStr==null || "".equals(rawStr))
			return new String[0][0];
		String[] str = rawStr.split(",,");
		String[][] result = new String[str.length][2];
		int i=0;
		for(String s : str){
			String[] paramMap = s.split("##");
			result[i][0] = paramMap[0];
			result[i][1] = paramMap[1];
			i++;
		}
		return result;
		
		//String rawStr = this.getPropertyAsString(ARG_TABLE);
		//if(p.getObjectValue()!=null && p.getObjectValue() instanceof String[][]){
		//	return (String[][]) p.getObjectValue();
		//}else{
		//return new String[0][0];
		//}
		
	}
	
	private String getTableDataString(){
		String rawStr = this.getPropertyAsString(ARG_TABLE);
		if(rawStr==null || "".equals(rawStr))
			return null;
		String[] str = rawStr.split(",,");
		StringBuilder sb = new StringBuilder();
		for(String s : str){
			String[] paramMap = s.split("##");
			sb.append(paramMap[0]).append(":").append(paramMap[1]);
		}
		return sb.toString();
	}

	public void setArgTable(String[][] argTable) {
		/*JMeterProperty p = new ObjectProperty(ARG_TABLE, argTable);
		this.setProperty(p);*/
		StringBuilder sb = new StringBuilder("");
		for(String[] row : argTable){
			sb.append(row[0]).append("##").append(row[1]).append(",,");
		}
		if(sb.length() > 0)
			sb.setLength(sb.length()-2);
		this.setProperty(ARG_TABLE, sb.toString());
	}
	
	public int getArgType() {
		return this.getPropertyAsInt(ARG_TYPE);
	}

	public void setArgType(int argType) {
		this.setProperty(ARG_TYPE, argType);
	}
	
	public String getTimeout(){
		return this.getPropertyAsString(TIMEOUT);
	}
	
	public void setTimeout(String time_out) {
		this.setProperty(TIMEOUT, time_out);
	}

}
