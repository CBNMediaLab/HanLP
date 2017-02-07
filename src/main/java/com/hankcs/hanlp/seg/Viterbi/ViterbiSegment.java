/*
 * <summary></summary>
 * <author>He Han</author>
 * <email>hankcs.cn@gmail.com</email>
 * <create-date>2015/1/19 20:51</create-date>
 *
 * <copyright file="ViterbiSegment.java" company="上海林原信息科技有限公司">
 * Copyright (c) 2003-2014, 上海林原信息科技有限公司. All Right Reserved, http://www.linrunsoft.com/
 * This source is subject to the LinrunSpace License. Please contact 上海林原信息科技有限公司 to get more information.
 * </copyright>
 */
package com.hankcs.hanlp.seg.Viterbi;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.recognition.nr.JapanesePersonRecognition;
import com.hankcs.hanlp.recognition.nr.PersonRecognition;
import com.hankcs.hanlp.recognition.nr.TranslatedPersonRecognition;
import com.hankcs.hanlp.recognition.ns.PlaceRecognition;
import com.hankcs.hanlp.recognition.nt.OrganizationRecognition;
import com.hankcs.hanlp.seg.WordBasedGenerativeModelSegment;
import com.hankcs.hanlp.seg.common.NerObject;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Viterbi分词器<br>
 * 也是最短路分词，最短路求解采用Viterbi算法
 *
 * @author hankcs
 */
public class ViterbiSegment extends WordBasedGenerativeModelSegment
{
    @Override
    protected List<Term> segSentence(char[] sentence)
    {
//        long start = System.currentTimeMillis();
        WordNet wordNetAll = new WordNet(sentence);
        ////////////////生成词网////////////////////
        GenerateWordNet(wordNetAll);
        ///////////////生成词图////////////////////
//        System.out.println("构图：" + (System.currentTimeMillis() - start));
        if (HanLP.Config.DEBUG)
        {
            System.out.printf("粗分词网：\n%s\n", wordNetAll);
        }
        
//        start = System.currentTimeMillis();
        List<Vertex> vertexList = viterbi(wordNetAll);//以viterbi算法进行分词

//        System.out.println("最短路：" + (System.currentTimeMillis() - start));

        if (config.useCustomDictionary)
        {
            combineByCustomDictionary(vertexList);
        }

        if (HanLP.Config.DEBUG)
        {
            System.out.println("粗分结果" + convert(vertexList, false));
        }

        // 数字识别
        if (config.numberQuantifierRecognize)
        {
            mergeNumberQuantifier(vertexList, wordNetAll, config);
        }

        // 实体命名识别
        if (config.ner)
        {
            WordNet wordNetOptimum = new WordNet(sentence, vertexList);
            int preSize = wordNetOptimum.size();
            if (config.nameRecognize)
            {
                PersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.translatedNameRecognize)
            {
                TranslatedPersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.japaneseNameRecognize)
            {
                JapanesePersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.placeRecognize)
            {
                PlaceRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (config.organizationRecognize)
            {
                // 层叠隐马模型——生成输出作为下一级隐马输入
                vertexList = viterbi(wordNetOptimum);
                wordNetOptimum.clear();
                wordNetOptimum.addAll(vertexList);
                preSize = wordNetOptimum.size();
                OrganizationRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);
            }
            if (wordNetOptimum.size() != preSize)
            {
                vertexList = viterbi(wordNetOptimum);
                if (HanLP.Config.DEBUG)
                {
                    System.out.printf("细分词网：\n%s\n", wordNetOptimum);
                }
            }
        }

        // 如果是索引模式则全切分
        if (config.indexMode)
        {
            return decorateResultForIndexMode(vertexList, wordNetAll);
        }

        // 是否标注词性
        if (config.speechTagging)
        {
            speechTagging(vertexList);
        }

        return convert(vertexList, config.offset);
    }
    
    /**
     * 获取命名实体识别最终结果
     * */
    public List<NerObject> getNER(String[] strArr)
    {   
    	List<String>strList=convertArrToList(strArr);
    	char[] sentence=getCharArr(strList);
//        long start = System.currentTimeMillis();
        WordNet wordNetAll = new WordNet(sentence);
        ////////////////生成词网////////////////////
        GenerateWordNet(wordNetAll);
        ///////////////生成词图////////////////////
//        System.out.println("构图：" + (System.currentTimeMillis() - start));
        if (HanLP.Config.DEBUG)
        {
            System.out.printf("粗分词网：\n%s\n", wordNetAll);
        }
//        start = System.currentTimeMillis();
//        List<Vertex> vertexList = viterbi(wordNetAll);
        LinkedList<Vertex> vertexList = new LinkedList<Vertex>();

        vertexList.add(new Vertex(""));

        for(String s:strList){
        	vertexList.add(new Vertex(s));
        }

        vertexList.add(new Vertex(""));

        
//        System.out.println("最短路：" + (System.currentTimeMillis() - start));

//        if (config.useCustomDictionary)
//        {
//        }


        if (HanLP.Config.DEBUG)
        {
            System.out.println("粗分结果" + convert(vertexList, false));
        }


        // 数字识别
        if (config.numberQuantifierRecognize)
        {
            mergeNumberQuantifier(vertexList, wordNetAll, config);
        }

        // 实体命名识别
        WordNet wordNetOptimum = new WordNet(sentence, vertexList);
        int preSize = wordNetOptimum.size();
        
        List<Map> cnNameList=PersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);//中文人名识别
        TranslatedPersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);//音译人名识别
        JapanesePersonRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);//日语人名识别
        
//        for(Map cnMap:cnNameList){
//        	System.out.println(cnMap.get("keyword")+"|"+cnMap.get("start")+","+cnMap.get("end")+","+cnMap.get("nature"));
//        }
        
        
        List<Map> locList=PlaceRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);//地名识别
//        for(Map locMap:locList){
//        	System.out.println(locMap.get("keyword")+"|"+locMap.get("start")+","+locMap.get("end")+","+locMap.get("nature"));
//        }

        wordNetOptimum.clear();
        wordNetOptimum.addAll(vertexList);
        preSize = wordNetOptimum.size();
        List<Map> orgList=OrganizationRecognition.Recognition(vertexList, wordNetOptimum, wordNetAll);//组织名称识别
//        for(Map orgMap:orgList){
//        	System.out.println(orgMap.get("keyword")+"|"+orgMap.get("start")+","+orgMap.get("end")+","+orgMap.get("nature"));
//        }
        
        
        if (wordNetOptimum.size() != preSize)
        {
//                vertexList = (LinkedList<Vertex>) viterbi(wordNetOptimum);
            if (HanLP.Config.DEBUG)
            {
                System.out.printf("细分词网：\n%s\n", wordNetOptimum);
            }
        }

//        // 如果是索引模式则全切分
//        if (config.indexMode)
//        {
//            return decorateResultForIndexMode(vertexList, wordNetAll);
//        }
//
        // 是否标注词性
//        if (config.speechTagging)
//        {
//            speechTagging(vertexList);
//        }
        
        
        List<Term> resultList=convert(vertexList, config.offset);
        appendNERtoResultList(resultList,cnNameList);
        appendNERtoResultList(resultList,locList);
        appendNERtoResultList(resultList,orgList);
//        int idx=0;
//        for(Term t:resultList){
//        	System.out.println(idx+":"+t.word+"|"+t.natureList);
//        	idx++;
//        }
        List<NerObject> nerList=extractNERCombination(resultList);
//        for(NerObject nerObj:nerList){
//        	System.out.println(nerObj.nerName+":"+nerObj.nerFlag+","+nerObj.start+","+nerObj.end);
//        }
        return nerList;
    }
    
    /**
     * 通过规则将层叠在词性标注结果resultList的natureList中的标注重新组合并提取成实体
     *
     */
    public static List<NerObject> extractNERCombination(List<Term> resultList){
    	//实体标注的优先级为org>loc>per，人名类的优先级为中文人名>日语人名>音译人名
    	//以整数表示等级以便计算，nrf:1,nrj:2,nr:3,ns:4,nt:5
    	//相连的同类实体识别合并成一个实体
    	List<NerObject> nerList=new ArrayList<NerObject>();
    	
    	Map<Integer,String> nerPosMap=new HashMap<Integer,String>();//用数字表示不同类型的实体
    	nerPosMap.put(0,"none");
    	nerPosMap.put(1,"nrf");
    	nerPosMap.put(2,"nrj");
    	nerPosMap.put(3,"nr");
    	nerPosMap.put(4,"ns");
    	nerPosMap.put(5,"nt");
    	
    	for(Term tm:resultList){
    		//循环第一次，按优先级整理每个词的实体标识
    		List<String> natureList=tm.natureList;
    		int level=0;
    		for(String nature:natureList){
    			if(nature.length()>=2){
	                if(nature.substring(0,2).contains("nr"))level=Math.max(level,3);
	                if(nature.substring(0,2).contains("ns"))level=Math.max(level,4);
	                if(nature.substring(0,2).contains("nt"))level=Math.max(level,5);
	                if(nature.length()>=3){
	                	if(nature.substring(0,3).contains("nrf"))level=Math.min(level,1);
	                	if(nature.substring(0,3).contains("nrj"))level=Math.min(level,2);
	                }
    			}
    		}
    		tm.nerPos=nerPosMap.get(level);
    	}
    	
    	int i=0;
		String nerPosNow="none";
    	for(Term tm:resultList){
    		//循环第二次，将所有的实体标识合并成实体
			if(!nerPosNow.equals(tm.nerPos)){
				nerPosNow=tm.nerPos;
				if(nerList.size()>0){
					if(nerList.get(nerList.size()-1).end<0){
						nerList.get(nerList.size()-1).end=i;
//    				    System.out.println(nerList.get(nerList.size()-1).nerFlag+"|"+nerList.get(nerList.size()-1).start+","+nerList.get(nerList.size()-1).end);
					}
				}
				if(!nerPosNow.equals("none")){
					String nerFlag="";
					if(tm.nerPos.equals("nrf")||tm.nerPos.equals("nrj")||tm.nerPos.equals("nr"))nerFlag="PER";
					if(tm.nerPos.equals("ns"))nerFlag="LOC";
					if(tm.nerPos.equals("nt"))nerFlag="ORG";
				    NerObject nerObj=new NerObject();
				    nerObj.nerFlag=nerFlag;
				    nerObj.start=i;
	    			nerList.add(nerObj);
				}
    		}
			if(i==resultList.size()-1&&nerList.size()>0){
				//在循环到最后一个term时，最近的一个实体要是还没有end，要强行end
			    if(nerList.get(nerList.size()-1).end<0){
			    	nerList.get(nerList.size()-1).end=i+1;
			    }
			}
    		i++;
    	}
    	
    	for(NerObject nerObj:nerList){
    		int start=(Integer)nerObj.start;
    		int end=(Integer)nerObj.end;
    		nerObj.nerName=getNerName(resultList,start,end);
    	}
    	
		return nerList;
    }
    
    /**
     * 根据范围数，截取在resultList中截取
     * 
     */
    private static String getNerName(List<Term> resultList,int start,int end){
    	String nerName="";
    	for(int i=start;i<end;i++){
    		nerName=nerName+resultList.get(i).word;
    	}
    	return nerName;
    }
    
    
    
    /**
    *将不同种类实体识别结果放到
    *@param resultList 存有带pos的分词结果
    *@param nerList 实体识别方法识别出的实体列表
    */
    public static void appendNERtoResultList(List<Term> resultList,List<Map> nerList){
    	for(Map nerMap:nerList){
    		int start=(Integer)nerMap.get("start");
    		int end=(Integer)nerMap.get("end");
    		String nerAttr=(String)nerMap.get("nature");
    		for(int i=start;i<end;i++){
    			resultList.get(i).natureList.add(nerAttr);
    		}
    	}
    }
    

    public static List<Vertex> viterbi(WordNet wordNet)
    {
        // 避免生成对象，优化速度
        LinkedList<Vertex> nodes[] = wordNet.getVertexes();
        LinkedList<Vertex> vertexList = new LinkedList<Vertex>();
        for (Vertex node : nodes[1])
        {
            node.updateFrom(nodes[0].getFirst());
        }
        for (int i = 1; i < nodes.length - 1; ++i)
        {
            LinkedList<Vertex> nodeArray = nodes[i];
            if (nodeArray == null) continue;
            for (Vertex node : nodeArray)
            {
                if (node.from == null) continue;
                for (Vertex to : nodes[i + node.realWord.length()])
                {
                    to.updateFrom(node);
                }
            }
        }
        Vertex from = nodes[nodes.length - 1].getFirst();
        while (from != null)
        {
            vertexList.addFirst(from);
            from = from.from;
        }
        return vertexList;
    }

    /**
     * 第二次维特比，可以利用前一次的结果，降低复杂度
     *
     * @param wordNet
     * @return
     */
//    private static List<Vertex> viterbiOptimal(WordNet wordNet)
//    {
//        LinkedList<Vertex> nodes[] = wordNet.getVertexes();
//        LinkedList<Vertex> vertexList = new LinkedList<Vertex>();
//        for (Vertex node : nodes[1])
//        {
//            if (node.isNew)
//                node.updateFrom(nodes[0].getFirst());
//        }
//        for (int i = 1; i < nodes.length - 1; ++i)
//        {
//            LinkedList<Vertex> nodeArray = nodes[i];
//            if (nodeArray == null) continue;
//            for (Vertex node : nodeArray)
//            {
//                if (node.from == null) continue;
//                if (node.isNew)
//                {
//                    for (Vertex to : nodes[i + node.realWord.length()])
//                    {
//                        to.updateFrom(node);
//                    }
//                }
//            }
//        }
//        Vertex from = nodes[nodes.length - 1].getFirst();
//        while (from != null)
//        {
//            vertexList.addFirst(from);
//            from = from.from;
//        }
//        return vertexList;
//    }
		private char[] getCharArr(List<String> strList){
			String charStr="";
			for(String s:strList){
				charStr=charStr+s;
			}
			return charStr.toCharArray();
		} 
        
		/**
		 * 将字符串型数组转化为列表
		 * */
        private List<String>  convertArrToList(String[] strArr){
        	List<String> strList=new ArrayList<String>();
        	for(String sa:strArr){
        		strList.add(sa);
        	}
        	return strList;
        }
    
    
}



