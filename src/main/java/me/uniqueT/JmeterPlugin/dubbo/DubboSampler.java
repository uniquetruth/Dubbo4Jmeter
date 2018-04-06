package me.uniqueT.JmeterPlugin.dubbo;

import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.uniqueT.JmeterPlugin.dubbo.client.ClientCI;
import me.uniqueT.JmeterPlugin.dubbo.client.DubboClientFactory;
import me.uniqueT.JmeterPlugin.dubbo.client.GenericSClient;

public class DubboSampler extends AbstractSampler {
	
	private static final String IP="ip";
	private static final String PORT="port";
	private static final String INF_NAME="infName";
	private static final String INVOKE_CMD="invokeCMD";
	private static final String METHOD="method";
	private static final String IMP_CLASS="impClass";
	private static final String ARGS_TYPE="argsType";
	private static final String TIMEOUT="timeout";

	/**
	 * 
	 */
	private static final long serialVersionUID = 2884657011121298989L;

	public SampleResult sample(Entry arg0) {
		SampleResult result = new SampleResult();
        result.setSampleLabel(getName());
        ClientCI client = null;
        String res = null;
        try {
            //System.out.println(impClass);
    		client = DubboClientFactory.getDubboClient(getImpClass());
    		if(!"".equals(getTimeout())){
    			client.setTimeout(Integer.valueOf(getTimeout()));
    		}
    		
    		client.init(getIp(), Integer.valueOf(getPort()));
    		
    		if(client instanceof GenericSClient && !"".equals(getArgsType())){
    			((GenericSClient)client).gsBuild(getInfName(), getMethod(), getInvokeCMD().replace("\n", ""), getArgsType());
    		}else{
    			client.build(getInfName(), getMethod(), getInvokeCMD().replace("\n", ""));
    		}
    		result.setSamplerData(getSamplerData());
    		result.sampleStart();
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
		s.append("ip:").append(getIp()).append("\nport:").append(getPort()).append("\nimplement:").append(getImpClass())
			.append("\nmethod:").append(getMethod()).append("\nargement:").append(getInvokeCMD());
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
	
	public String getArgsType() {
		return this.getPropertyAsString(ARGS_TYPE);
	}
	
	public void setArgsType(String argsType) {
		this.setProperty(ARGS_TYPE, argsType);
	}
	
	public String getTimeout() {
		return this.getPropertyAsString(TIMEOUT);
	}
	
	public void setTimeout(String timeout) {
		this.setProperty(TIMEOUT, timeout);
	}

}
