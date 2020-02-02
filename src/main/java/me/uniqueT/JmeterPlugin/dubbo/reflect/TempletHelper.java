package me.uniqueT.JmeterPlugin.dubbo.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Vector;

import org.apache.jmeter.util.JMeterUtils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import me.uniqueT.JmeterPlugin.dubbo.util.JsonFormatTool;

public class TempletHelper {
	
	//生成模板时是否考虑参数的父类中的字段
	private static String deepReflect = JMeterUtils.getPropDefault("dubbo.deepReflect", "false");
	private Class<?> c;
	private String method;
	
	public TempletHelper(Class<?> _c, String _method){
		c = _c;
		method = _method;
	}
	
	public TempletHelper(String infName, String _method) throws ClassNotFoundException{
		c = Class.forName(infName);
		method = _method;
	}

	/**
	 * 根据接口类名和方法名称，获取参数模板
	 * @param 接口类名 - infName
	 * @param 方法名称 - method
	 * @return 参数模板字符串
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	@SuppressWarnings("rawtypes")
	public String getTemplet() throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		//logger.info("start to get Templet.");
		Method targetMethod = null;
		StringBuilder s = new StringBuilder("");
		targetMethod = getAlmostMethod();
		//logger.info("get method successfully, method is :"+targetMethod.toGenericString());
		Class[] cc = targetMethod.getParameterTypes();
		int i=1;
		for(Class parac : cc){
			String cname = parac.getName();
			if(cname.startsWith("com.")){
				s.append(parseObj(parac)).append(",");
			/*}else if(isSubClass(parac, Map.class)){
				s.append(parseMap(parac, i, null));*/
			}else if(parac.isArray() || isSubClass(parac, Collection.class)){
				s.append(parseArray(parac, i, null)).append(",");
			}else if(cname.contains("String")){
				s.append("\"String\",");
				//如果编译时启用调试信息（javac -g），则可以使用jad反编译获得更好的反射效果
				//s.append("\"").append(getArgName(i)).append("\",");  
			}else{
				s.append(cname).append(",");
				//s.append(cname).append("_").append(getArgName(i)).append(",");
			}
			s.append("\n");
			i++;
		}
		String result = s.toString();
		return result.substring(0, result.length()-2);
	}
	
	//获得形参名称
	/*private String getArgName(int i) {
		return JarReflectUtil.getArgName(c, method, i);
	}*/

	private boolean isSubClass( Class<?> c, Class<?> targetC) {
		try{
			c.asSubclass(targetC);
		}catch(ClassCastException ce){
			return false;
		}
		return true;
	}

	/**
	 * list和数组都可以在json中表示为数组<p>
	 * 有两种情况会调用本方法，1、处理方法参数，此时传入index指明参数顺序,忽略fieldName；<p>
	 * 2、处理对象的成员变量，此时传入index为-1，且fieldName为成员变量名
	 * @param parac
	 * @param index
	 * @param fieldName
	 * @return 数组或list解析后的json字符串
	 * @throws ClassNotFoundException 
	 * @throws SecurityException 
	 * @throws NoSuchFieldException 
	 */
	private String parseArray(Class<?> parac, int index, String fieldName) throws ClassNotFoundException, NoSuchFieldException, SecurityException{
		StringBuilder s = new StringBuilder("[");
		if(index > 0 && parac.isArray()){  //方法中的参数为数组类型
			s.append(parseObj(parac.getComponentType()));
		}else if(index <= 0 && parac.getDeclaredField(fieldName).getType().isArray()){ //类中的字段为数组类型
			s.append(parseObj(parac.getDeclaredField(fieldName).getType().getComponentType()));
		}else{  //泛型（容器类型）处理
			Class<?> genericType = null;
			String gTypeName = null;
			try{
				if(index > 0){  //获取方法中某个泛型参数的具体类型
					//logger.info("start to get generic type from method :" + method);
					//logger.info("argument index is :" + index);
					genericType = JarReflectUtil.getGenericType(c, method, index);
				}else{  //获取一个类的某个泛型成员变量的具体类型
					//logger.info("start to get generic type from class :" + parac.getName());
					//logger.info("the field is :"+fieldName);
					genericType = JarReflectUtil.getGenericType(parac, fieldName);
				}
				gTypeName = genericType.getName();
				//logger.info("get the generic type is :" + gTypeName);
				if(gTypeName.startsWith("com.")){
					s.append(parseObj(genericType)).append(",");
				/*}else if(isSubClass(genericType, Map.class)){
					s.append(parseMap(parac, gTypeName));*/
				}else if(genericType.isArray()){ //不考虑泛型嵌套的复杂情况
					//logger.fine("test if it is a generic type +++++ " + gTypeName);
					s.append(parseArray(genericType, -1, gTypeName)).append(",");
				}else if(gTypeName.contains("String")){
					s.append("\"String\",");
				}else{
					s.append(gTypeName).append(",");
				}
			}catch(ClassNotFoundException ce){
				String t = ce.getMessage().replace("[]", "");
				//类型名称里如果包含.则认为是java类
				if(t.contains(".")){
					s.append("\"class ").append(ce.getMessage()).append(" not found\",");
				}else{
					s.append("").append(t).append("");
				}
			}
		}
		if(s.toString().endsWith(",")){
			s.deleteCharAt(s.length()-1);
		}
		s.append("]");

		Gson gson = new Gson();
		JsonParser jp = new JsonParser();
		JsonFormatTool jft = new JsonFormatTool();
		String beautyJSON=null;
		try{
			JsonElement je = jp.parse(s.toString());
			beautyJSON = gson.toJson(je);
			beautyJSON = jft.formatJson(beautyJSON);
		}catch(JsonSyntaxException jse){
			jse.printStackTrace();
		}
		return beautyJSON;
	}

	//判断是否为java的基本类型
	/*private boolean isBasicType(String t) {
		if("byte".equals(t) || "int".equals(t) || "long".equals(t) || "char".equals(t)
				|| "short".equals(t) || "float".equals(t) || "double".equals(t)){
			return true;
		}else{
			return false;
		}
	}*/

	//将对象转化为json格式
	private String parseObj(Class<?> clazz) throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		StringBuilder s = new StringBuilder("{");
		
		s.append(getClassFieldString(clazz, new Vector<String>()));
		
		//s.deleteCharAt(s.length() - 1);
		s.append("}");
		//传入的不是复杂对象，则直接返回类型名称
		if(s.length()==1){
			s.setLength(0);
			s.append(clazz.getName());
		}
		
		Gson gson = new Gson();
		JsonParser jp = new JsonParser();
		JsonFormatTool jft = new JsonFormatTool();
		String beautyJSON=null;
		try{
			JsonElement je = jp.parse(s.toString());
			beautyJSON = gson.toJson(je);
			beautyJSON = jft.formatJson(beautyJSON);
		}catch(JsonSyntaxException jse){
			jse.printStackTrace();
		}
		return beautyJSON;
	}
	
	//获取该类的字段模板
	private String getClassFieldString(Class<?> clazz, Vector<String> overrideFields) throws ClassNotFoundException, NoSuchFieldException, SecurityException {
		StringBuilder s = new StringBuilder("");
		Vector<String> ofs = new Vector<String>();
		for(Field f : clazz.getDeclaredFields()){
			if(overrideFields.contains(f.getName()))
				continue;
			s.append("\"").append(f.getName()).append("\":");
			String typeName = f.getType().getName();
			if(typeName.startsWith("com.")){
				s.append(parseObj(f.getType())).append(",");
			}else if(f.getType().isArray() || isSubClass(f.getType(), Collection.class)){
				s.append(parseArray(clazz, -1, f.getName())).append(",");
			}else if(typeName.contains("String")){
				s.append("\"String\",");
			}else{
				s.append(typeName+",");
			}
			ofs.add(f.getName());
		}
		if(overrideFields!=null) {
			ofs.addAll(overrideFields);
		}
		if("true".equals(deepReflect)) {
			if(!clazz.getSuperclass().equals(Object.class)) {
				s.append(getClassFieldString(clazz.getSuperclass(), ofs));
				s.append(",");
			}
		}
		
		s.deleteCharAt(s.length() - 1);
		return s.toString();
		
	}

	//将Map转化为json格式
	/*private String parseMap(Class<?> clazz, int index, String fieldName) throws ClassNotFoundException {
		Class<?>[] genericType = null;
		StringBuffer s = new StringBuffer("{");
		try{
			if(index > 0){
				genericType = JarReflectUtil.getGenericMapType(c, method, index);
			}else{
				genericType = JarReflectUtil.getGenericMapType(clazz, fieldName);
			}
			
			//not implemented yet
			
		}catch(ClassNotFoundException ce){
			s.append("\"class '").append(ce.getMessage()).append("' not found\",");
		}
		
		s.append("}");
		
		Gson gson = new Gson();
		JsonParser jp = new JsonParser();
		JsonFormatTool jft = new JsonFormatTool();
		String beautyJSON=null;
		try{
			JsonElement je = jp.parse(s.toString());
			beautyJSON = gson.toJson(je);
			beautyJSON = jft.formatJson(beautyJSON);
		}catch(JsonSyntaxException jse){
			logger.info(jse);
		}
		return beautyJSON;
	}*/

	/**
	 * 获取最可能是我们需要的method，由于dubbo不暴露参数方法的特性，没有办法做到100%准确
	 * @param 方法名 - method
	 * @param 类名 - c
	 * @return Method对象
	 */
	public Method getAlmostMethod() {
		Method[] ms = c.getDeclaredMethods();
		Method targetMethod=null;
		for(Method m : ms){
			if(method.equals(m.getName())){
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
	
	public Method getAlmostMethod(int argCount) {
		Method[] ms = c.getDeclaredMethods();
		Method targetMethod=null;
		for(Method m : ms){
			if(method.equals(m.getName())&&m.getParameterTypes().length==argCount){
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

}
