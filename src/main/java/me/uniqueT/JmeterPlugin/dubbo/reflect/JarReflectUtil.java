package me.uniqueT.JmeterPlugin.dubbo.reflect;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JarReflectUtil {
	
	private static final String TMP_DIR = "tmp";

	/**
	 * 得到方法中某个泛型参数的具体类型
	 * @param c - 方法所在的类
	 * @param method - 方法名称
	 * @param index - 目标参数在方法参数列表的第几个
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getGenericType(Class<?> c, String method, int index) throws ClassNotFoundException {
		String className = null;
		//从jar包释放class文件出来
		String classFile = extractClass(c);
		String javaFile = classFile.replace(".class", ".java");
		//组成反编译命令
		StringBuilder cmd = new StringBuilder("cmd /c javap ").append(classFile).append(" | findstr ")
				.append(method).append(" > ").append(javaFile);
		//得到泛型所在的行
		String keyLine = getKeyLine(cmd.toString(), javaFile, method);
		//logger.info("keyLine  +++++ "+keyLine);
		//得到尖括号内的字符串
		className = getGenericWords(keyLine, index);
		return Class.forName(className);
	}

	/**
	 * 得到一个类的某泛型字段的具体类型
	 * @param c - 泛型字段所在的类
	 * @param fieldName - 泛型字段的名称
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static Class<?> getGenericType(Class<?> c, String fieldName) throws ClassNotFoundException {
		String className = null;
		//从jar包释放class文件出来
		String classFile = extractClass(c);
		String javaFile = classFile.replace(".class", ".java");
		//组成反编译命令
		StringBuilder cmd = new StringBuilder("cmd /c javap -p ").append(classFile).append(" | findstr ")
				.append(fieldName).append(" > ").append(javaFile);
		//得到泛型所在的行
		String keyLine = getKeyLine(cmd.toString(), javaFile, fieldName);
		//logger.info("keyLine  +++++ "+keyLine);
		//得到尖括号内的字符串
		className = getGenericWords(keyLine);
		//logger.fine("debug  +++++ 2 "+className);
		return Class.forName(className);
	}
	
	private static String getGenericWords(String keyLine, int index){
		//获取方法签名中的泛型参数
		Pattern pattern = Pattern.compile("\\(.+?\\)");
		Matcher matcher = pattern.matcher(keyLine);
		matcher.find();
		String matchResult = matcher.group();
		String para = matchResult.substring(1, matchResult.length()-1).split(",")[index-1].trim();
		//获取泛型参数中的具体类型
		pattern = Pattern.compile("<.+?>");
		matcher = pattern.matcher(para);
		if(matcher.find()){
			matchResult = matcher.group();
			//logger.info("match result is :" + matchResult);
			return matchResult.substring(1, matchResult.length()-1);
		}else{
			//logger.info("can not find generic type in line: " + keyLine);
			//如果泛型未指定具体类型，则返回参数类型
			return para;  //javap反编译的方法中只有参数类型，没有参数名称
			//return para.split("\\s+")[1];  //jad反编译的方法中可以返回参数名称
		}
	}
	
	private static String getGenericWords(String keyLine){
		//获取域声明中的的泛型参数的具体类型
		Pattern pattern = Pattern.compile("<.+?>");
		Matcher matcher = pattern.matcher(keyLine);
		if(matcher.find()){
			String matchResult = matcher.group();
			//logger.info("match result is :" + matchResult);
			return matchResult.substring(1, matchResult.length()-1);
		}else{
			//如果泛型未指定具体类型，则返回成员变量名
			//logger.info("can not find generic type in line: " + keyLine);
			String words[] = keyLine.split("\\s+");
			return words[words.length-1].replace(";", "");
		}
	}
	
	/**
	 * 使用javap工具反编译class文件，然后读取所需要的行
	 * @param cmd
	 * @param javaFile
	 * @return
	 */
	private static String getKeyLine(String cmd, String javaFile, String keyWord){
		BufferedReader br = null;
		String result=null;
		File f = new File(javaFile);
		Process p = null;
		try {
			//重视效率，当模板生成不对时可以考虑手工清空tmp文件夹
			if(!f.exists()){
				p = Runtime.getRuntime().exec(cmd);
				//Runtime调用生成文件时不一定立即完成操作系统磁盘上的动作，这里主动等待一下
				p.waitFor();
			}
			br = new BufferedReader(new FileReader(f));
			result = br.readLine();
			
			//当已有文件中不存在需要的字符时，再重新反编译
			if(!result.contains(keyWord)){
				br.close();
				p = Runtime.getRuntime().exec(cmd);
				p.waitFor();
				
				br = new BufferedReader(new FileReader(f));
				result = br.readLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			try {
				if(br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * 将class文件从jar包里释放出来,放入tmp目录中
	 * @param c
	 * @return
	 */
	private static String extractClass(Class<?> c){
		JarFile jarFile = null;
		BufferedInputStream in = null;
		BufferedOutputStream os = null;
		String classFile = null;
		try {
			String resourceUrl= URLDecoder.decode(c.getResource("").getPath(), "UTF-8");
			String jarpath = resourceUrl.substring(5, resourceUrl.indexOf("!"));
			//logger.fine("jarpath ===== "+jarpath);
			jarFile = new JarFile(jarpath);
			String filePath = c.getName().replace(".", "/")+".class";
			//logger.fine("filePath ===== "+filePath);
			classFile = TMP_DIR + "/" + filePath;
			File f = new File(classFile);
			if(f.exists())
				return classFile;
			
			JarEntry entry = jarFile.getJarEntry(filePath);
			File tmpdir = new File(TMP_DIR);
			if(!tmpdir.exists())
				tmpdir.mkdir();
			tmpdir = new File(TMP_DIR + File.separator + filePath.substring(0, filePath.lastIndexOf("/")));
			if(!tmpdir.exists())
				tmpdir.mkdirs();
			
			in = new BufferedInputStream(jarFile.getInputStream(entry));
			os = new BufferedOutputStream(new FileOutputStream(classFile));
			byte[] b = new byte[2048];
			int byteread = 0;
			while((byteread = in.read(b))!=-1){
				os.write(b, 0, byteread);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}finally{
			try {
				if(jarFile != null)
					jarFile.close();
				if(os != null)
					os.close();
				if(in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return classFile;
	}

}
