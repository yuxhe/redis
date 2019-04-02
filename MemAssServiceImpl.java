package com.action.bdapp.modules.mem.service.impl;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.action.bandou.common.spring.CustomizedPropertyConfigurer;
import com.action.bandou.common.util.DateUtils;
import com.action.bandou.common.util.JsonUtil;
import com.action.bandou.common.util.RandUtil;
import com.action.bandou.modules.inf.model.InfSysremindinfo;
import com.action.bandou.modules.inf.service.InfBdService;
import com.action.bandou.mongo.model.MongoValueField;
import com.action.bandou.mongo.service.MongoService;
import com.action.bdapp.common.exception.BusinessException;
import com.action.bdapp.common.mybatis.entity.EvaGrowthevaluation;
import com.action.bdapp.common.mybatis.entity.MemBaseinfo;
import com.action.bdapp.common.mybatis.entity.MemFamilyattainment;
import com.action.bdapp.common.mybatis.entity.MemFamilybase;
import com.action.bdapp.common.mybatis.entity.MemFamilymember;
import com.action.bdapp.common.mybatis.entity.SysSchoolbaseinfo;
import com.action.bdapp.common.mybatis.entity.TravelStudentinfo;
import com.action.bdapp.common.mybatis.mapper.MemBaseinfoMapper;
import com.action.bdapp.common.mybatis.mapper.MemFamilyattainmentMapper;
import com.action.bdapp.common.mybatis.mapper.MemFamilybaseMapper;
import com.action.bdapp.common.mybatis.mapper.MemFamilymemberMapper;
import com.action.bdapp.common.mybatis.mapper.SysSchoolbaseinfoMapper;
import com.action.bdapp.common.mybatis.mapper.TravelStudentinfoMapper;
import com.action.bdapp.common.util.tree.TrieTree;
import com.action.bdapp.modules.mem.model.SchoolModel;
import com.action.bdapp.modules.mem.service.CardUtil;
import com.action.bdapp.modules.mem.service.GrowupNoteService;
import com.action.bdapp.modules.mem.service.MemAssService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.StringUtil;

@Service
public class MemAssServiceImpl implements MemAssService {
    @Resource
    private  MemBaseinfoMapper   memBaseinfoMapper ;
    @Resource
    private  MemFamilybaseMapper    memFamilybaseMapper;
    @Resource
    private  MemFamilymemberMapper  memFamilymemberMapper ;
    @Resource
    private MemFamilyattainmentMapper   memFamilyattainmentMapper;
    @Resource
    private SysSchoolbaseinfoMapper  sysSchoolbaseinfoMapper;
    @Resource
	private  TravelStudentinfoMapper   travelStudentinfoMapper ;
	@Autowired
	private MongoService mongoService;
	@Autowired
	private InfBdService   infService ;
	@Autowired
	private GrowupNoteService  growupNoteService;   //add yuxh on 2017-6-13 关于成长记录笔记自然的特殊处理
	@Autowired
	private MongoTemplate mongoTemplate;
	@Autowired
    private   RedisTemplate   redisTemplate ;
	//@Autowired
	@Resource(name = "redisDbSelect10Template")
	private   StringRedisTemplate  stringRedisTemplate;
	
	@Autowired
	private   TrieTree  tree;
	
	
	
	
	
	/*** add  yuxh on 2017-7-2 
	 *   自动匹配学校名称   从缓存获取学校下拉列表数据
	 */
	public  List<SchoolModel>  getSchoolMatch(String  words) {
		long t1=new Date().getTime() ;
		List<SchoolModel>  list=new ArrayList<SchoolModel>();
		List<SchoolModel>  listmatch=new ArrayList<SchoolModel>();
		StringBuffer listpos=new  StringBuffer(",");
		StringBuffer listmatchpos=new  StringBuffer(",");
		if  (Strings.isBlank(words)) {
			return  list;
		}
		//TrieTree tree = new TrieTree();
		List<String>  setList=tree.parseWords(words);
		List<String>  setMatchList=new ArrayList<>();
	    //----------------------算法调整、
		//1、算法要领：重点在优化性能
		SetOperations<String, String>  set=stringRedisTemplate.opsForSet();
		ValueOperations<String,String> ops=stringRedisTemplate.opsForValue() ;
		String	identifier="";
		Set<String>  ids=null;
		//System.out.println(setList); //???
		//--------------------------------------------
		//if  (ids.isEmpty()) {//按单字获取数据
			List<String>  singleids=new ArrayList<String>();//2、单个字的拆分
			for (Iterator<String> iterator = setList.iterator(); iterator.hasNext(); ){  
				 String  currentStr=iterator.next().trim();
		         if (currentStr.length()==1 || Strings.isBlank(currentStr)){//排除特殊字符
		        	 if (Strings.isNotBlank(currentStr)  && (!("省".equals(currentStr) || "市".equals(currentStr) || "县".equals(currentStr) || "镇".equals(currentStr) 
		        			 || ("大".equals(currentStr) &&  words.indexOf("大学") >=0) || ("中".equals(currentStr) &&  words.indexOf("中学") >=0) || ("小".equals(currentStr) &&  words.indexOf("小学") >=0)    || "区".equals(currentStr) || "村".equals(currentStr) || "乡".equals(currentStr)))) {
		        		 singleids.add(currentStr);
		        	 }
		         } else if (currentStr.length()==2) {
		        	 if (!("小学".equals(currentStr) || "中学".equals(currentStr))) {
		        		 setMatchList.add(currentStr);
		        	 }
		         }
		    }
			
			StringBuffer  preword=new StringBuffer();//去掉省、市、区等的数据
			for (String str:singleids) {
				preword.append(str) ;
			}
			
			identifier =RandUtil.uuid();
			set=stringRedisTemplate.opsForSet();
			ops=stringRedisTemplate.opsForValue() ;
			System.out.println("singleids="+singleids);//
			//System.out.println("identifier="+identifier);
			
			//long t0=new Date().getTime();
			//System.out.println((t0 - t1) +"毫秒");
			if  (singleids==null || singleids.isEmpty()) {//add 
				return  list;
			}
			set.intersectAndStore(null, singleids, identifier) ;
			//--------------算法是否再次拆分
			stringRedisTemplate.expire(identifier, 2, TimeUnit.MINUTES) ;
			ids=set.members(identifier) ;
			//ids.addAll(ids1) ;
		//}
		//--------------------------------------------
			
		/*
		long t2=new Date().getTime();
		System.out.println("b="+(t2 - t0) +"毫秒");
		System.out.println(ids);
		*/	
		int i=0;
		Set<String>  idskey=new HashSet<>();
		for (String str:ids) {
			 idskey.add("set:sysSchoolbaseinfo:" +str);
		}
		
		List<String>  schoolnames=ops.multiGet(idskey) ;
		/*
		System.out.println("ids="+ids);
		System.out.println("schoolnames="+schoolnames);
		*/
		int forint=-1; //控制获取的redis 缓存中的数据
		SchoolModel school=null;
		for  (String str:ids) {//set:sysSchoolbaseinfo:
			 forint++;
			 i++;
			 if (i >100) {
				break;
			 }
			 
			 String  schoolanme=schoolnames.get(forint);  //ops.get("set:sysSchoolbaseinfo:"+str);
			 //---------------------------------add yuxh on 2017-8-10 排除数据、按类型
			 if  (words.indexOf("中学") >=0 ||  words.endsWith("中"))  {
				 if  (schoolanme.indexOf("小学") >=0 ||  schoolanme.indexOf("幼儿园") >=0 || schoolanme.indexOf("村小") >=0 || schoolanme.indexOf("村校") >=0) {//二者关系不匹配 跳过 
					 i-- ;
					 continue;
				 }
			 }
			 if  (words.indexOf("小学") >=0 || words.indexOf("村小") >=0 || words.indexOf("村校") >=0 || words.endsWith("小"))  {
				 if  (schoolanme.indexOf("中学") >=0 ||  schoolanme.indexOf("幼儿园") >=0) {//二者关系不匹配 跳过
					  i-- ;
					 continue;
				 }
			 }
			 
			 if  (words.indexOf("幼儿园") >=0 ||  words.endsWith("幼"))  {
				 if  (schoolanme.indexOf("中学") >=0 ||  schoolanme.indexOf("小学") >=0 || words.indexOf("村小") >=0 || words.indexOf("村校") >=0) {//二者关系不匹配 跳过
					 i--; 
					 continue;
				 }
			 }
			 
			 			 
			 school=new  SchoolModel();
    		 school.setSchoolname(schoolanme);    		 
    		 school.setSchoolid(Long.valueOf(str));
    		 String  sword="";
    		 if (setMatchList!=null && setMatchList.size()==1) {
    			 sword=setMatchList.get(0).substring(1,2) + setMatchList.get(0).substring(0,1);
    		 }
    		 int  i_break=0;
    		 if  (Strings.isNotBlank(sword) && schoolanme.indexOf(sword) >=0) {//字交叉，若存在则不加入此数据
    			 i_break=1;
    		 }
    		 if (i_break==0 && listpos.toString().indexOf(","+school.getSchoolname()+",") <0) {
    		    list.add(school);
    		    listpos.append(school.getSchoolname()).append(",");
    		 }
    		 
    		 String  schoolanmeTmp=schoolanme.replace("省","");
    		 schoolanmeTmp=schoolanmeTmp.replace("市","");
    		 schoolanmeTmp=schoolanmeTmp.replace("县","");
    		 schoolanmeTmp=schoolanmeTmp.replace("镇","");
    		 schoolanmeTmp=schoolanmeTmp.replace("区","");
    		 schoolanmeTmp=schoolanmeTmp.replace("村","");
    		 schoolanmeTmp=schoolanmeTmp.replace("乡","");
    		 
    		 if  (words.indexOf("大学") >=0) {
    		     schoolanmeTmp=schoolanmeTmp.replace("大","");
		     }
		     if  (words.indexOf("中学") >=0) {
    		   schoolanmeTmp=schoolanmeTmp.replace("中","");
	         }
		     if  (words.indexOf("小学") >=0) {
    		      schoolanmeTmp=schoolanmeTmp.replace("小","");
		     }
		     
    		 if (listmatchpos.toString().indexOf(","+school.getSchoolname()+",") <0) {//去重
	    		 if  (schoolanmeTmp.indexOf(preword.toString()) >=0 ) {//全匹配
	    		     listmatch.add(school);
	    		     listmatchpos.append(school.getSchoolname()).append(",");
	    		 }else if (setMatchList!=null && setMatchList.size()==1 && schoolanme.indexOf(setMatchList.get(0)) >=0) {
	    			 listmatch.add(school);//有拆词强拆匹配的数据
	    			 listmatchpos.append(school.getSchoolname()).append(",");
	    		 }else if  (words.indexOf("中心") >=0 &&  schoolanme.indexOf("中心") >=0)  {
	    			 listmatch.add(school);//均含有中心校字眼
	    			 listmatchpos.append(school.getSchoolname()).append(",");
	    		 }
    		 }
		}
		
		//System.out.println("c="+(new Date().getTime() - t2) +"毫秒");
		if (listmatch!=null && !listmatch.isEmpty()  ) {//强势匹配策略
			Collections.sort(listmatch,new Comparator<SchoolModel>(){
	            public int compare(SchoolModel arg0, SchoolModel arg1) {
	                return arg0.getSchoolname().compareTo(arg1.getSchoolname());
	            }
	        });
			return  listmatch;
		}
		Collections.sort(list,new Comparator<SchoolModel>(){
            public int compare(SchoolModel arg0, SchoolModel arg1) {
                return arg0.getSchoolname().compareTo(arg1.getSchoolname());
            }
        });
		return  list;
	}
	
	
}
