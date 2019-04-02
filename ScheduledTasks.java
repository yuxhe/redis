package com.action.bdapp.common.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisConnectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.action.bandou.common.util.JsonUtil;
import com.action.bdapp.common.mybatis.entity.SysBusparameter;
import com.action.bdapp.common.mybatis.entity.SysParameter;
import com.action.bdapp.common.mybatis.entity.SysSchoolbaseinfo;
import com.action.bdapp.common.mybatis.mapper.SysBusparameterMapper;
import com.action.bdapp.common.mybatis.mapper.SysParameterMapper;
import com.action.bdapp.common.mybatis.mapper.SysSchoolbaseinfoMapper;
import com.action.bdapp.common.util.tree.TrieTree;
import com.github.pagehelper.StringUtil;

import cn.jiguang.common.utils.StringUtils;
@Component
public class ScheduledTasks {
	//private static Logger logger =  LogManager.getLogger(ScheduledTasks.class);
	private static final Logger logger = LogManager.getLogger(ScheduledTasks.class);
	@Autowired
	private SysParameterMapper sysParameterMapper;
	@Autowired
	private   SysBusparameterMapper sysBusparameterMapper ;
	@Autowired
	private   SysSchoolbaseinfoMapper  sysSchoolbaseinfoMapper;
	@Autowired
    private   RedisTemplate   redisTemplate ;
	//@Autowired
	@Resource(name = "redisDbSelect10Template")
	private   StringRedisTemplate  stringRedisTemplate;
	@Autowired
	private   TrieTree  tree;
	
	
    @Scheduled(fixedRate = 10 * 60 * 1000) //10分钟刷新缓存  10 * 60 * 1000
    public void  loadSysParam() {
       @SuppressWarnings("unchecked")
	   //HashOperations<String,String,Serializable> ops = redisTemplate.opsForHash();
       HashOperations<String,String,String> opscode = redisTemplate.opsForHash();
       List<SysParameter>  list=sysParameterMapper.findList() ;
       Iterator<SysParameter> iter=list.iterator() ;
       while (iter.hasNext()) {
    	    SysParameter  sysParameter = iter.next();	    
			String code = sysParameter.getSysparamcode();
			opscode.put("sysparam", code, sysParameter.getSysparamname());
			//System.out.println("----------------------------="+sysParameter.getSysparamcode()); 
			//logger.debug("加载系统参数"+code +"="+JsonUtil.toJson(sysParameter));
			//SysParameter sysParameter1=(SysParameter)ops.get("sysparam","ORGNO"); //缓存中获取某系统参数
			//System.out.println("++++++++++="+JsonUtil.toJson(sysParameter1));
		}
       
    }
    
    @Scheduled(fixedRate = 10 * 60 * 1000) //10分钟  10 * 60 * 1000
    public  void loadBusParam() {
    	 @SuppressWarnings("unchecked")
  	     //序列化，还是存在性能问题
    	 HashOperations<String,String,String> opscode = redisTemplate.opsForHash();    	 
         List<SysBusparameter>  listDb=sysBusparameterMapper.findList() ;
         Iterator<SysBusparameter> iter=listDb.iterator() ;
         Map<String, List<SysBusparameter>> parentMap = new HashMap<String, List<SysBusparameter>>();
         int i= -1;
 		 while (iter.hasNext()) {
 			i=i+1;
 			SysBusparameter  sysBusparameter = iter.next();
 			String busparamcode = sysBusparameter.getBusparamcode();
 			String parbusparamcode =sysBusparameter.getPbusparamcode();
 			opscode.put("busparameter", busparamcode,sysBusparameter.getBusparamname());		
 			if (StringUtil.isNotEmpty(parbusparamcode)) {
 				List<SysBusparameter> list = parentMap.get(parbusparamcode);
 				if (list == null) {
 					list = new ArrayList<SysBusparameter>();
 				}
 				list.add(sysBusparameter);
 				if  (!iter.hasNext() ||  !parbusparamcode.equals(listDb.get(i+1).getPbusparamcode())) {  //同下一项不等 则 放入缓存
 					opscode.put("parent_busparameter", parbusparamcode,JsonUtil.toJson(list)); //组合出的 父节点的存储 哈
 				}	
 				parentMap.put(parbusparamcode,list);
 			} else {
 				parentMap.put(parbusparamcode, new ArrayList<SysBusparameter>());
 			}
 			//SysBusparameter sysParameter1=(SysBusparameter)ops.get("sysparam","ORGNO"); //缓存中获取某系统参数
 			//logger.debug("加载业务参数"+busparamcode +"="+JsonUtil.toJson(sysBusparameter));
 		}
 		 
 		/*
 		SysBusparameter sysBusparameter=(SysBusparameter)ops.get("busparameter","badminton");
 		//获取单个的打印
 		System.out.println("++++++++++="+JsonUtil.toJson(sysBusparameter));		
 		List<SysBusparameter> sysBusparameterL=(List<SysBusparameter>)ops.get("parent_busparameter","badminton"); //获取子项目
 		System.out.println("=============="+JsonUtil.toJson(sysBusparameterL));
 		*/	
    }
    
    //全国学校名称 刷新缓存
    @Scheduled(initialDelay=20,fixedRate = 180 * 60 * 1000) //20 分钟刷新缓存  20 * 60 * 1000
    public void  loadSchoolbaseinfo() {
       @SuppressWarnings("unchecked")
       //ZSetOperations<String,String> zset=stringRedisTemplate.opsForZSet();
       SetOperations<String,String>  set=stringRedisTemplate.opsForSet();
       ValueOperations<String,String> ops=stringRedisTemplate.opsForValue() ;
       List<SysSchoolbaseinfo>  list=sysSchoolbaseinfoMapper.findAll() ;
       //TrieTree tree = new TrieTree();
       try {
    	   String encoding="UTF-8";
    	   InputStream in=getClass().getClassLoader().getResourceAsStream("sjxzqu.txt");
    	   BufferedReader in2=new BufferedReader(new InputStreamReader(in,encoding));
    			   String lineTxt = null;
    			   while((lineTxt = in2.readLine()) != null){
    					tree.insert(lineTxt);
    					if  (lineTxt.endsWith("街"))  {
    						tree.insert(lineTxt.substring(0, lineTxt.length() - 1));
    					}
    					if  (lineTxt.endsWith("乡"))  {
    						tree.insert(lineTxt.substring(0, lineTxt.length() - 1)+"村");
    						tree.insert(lineTxt.substring(0, lineTxt.length() - 1));
    					}
    			   }
    			   in2.close();
    	} catch (Exception e) {
    		System.out.println("读取文件内容出错");
    		logger.info("读取文件内容出错");
    		e.printStackTrace();
    	}
       
       int count = 1000;       //暂时按一千条划分
       int listSize = list.size();
       int RunSize = (listSize / count)+1;   //20000/1000

       //ThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(RunSize);
       //ExecutorService executor = Executors.newSingleThreadExecutor();
       ExecutorService executor= Executors.newFixedThreadPool(RunSize);
       //ExecutorService executor=Executors.newSingleThreadExecutor();
       //CountDownLatch countDownLatch = new CountDownLatch(RunSize);
       for (int i = 0; i < RunSize; i++) {
    	   List<SysSchoolbaseinfo> newList = null ;
           if((i+1)==RunSize){
               int startIndex = (i*count);
               int endIndex = list.size();
               newList =list.subList(startIndex,endIndex);
           }else{
               int startIndex = i*count;
               int endIndex = (i+1)*count;
               newList =list.subList(startIndex,endIndex);
           }
           ExecupteRedis redisRunnable = new ExecupteRedis(newList,"Thread"+i);
           executor.execute(redisRunnable);
       }
       executor.shutdown();
       System.out.println("学校集合 --- 索引开始建立");
       logger.info("学校集合 --- 索引开始建立");
    }
    
    //----------------------------------------------------
    class ExecupteRedis implements Runnable{ //填充数据哈
        private List <SysSchoolbaseinfo> list;
        private Map<String,List<String>> mapList=new HashMap<String,List<String>>();//set数据
        private Map<String,String> mapValue=new HashMap<String,String>();//单个数据
        private ThreadLocal<Integer> threadLocal = null;//记录当前循环的次数
        private String  name;//线程名称
        public ExecupteRedis (List <SysSchoolbaseinfo> list,String name){
            this.list  = list ;
            this.name=name;//线程名称
        }

        @Override
        public void run() {
            if(null!=list){
            	SetOperations<String,String>  set=stringRedisTemplate.opsForSet();
                ValueOperations<String,String> ops=stringRedisTemplate.opsForValue() ;
                
            	Iterator<SysSchoolbaseinfo> iter=list.iterator() ;
            	if (threadLocal==null) {
                {
                	threadLocal = new ThreadLocal<Integer>() ;
                    threadLocal.set(0);
                 }
            	}
                try {
                while (iter.hasNext() &&  threadLocal.get() <1000) {//按1000个分隔数据
             	   SysSchoolbaseinfo  sysSchoolbaseinfo = iter.next();//Zset:sysSchoolbaseinfo
             	   String  str=sysSchoolbaseinfo.getSchoolname();
             	   List<String>  setList=tree.parseWords(str);                 
             	   threadLocal.set(threadLocal.get()+1);
             	   for (String key:setList) {
             		  if  (StringUtils.isEmpty(key) ) {
             			  continue;
             		  }
             	      List<String>  mList=mapList.get(key) ;
             	      if  (mList==null) {
             	    	  mList=new ArrayList<String>();
             	      }
             	      mList.add(sysSchoolbaseinfo.getSchoolid().toString());
             	      mapList.put(key, mList);
             	   }
             	   mapValue.put("set:sysSchoolbaseinfo:"+sysSchoolbaseinfo.getSchoolid().toString(), str);
         		}

                //1、-------------------------------------------加入list数据   zset数据,目的异常时从异常点从新开始循环
                List<String>  list=new ArrayList<>() ;
                for (String key:mapList.keySet()) {
                	list.add(key) ;
                }
                
                int i=0;
                for (int k=0;k<list.size();k++) {
                	try {
                	i++;
                	String key=list.get(k);
                	String[] strings = new String[mapList.get(key).size()];
             	    mapList.get(key).toArray(strings);
                    set.add(key,strings) ;
                    if  (i==20) {//20个时要延迟一下
   	                	RedisConnectionUtils.unbindConnection(stringRedisTemplate.getConnectionFactory());
   	              	    try {
   	          			   Thread.sleep(100);
   	          		    } catch (InterruptedException e) {
   	          			   e.printStackTrace();
   	          		    }
   	              	    i=0;
               	    }
            	  } catch (Exception e) { //从新开始本次循环，由于存在异常
          			  e.printStackTrace();
          			  logger.info("线程A放缓存" + Thread.currentThread().getName()+":"+e.getMessage());
          			  System.out.println("线程" + Thread.currentThread().getName()+" 终止set在k="+k);
          			  k--;
          			  i--;
          		  }
                }
                
                //2、-------------------------------------------加入list数据  set数据，目的异常时从异常点从新开始循环
                list=new ArrayList<>() ;
                for (String key:mapValue.keySet()) {
                	list.add(key) ;
                }
                
                i=0;
                for (int k=0;k<list.size();k++) {
                	try {
                		 i++;
	                	 String key=list.get(k);
		               	 ops.set(key, mapValue.get(key));
		               	 stringRedisTemplate.expire(key, 2, TimeUnit.DAYS) ;
		               	 if (i==20) {//20个时要延迟一下
		               		RedisConnectionUtils.unbindConnection(stringRedisTemplate.getConnectionFactory());
		               		try {
		               			   Thread.sleep(100);
		               		} catch (InterruptedException e) {
		               			   e.printStackTrace();
		               		}
		               		i=0;
		               	 }
                	}catch (Exception e) { //从新开始本次循环，由于存在异常
            			  e.printStackTrace();
            			  logger.info("线程B放缓存" + Thread.currentThread().getName()+":"+e.getMessage());
            			  System.out.println("线程" + Thread.currentThread().getName()+" 终止简单set在k="+k);
              			  k--;
              			  i--;
              		}               	 
                }
                System.out.println("本次扫描放入redis缓存size="+mapList.size());
                logger.info("本次扫描放入redis缓存size="+mapList.size());
                }catch (Exception e) {
                	System.out.println("--------------------------");
        			e.printStackTrace();
        			logger.info(e.getMessage());
        		}
                
            }

        }
    }
    //----------------------------------------------------
 
}