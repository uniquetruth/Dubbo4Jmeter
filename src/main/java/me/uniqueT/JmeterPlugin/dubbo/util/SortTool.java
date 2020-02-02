package me.uniqueT.JmeterPlugin.dubbo.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

public class SortTool {
	
	@SuppressWarnings("unchecked")
	public static Map<String, Object> sortMap(Map<String, Object> originalMap){
		TreeMap<String, Object> resultMap = new TreeMap<String, Object>(originalMap);
		Iterator<Entry<String, Object>> iter = resultMap.entrySet().iterator();
		while (iter.hasNext()) {
			Entry<String, Object> entry = iter.next();
			Object val = entry.getValue();
			try{
				if(val instanceof Map){
					Map<String, Object> map = (Map<String, Object>)val;
					entry.setValue(sortMap(map));
				}else if(val instanceof Collection){
					List<Map<String, Object>> l = (ArrayList<Map<String, Object>>)val;
					entry.setValue(sortMapInList(l));
				}
			} catch (ClassCastException e) {
				continue;
			}
		}
		return resultMap;
	}
	
	public static List<Map<String, Object>> sortMapInList(List<Map<String, Object>> originalList){
		ArrayList<Map<String, Object>> l = new ArrayList<Map<String, Object>>();
		for(Map<String, Object> m : originalList){
			if(m != null){
				l.add(SortTool.sortMap(m));
			}else{
				l.add(null);
			}
		}
		return l;
	}

}
