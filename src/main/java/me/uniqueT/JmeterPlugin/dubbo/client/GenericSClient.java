package me.uniqueT.JmeterPlugin.dubbo.client;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
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
import me.uniqueT.JmeterPlugin.dubbo.util.SortTool;

public class GenericSClient implements ClientCI {
	
	private static final String TMP_DIR = "tmp";
	private static int DEFAULT_TIMEOUT = 30000;
	private static int LAST_TIMEOUT=DEFAULT_TIMEOUT;
			
	private ReferenceConfig<GenericService> reference;
	private GenericService genericService;
	private ReferenceConfigCache cache;
	private String methodName;
	private String infName;
	private String arguments;
	private Object[] param;
	private String[] paramType;
	private int timeout = DEFAULT_TIMEOUT;

	//不真正进行连接，在send的时候再连接服务端
	public void init(String ip, int port) throws IOException {
		reference = new ReferenceConfig<GenericService>();
		reference.setGeneric(true);
		reference.setApplication(new ApplicationConfig("letmetest"));
		//在缓存内进行环境隔离
		//reference.setGroup(ip+"_"+port);
		String url = "dubbo://"+ip+":"+port;
		reference.setUrl(url);
	}

	public void build(String _infName, String method, String _arguments) throws ClassNotFoundException, IOException {
		methodName = method;
		infName = _infName;
		arguments = _arguments;
		reference.setInterface(infName);
		
		int argCount = buildParam(arguments);
		
		Class<?> c;
		c = Class.forName(infName);
		Method targetMethod = getAlmostMethod(c, method, argCount);
		//System.out.println("targetMethod is : "+targetMethod.toGenericString());
		
		@SuppressWarnings("rawtypes")
		Class[] cc = targetMethod.getParameterTypes();
		paramType = new String[cc.length];
		for(int i=0;i<cc.length;i++){
			paramType[i] = cc[i].getName();
		}
		
		setReferenceTimeout();
	}
	
	private void setReferenceTimeout(){
		//使用静态变量记录上一次的超时时间设置，当超时时间设置发生变化时清除缓存
		//虽然这样可能造成不必要的缓存性能损失，但这种情况出现的概率应该是比较小的
		if(timeout != LAST_TIMEOUT){
			ReferenceConfigCache.getCache().destroy(reference);
			LAST_TIMEOUT = timeout;
		}
		reference.setTimeout(timeout);
	}

	private Method getAlmostMethod(Class<?> c, String method, int argCount) {
		Method[] ms = c.getDeclaredMethods();
		Method targetMethod=null;
		for(Method m : ms){
			if(method.equals(m.getName()) && m.getParameterTypes().length==argCount){
				targetMethod = m;
				for(Class<?> pc : targetMethod.getParameterTypes()){
					if("com.ulic.im.client.vo.AppInfo".equals(pc.getName())){
						return targetMethod;
					}
				}
			}
		}
		return targetMethod;
	}
	
	public void gsBuild(String _infName, String method, String[][] args) throws IOException {
		methodName = method;
		infName = _infName;
		reference.setInterface(infName);
		
		paramType = new String[args.length];
		for(int i=0;i<args.length;i++){
			paramType[i] = args[i][0];
		}
		
		StringBuilder argStr = new StringBuilder();
		for(int i=0;i<args.length;i++){
			argStr.append(args[i][1]).append(",");
		}
		if(argStr.length() > 1)
			argStr.deleteCharAt(argStr.length()-1);
		
		buildParam(argStr.toString());
		
		setReferenceTimeout();
	}

	@SuppressWarnings("unchecked")
	public String send() throws IOException {
		
		cache = ReferenceConfigCache.getCache();
		genericService = cache.get(reference);
		//genericService = reference.get();
		Object result = genericService.$invoke(methodName, paramType, param);
		String resultStr=null;
		//处理返回值
		try{
			Gson gson = new Gson();
			//泛化调用的实际返回值是HashMap<String, Object>类型,或该类型的ArrayList数组
			if(result instanceof Map){
				Map<String, Object> m = (HashMap<String, Object>)result;
				clearClass(m);
				m = SortTool.sortMap(m);  //排序固定字段的顺序，便于断言
				resultStr = gson.toJson(m, Map.class);
			}else if(result instanceof Collection){
				List<Map<String, Object>> l = (ArrayList<Map<String, Object>>)result;
				for(Map<String, Object> m : l){
					clearClass(m);
				}
				l = SortTool.sortMapInList(l);
				resultStr = gson.toJson(l, ArrayList.class);
			}else if(result == null){
				resultStr = "null";
			}else if(result.getClass().isArray()){
				Object oarray[] = (Object[])result;
				StringBuilder sb1 = new StringBuilder("[");
				for(int x=0;x<oarray.length;x++){
					Map<String, Object> m1 = (HashMap<String, Object>)oarray[x];
					clearClass(m1);
					m1 = SortTool.sortMap(m1);
					sb1.append(gson.toJson(m1, Map.class)).append(",");
				}
				if(sb1.length() > 1)
					sb1.setLength(sb1.length()-1);
				return sb1.append("]").toString();
			}else
				resultStr = result.toString();
		}catch(ClassCastException e){
			resultStr = result.toString();
		}
		return resultStr;
	}

	@SuppressWarnings("unchecked")
	private void clearClass(Map<String, Object> m) throws IOException {
		if(m == null) return;
		m.remove("class");
		Iterator<Entry<String, Object>> iter = m.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> entry = iter.next();
			//System.out.println(entry);
			Object val = entry.getValue();
			
			try {
				if (val == null) {
					continue;
				} else if (val instanceof Collection) {
					for (HashMap<String, Object> submap : (Collection<HashMap<String, Object>>) val)
						clearClass(submap);
				} else if (val.getClass().isArray()) {
					if (val.getClass().getComponentType() == byte.class) {
						// 如果返回类型是字节数组，则当做文件处理
						entry.setValue(processFile(entry.getKey(), (byte[]) val));
					} else {
						for (Object o : (Object[]) val) {
							HashMap<String, Object> submap = (HashMap<String, Object>) o;
							clearClass(submap);
						}
					}
				} else if (val instanceof HashMap) {
					clearClass((HashMap<String, Object>) val);
				}
			} catch (ClassCastException e) {
				continue;
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
		return "df";
	}

	public void close() {
		//System.out.println("now closing the client");
		/*if(reference!=null)
			reference.destroy();*/
		//使用缓存cache后不用主动销毁reference对象，否则第二次调用会出错
		genericService = null;
	}

	public void setTime(int timesecond) {
		/*timeout = timesecond;
		if(reference!=null){
			System.out.println("Debug 置超时时间为："+timeout);
			reference.setTimeout(timeout);
		}*/
		/*MethodConfig config = new MethodConfig();
		methodConfigs*/
		timeout = timesecond;
	}
	
	//参数处理
	private int buildParam(String paraStr) throws IOException {
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
		/*for(String sa : listParam){
		System.out.println(sa);}*/
		param = new Object[listParam.size()];
		for(int i=0;i<listParam.size();i++){
			String s = listParam.get(i);
			if(s.startsWith("{")){  //参数如果是对象则返回json map
				param[i] = getMapFromJson(s);
			}else if(s.startsWith("[")){
				param[i] = getListFromPara(s);
			}else if(s.startsWith("\"")){  //参数如果是字符串则返回字符串
				String strValue = s.substring(1, s.length()-1);
				if(strValue.contains(File.separator) && (new File(strValue)).exists()){
					param[i] = getFileBytes(strValue);
				}else{
					param[i] = strValue;
				}
			}else if(s.startsWith("null")){
				param[i] = null;
			}else{  //参数既不是对象也不是字符串则当int处理
				param[i] = Integer.valueOf(s).intValue();
			}
		}
		return listParam.size();
	}
	
	private byte[] getFileBytes(String filepath) throws IOException {
		File f = new File(filepath);
		FileInputStream fis = null;
        ByteArrayOutputStream bos = null;
        byte[] buffer = null;
        //System.out.println("before get file"+filepath);
        try {
            fis = new FileInputStream(f);
            bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            buffer = bos.toByteArray();
        } catch (IOException ex) {
        	throw ex;
        } finally {
        	try {
        		if(fis!=null)
					fis.close();
        		if(bos!=null)
        			bos.close();
			} catch (IOException e) {
				throw e;
			}
        }
		return buffer;
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

	private ArrayList<Object> getListFromPara(String s) throws IOException {
		ArrayList<Object> l = new ArrayList<Object>();
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
		if(s.contains(File.separator)){
			l = setFilePara(l);
		}
		return l;
	}

	private Map<String, Object> getMapFromJson(String s) throws IOException {
		Gson gson = new GsonBuilder().enableComplexMapKeySerialization().create();
		Type type = new TypeToken<Map<String, Object>>() {}.getType(); 
		Map<String, Object> m = gson.fromJson(s, type);
		if(s.contains(File.separator)){
			m = setFilePara(m);
		}
		return m;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> setFilePara(Map<String, Object> map) throws IOException {
		Iterator<Entry<String, Object>> it = map.entrySet().iterator();
        while(it.hasNext()){
            Entry<String, Object> itEntry = it.next();
            Object itValue = itEntry.getValue();
            if(itValue instanceof String){
            	String s = (String)itValue;
            	if(s.contains(File.separator) && (new File(s)).exists()){
            		itEntry.setValue(getFileBytes(s));
            	}
            }else if(itValue instanceof ArrayList<?>){
            	itEntry.setValue(setFilePara((ArrayList<Object>) itValue));
            }else if(itValue instanceof Map<?, ?>){
            	itEntry.setValue(setFilePara((Map<String, Object>)itValue));
            } 
        }
        return map;
	}

	@SuppressWarnings("unchecked")
	private ArrayList<Object> setFilePara(ArrayList<Object> list) throws IOException {
		Object[] olist = list.toArray();
		for(int i=0;i<list.size();i++){
    		if(olist[i] instanceof String){
    			String s = (String)olist[i];
    			if(s.contains(File.separator) && (new File(s)).exists()){
            		olist[i] = getFileBytes(s);
    			}
            }else if(olist[i] instanceof ArrayList<?>){
            	olist[i] = setFilePara((ArrayList<Object>)olist[i]);
            }else if(olist[i] instanceof Map<?, ?>){
            	olist[i] = setFilePara((Map<String, Object>)olist[i]);
            }
    	}
		ArrayList<Object> returnList = new ArrayList<Object>();
		for(int i=0;i<list.size();i++){
			returnList.add(olist[i]);
		}
		return returnList;		
	}

}
