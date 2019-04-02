package com.action.bdapp.common.util.tree;

import java.util.LinkedList;

/**
 * @author yuxh
 * @Description  
 * @revise
 * @time     2017年8月8日 下午3:17:59
 * @version 1.0
 * @copyright Copyright @2017, Co., Ltd. All right.
 */
public class SetList<T> extends LinkedList<T>{
	private static final long serialVersionUID = 1434324234L;  
    @Override  
    public boolean add(T object) {  
        if (size() == 0) {  
            return super.add(object);  
        } else {  
            int count = 0;  
            for (T t : this) {  
                if (t.equals(object)) {  
                    count++;  
                    break;  
                }  
            }  
            if (count == 0) {  
                return super.add(object);  
            } else {  
                return false;  
            }  
        }  
    }
}
