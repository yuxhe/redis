package com.action.bdapp.common.util.tree;

import java.util.LinkedList;
import java.util.List;

/**
 * @author yuxh
 * @Description  
 * @revise
 * @time     2017年8月8日 下午3:16:54
 * @version 1.0
 * @copyright Copyright @2017, Co., Ltd. All right.
 */
public class Node {
	 //记录当前节点的字
    char c;
    //判断该字是否词语的末尾，如果是则为false
    boolean isEnd;
    //子节点
    List<Node> childList;
    public Node(char c) {
        super();
        this.c = c;
        isEnd = false;
        childList = new LinkedList<Node>();
    }
    //查找当前子节点中是否保护c的节点
    public Node findNode(char c){
        for(Node node : childList){
            if(node.c == c){
                return node;
            }
        }
        return null;
    }
}
