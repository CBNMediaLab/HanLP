package com.hankcs.general;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hankcs.hanlp.seg.Segment;
import com.hankcs.hanlp.seg.Viterbi.ViterbiSegment;
import com.hankcs.hanlp.seg.common.Term;
import com.hankcs.hanlp.seg.common.Vertex;
import com.hankcs.hanlp.seg.common.WordNet;
import com.hankcs.hanlp.seg.common.NerObject;
import com.hankcs.hanlp.dictionary.CoreSynonymDictionary;


public class GeneralTest {
    public static void main(String[] args) {
	    ViterbiSegment vs=new ViterbiSegment();
	    //
	    String[] strArr={"日前","，","美国","前","总统","克林顿","安倍晋三","和","中国银监会","前主席","、","北京","大学","中国","经济","研究所","BCT","银联集团","杰出","研究员","刘","明","康","先生"
	    		,"在","上海","闵行","区","出席","了","由","上海","第一财经","研究院","举办","的","金融","论坛","并","探讨","了","在","2017","年","的","不确定性","中","寻找","哈佛大学"};
	    List<NerObject> nerList=vs.getNER(strArr);
	    for(NerObject nerObj:nerList){
	    	System.out.println(nerObj.nerName+","+nerObj.nerFlag+","+nerObj.start+","+nerObj.end);
	    }
	} 
}
