package com.action.bdapp.common.util.tree;

import java.util.List;
import java.util.ListIterator;

import org.springframework.stereotype.Component;

/**
 * @author yuxh
 * @Description  
 * @revise
 * @time     2017年8月8日 下午3:15:09
 * @version 1.0
 * @copyright Copyright @2017, Co., Ltd. All right.
 */
@Component
public class TrieTree {
	Node root = new Node(' ');
    //构建Trie Tree
    public void insert(String words){
        char[] arr = words.toCharArray();
        Node currentNode = root;
        for (char c : arr) {
            Node node = currentNode.findNode(c);
            //如果不存在该节点则添加
            if(node == null){
                Node n = new Node(c);
                currentNode.childList.add(n);
                currentNode = n;
            }else{
                currentNode = node;
            }
        }
        //在词的最后一个字节点标记为true
        currentNode.isEnd = true;
    }

    //判断Trie Tree中是否包含该词
    public boolean search(String word){
        char[] arr = word.toCharArray();
        Node currentNode = root;
        for (int i=0; i<arr.length; i++) {
            Node n = currentNode.findNode(arr[i]);
            if(n != null){
                currentNode = n;
                //判断是否为词的尾节点节点
                if(n.isEnd){
                    if(n.c == arr[arr.length-1]){
                        return true;
                    }
                }
            }
        }
        return false;
  }

  //自己写个递归while 循环 ,循环不漏掉一个单词匹配，放弃最大匹配原则
    public  List<String> parseWords(String words){
    	//算法要领：
    	//1、递归到每个字  ，while 循环开始匹配，注意当前字典树的移动
    	//2、无匹配自动 跳转到下一个字，同时字典树从头开始匹配
    	//3、若有匹配，则匹配出所有的，所有儿子一一进行匹配完整
    	words=words.replace("（", "") ;
    	words=words.replace("）", "") ;
    	words=words.replace("(", "") ;
    	words=words.replace(")", "") ;
    	words=words.replace(" ", "") ;
    	words=words.replace(" ", "") ;
    	List<String>  listSet = new SetList<>();
    	if (words.length() >0) {//全字拆分
	           for (int p=0;p<words.length();p++) {//同时拆分出每个汉字
	       		    listSet.add(words.substring(p, p+1));
	       	   }
     	}
    	//System.out.println(words);
    	Node currentNode = root;
    	String word="";
    	String preStr="";
    	StringBuilder sb = new StringBuilder();
        char[] arr = words.toCharArray();
        int ii_length=arr.length;
        int   ii_pos_current=0;
        int   idx=0;
        int   numbers=0;
        int   prepos=0;
        while (ii_pos_current < ii_length ) {
        	//开始匹配
        	for (int i=ii_pos_current; i<arr.length; i++) {
        		//System.out.println("currentNode="+currentNode.c +"   arr[i]="+arr[i]);
                Node n = currentNode.findNode(arr[i]);
                if(n != null){
                    sb.append(n.c);
                    currentNode = n;
                    //匹配到词
                    if(n.isEnd && (sb.toString().length() >1 || "一二三四五六七八九十".indexOf(sb.toString()) >=0)){//&& sb.toString().length() >1
                        //记录最后一次匹配的词
                        word = sb.toString();
                        //记录最后一次匹配坐标 
                        idx = i ;
                    	//System.out.println("----" + word +":"+i +";"+ idx);
                    	if  (word.startsWith(preStr) && word.length() > preStr.length() + 1)  {
                    		listSet.add(word.substring(preStr.length()));
                    	}
                    	//数据本身小时 做必要的拆分----------
                    	if (words.length() >=5 && words.length() <=8 && word.length()>1 && word.length()<=3) {
                    		listSet.add(words.substring(0,(words.length() - word.length() - 2) ));
                    	}
                    	//---------------------------------
                    	numbers=i - (word.length()) - prepos ; //- (word.length())
                    	//--------------------------------特殊处理数据
                    	if (prepos==0 && numbers==0) {
                    		prepos=-1;
                    		numbers=1;
                    	}else if (prepos==0) {
                    		prepos=-1;
                    		numbers=numbers+1;
                    	}
                    	//--------------------------------          
                        if   (numbers >0 &&  numbers <=3)  {//prepos >0 && 
                        	 listSet.add(words.substring(prepos + 1, prepos + 1 + numbers));
                        	 for (int p=0;p<numbers;p++) {//同时拆分出每个汉字
                        		 listSet.add(words.substring(prepos + 1 + p, prepos + 1 + p + 1));
                        	 }                            	 
                        }else if (numbers >=4) {//按字拆分数据
                        	 for  (int k=0;k<numbers;k++)  {
                        		//System.out.println( words.substring(prepos + 1 + k, prepos + 1 +k + 1));
                        		listSet.add(words.substring(prepos + 1 + k, prepos + 1 +k + 1));
                        	 }
                        }                           
                        listSet.add(word);
                        preStr=word;
                        prepos=i;
                    }
                }else{
                    //判断word是否有值
                	break;
                  }                   
            }
        	//数据从新初始化
        	sb = new StringBuilder();
        	currentNode = root;
        	idx=ii_pos_current;
        	ii_pos_current++;  //递归处理下一个哈
        	idx=ii_pos_current;
        	preStr="";
        }
        //-----------------------------------add yuxh on 2017-8-8 循环到末尾未匹配数据时的处理
        if  (idx - prepos >=2 && prepos >0) {
        	if (words.substring(prepos+1,idx).length() <=2) {
         	   listSet.add(words.substring(prepos+1,idx));
         	}
        }else  if (prepos==0) { //特殊处理
        	String  wordTmp=words;
        	if (wordTmp.endsWith("中学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("中学"));
        		listSet.add("中学");
        	}else if (wordTmp.endsWith("小学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("小学"));
        		listSet.add("小学");
        	}else if (wordTmp.endsWith("大学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("大学"));
        		listSet.add("大学");
        	}
        }
        //------------------------------------假如末尾为小学、中学 且只有 <=4个汉字
        if  (words.length()<=4) {//特殊处理数据少的
        	String  wordTmp=words;
        	if (wordTmp.endsWith("中学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("中学"));
        		listSet.add(wordTmp);
        	}else if (wordTmp.endsWith("小学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("小学"));
        		listSet.add(wordTmp);
        	}else if (wordTmp.endsWith("大学")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("大学"));
        		listSet.add(wordTmp);
        	}
        }
        
        if  (words.length()<=5) {//特殊处理数据少的
        	String  wordTmp=words;
        	if (wordTmp.endsWith("中心校")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("中心校"));
        		listSet.add(wordTmp);
        	}else if (wordTmp.endsWith("小学校")) {
        		wordTmp=wordTmp.substring(0, wordTmp.lastIndexOf("小学校"));
        		listSet.add(wordTmp);
        	}
        }
        //------------------------------------
      //处理分离数字 字母的处理
        ListIterator<String> iter=listSet.listIterator(); 
        StringBuilder sbComp = new StringBuilder();
        while (iter.hasNext()) {
        	String  str=iter.next() ;
        	if (str.length()==1 && str.compareTo("0") >=0 && str.compareTo("9") <=0) {
        		iter.remove();
        		String  worddata=sbComp.toString();
        		if  (worddata!=null && worddata.length()>0 && (worddata.substring(0, 1).compareTo("A") >=0 && worddata.substring(0, 1).compareTo("z") <=0))  {
        			iter.add(worddata);
        			sbComp = new StringBuilder();
        		}
        		sbComp.append(str);	
        	}
        	else if (str.length()==1 && str.compareTo("A") >=0 && str.compareTo("z") <=0) {
        		iter.remove();
        		String  worddata=sbComp.toString();
        		if  (worddata!=null && worddata.length()>0 && (worddata.substring(0, 1).compareTo("0") >=0 && worddata.substring(0, 1).compareTo("9") <=0))  {
        			iter.add(worddata);
        			sbComp = new StringBuilder();
        		}
        		sbComp.append(str);
        	}
        	else {
        		String  worddata=sbComp.toString();
        		if  (worddata!=null && worddata.length()>0)  {
        			iter.add(worddata);
        		}
        		sbComp = new StringBuilder();
        	}
        } 
        //-----------------数字字母在末尾 
        String  worddata=sbComp.toString();
        if  (worddata!=null && worddata.length()>0)  {
			iter.add(worddata);
		}
        //--------------------------add yuxh on 2017-8-8  防止整个数据为空的情况
        if (listSet.isEmpty()) {
        	for (int p=0;p<words.length();p++) {//同时拆分出每个汉字
       		    listSet.add(words.substring(p, p+1));
       	    }
        	listSet.add(words);
        }
        
        //删除数据 ，增强性能
    	if (listSet.contains("小学")) {
    		listSet.remove("小");
    		listSet.remove("学");
    		listSet.remove("校");
    	}
    	
    	if (listSet.contains("中学")) {
    		listSet.remove("中");
    		listSet.remove("学");
    		listSet.remove("校");
    	}
    	
        /*
    	if (listSet.contains("中心")) {
    		listSet.remove("中");
    		listSet.remove("心");
    		listSet.remove("学");
    		listSet.remove("校");
    	}
    	*/
    	
    	if (listSet.contains("大学")) {
    		listSet.remove("大");
    		listSet.remove("学");
    	}
        //--------------------------
        return  listSet;
    }
}

