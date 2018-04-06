package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.utils.ReferenceConfigCache;
import com.alibaba.dubbo.rpc.service.GenericService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class GenericSClient implements ClientCI {
	
	private static final String TMP_DIR = ".." + File.separator + "tmp";
			
	private ReferenceConfig<GenericService> reference;
	private GenericService genericService;
	private ReferenceConfigCache cache;
	private String methodName;
	private String infName;
	private int timeout=30000;
	private String argument;
	private Object[] param;
	private String[] paramType;

	//不真正进行连接，在send的时候再连接服务端
	public void init(String ip, int port) throws IOException {
		reference = new ReferenceConfig<GenericService>();
		reference.setGeneric(true);
		reference.setApplication(new ApplicationConfig("letmetest"));
		reference.setTimeout(timeout);
		String url = "dubbo://"+ip+":"+port;
		reference.setUrl(url);
	}

	public void build(String _infName, String method, String _argument) throws ClassNotFoundException {
		methodName = method;
		infName = _infName;
		argument = _argument;
		reference.setInterface(infName);
		
		int argCount = buildParam(argument);
		
		Class<?> c;
		c = Class.forName(infName);
		Method targetMethod = getAlmostMethod(c, method, argCount);
		
		@SuppressWarnings("rawtypes")
		Class[] cc = targetMethod.getParameterTypes();
		paramType = new String[cc.length];
		for(int i=0;i<cc.length;i++){
			paramType[i] = cc[i].getName();
		}
	}
	
	public void gsBuild(String _infName, String method, String _argument, String argsType) throws ClassNotFoundException {
		methodName = method;
		infName = _infName;
		argument = _argument;
		reference.setInterface(infName);
		
		buildParam(argument);
		
		String ats[] = argsType.split(",");
		paramType = new String[ats.length];
		int i=0;
		for(String at : ats){
			paramType[i] = at.trim();
			i++;
		}
	}

	private Method getAlmostMethod(Class<?> c, String method, int argCount) {
		Method[] ms = c.getDeclaredMethods();
		Method targetMethod=null;
		for(Method m : ms){
			if(method.equals(m.getName())){
				targetMethod = m;
				if(targetMethod.getParameterTypes().length == argCount){
					return targetMethod;
				}
			}
		}
		return targetMethod;
	}

	@SuppressWarnings("unchecked")
	public String send() throws IOException {
		
		cache = ReferenceConfigCache.getCache();
		genericService = cache.get(reference);
		
		//genericService = reference.get();
		Object result = genericService.$invoke(methodName, paramType, param);
		Gson gson = new Gson();
		//泛化调用的实际返回值是HashMap<String, Object>类型,或该类型的ArrayList数组
		if(result instanceof Map){
			//System.out.println("map");
			HashMap<String, Object> m = (HashMap<String, Object>)result;
			clearClass(m);
			return gson.toJson(m, HashMap.class);
		}else if(result instanceof Collection){
			//System.out.println("colletion");
			for(HashMap<String, Object> m : (Collection<HashMap<String, Object>>)result){
				clearClass(m);
			}
			return gson.toJson(result, ArrayList.class);
		}else
			return result.toString();
	}

	@SuppressWarnings("unchecked")
	private void clearClass(HashMap<String, Object> m) throws IOException {
		m.remove("class");
		Iterator<Entry<String, Object>> iter = m.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> entry = (Entry<String, Object>) iter.next();
			//System.out.println(entry);
			Object val = entry.getValue();
			if(val==null){
				continue;
			}else if(val instanceof Collection){
				for(HashMap<String, Object> submap : (Collection<HashMap<String, Object>>)val)
					clearClass(submap);
			}else if(val.getClass().isArray()){
				if(val.getClass().getComponentType() == byte.class){
					//如果返回类型是字节数组，则当做文件处理
					entry.setValue(processFile(entry.getKey(), (byte[])val));
				}else{
					for(Object o : (Object[])val){
						HashMap<String, Object> submap = (HashMap<String, Object>)o;
						clearClass(submap);
					}
				}
			}else if(val instanceof HashMap){
				clearClass((HashMap<String, Object>)val);
			}
		}
		
	}
	
	private String processFile(String key, byte[] val) throws IOException {
		File tmpdir = null;
		FileOutputStream fos = null;
		String rear = getAlmostFileRear(key);
		String dirPath = new StringBuilder(TMP_DIR).append(File.separator)
				.append(infName.substring(infName.lastIndexOf(".")+1))
				.append(File.separator).append(methodName).toString();
		String filePath = new StringBuilder(dirPath).append(File.separator).append(key)
				.append("_").append(System.currentTimeMillis()).append(".")
				.append(rear).toString();
		tmpdir = new File(dirPath);
		if(!tmpdir.exists())
			tmpdir.mkdirs();
		File f = new File(filePath);
		try {
			fos = new FileOutputStream(f);
			fos.write(val);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if(fos != null)
					fos.close();
			} catch (IOException e) {
				throw e;
			}
		}
		return f.getAbsolutePath().replaceAll("\\\\", "/");
	}

	//返回一个可能合理的文件后缀
	private String getAlmostFileRear(String key) {
		if(key.contains("pdf")||key.contains("PDF"))
			return "pdf";
		else if(key.contains("jpg")||key.contains("JPG"))
			return "jpg";
		else if(key.contains("tif")||key.contains("TIF"))
			return "tif";
		else
			return "df";
	}

	public void close() {
		//System.out.println("now closing the client");
		/*if(reference!=null)
			reference.destroy();*/
		//使用缓存cache后不用主动销毁reference对象，否则第二次调用会出错
		genericService = null;
	}

	public void setTimeout(int timesecond) {
		timeout = timesecond;
	}
	
	//参数处理
	private int buildParam(String paraStr) {
		if(paraStr==null){
			param = new Object[0];
			return 0;
		}
		String para = null;
		int end = 0;
		LinkedList<String> listParam = new LinkedList<String>();
		//有多个参数的情况
		while((end = paraStr.indexOf(",")) > 0){ //以逗号为参数分隔符，但不保证出现逗号一定是参数分隔符，因此需要后面的判断
			//if first parameter is an Object
			if(paraStr.startsWith("{")){
				para = getParaBySpliter(paraStr, '{', '}');
				listParam.add(para);
				paraStr = paraStr.substring(para.length());
			//if first parameter is an Array
			}else if(paraStr.startsWith("[")){
				para = getParaBySpliter(paraStr, '[', ']');
				listParam.add(para);
				paraStr = paraStr.substring(para.length());
			}else{  //其余情况暂以字符串处理
				listParam.add(paraStr.substring(0,end).trim());
			}
			//去掉处理过的部分继续循环
			paraStr = paraStr.substring(paraStr.indexOf(",")+1).trim();
		}
		//处理最后一个参数，如果有的话
		if(paraStr.length() > 0)
			listParam.add(paraStr);
		
		param = new Object[listParam.size()];
		for(int i=0;i<listParam.size();i++){
			String s = listParam.get(i);
			if(s.startsWith("{")){  //参数如果是对象则返回json map
				param[i] = getMapFromJson(s);
			}else if(s.startsWith("[")){
				param[i] = getListFromPara(s);
			}else if(s.startsWith("\"")){  //参数如果是字符串则返回字符串
				param[i] = s.substring(1, s.length()-1);
			}else{  //参数既不是对象也不是字符串则当int处理
				param[i] = Integer.valueOf(s).intValue();
			}
		}
		return param.length;
	}
	
	private String getParaBySpliter(String paraStr, char left, char right) {
		StringBuilder sb = new StringBuilder("");
		char c;
		int stack = 0;
		int i=0;
		int length = paraStr.length();
		while(i < length){
			c = paraStr.charAt(i);
			if(c == left){
				stack++;
			}else if(c == right){
				stack--;
			}
			sb.append(c);
			i++;
			if(stack == 0)
				break;
		}
		if(stack > 0)  //字符串不合json规范
			throw new IllegalArgumentException(paraStr + " is not a legal json string");
		else
			return sb.toString();
	}

	private List<Object> getListFromPara(String s) {
		List<Object> l = new ArrayList<Object>();
		String[] strarray;
		//Json对象数组
		if(s.contains("{")){
			Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
			Type type = new TypeToken<List<Map<String, Object>>>() {}.getType(); 
			l = gson.fromJson(s, type);
		//字符串数组
		}else if(s.contains("\"")){
			strarray=s.substring(1, s.length()-1).split(",");
			for(String str : strarray){
				l.add(str.substring(1, str.length()-1));
			}
		//数字数组
		}else{
			strarray=s.substring(1, s.length()-1).split(",");
			for(String str : strarray){
				l.add(Integer.parseInt(str));
			}
		}
		return l;
	}

	private Map<String, Object> getMapFromJson(String s) {
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		Type type = new TypeToken<Map<String, Object>>() {}.getType(); 
		Map<String, Object> m = gson.fromJson(s, type);
		return m;
	}

}
