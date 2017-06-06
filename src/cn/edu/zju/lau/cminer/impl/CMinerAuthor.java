package cn.edu.zju.lau.cminer.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import cn.edu.zju.lau.cminer.CMinerBase;
import cn.edu.zju.lau.cminer.model.Rule;
import cn.edu.zju.lau.cminer.model.SubsequenceSuffix;

/**
 * 挖掘序列中事件（字符）的关联关系
 * 实现《C-Miner: Mining Block Correlations in Storage Systems》中所述的C-Miner算法结果，
 * 使用作者的Core Algorithm 生成 Candidate Frequent Subsequences：DFS算法。
 * 
 * @author yuki lau
 * @date 2013-11-16
 */

public class CMinerAuthor extends CMinerBase {

	// 存储frequent subsequence在每个segments中的最长suffix
	private Map<String, SubsequenceSuffix> Ds;
	private Stack<SubsequenceSuffix> stk;
	
	public CMinerAuthor(){
		super();
		Ds = new HashMap<String,SubsequenceSuffix>();
		stk = new Stack<SubsequenceSuffix>();
	}
	
	public CMinerAuthor(String inputSequence, int windowSize, int maxGap, int minSupport, float minConfidence){
		super(inputSequence, windowSize, maxGap, minSupport, minConfidence);
		Ds = new HashMap<String,SubsequenceSuffix>();
		stk = new Stack<SubsequenceSuffix>();
	}
	
	@Override
	public void candidateFreSubsequences() {
		System.out.println("生成候选频繁子序列的DFS方法需要参数，抽象接口没定义好，哎~ ");
		System.out.println("请调用有参数的同名方法。");
	}
	
	@Override
	public Map<String, Rule> startMining() {
		// 对初始访问序列分段
		cutAccessSequence();
		
		// 获取长度为1的频繁序列
		generateFirstDs();
				
		// mining the frequent subsequence
		// choose a candidate frequent subseqence
		//SubsequenceSuffix ss = getSeqFromDs();//get the first sub-sequence from Ds
		//if(ss!=null){
		//	stk.push(ss);
		//}
		myCandidateFreSubsequences();
			// 过滤：Closed频繁子序列
		closedFreSubsequences();
			// 生成：关联规则
		generateRules();
		
		return rules;
	}
	


	@Override
	public void clear() {
		super.clear();
		Ds.clear();
		stk.clear();
	}
	

	/**
	 * 获取长度为1的频繁序列，以及各个频繁子序列的后缀集合
	 * 
	 *return: Map<String, SubsequenceSuffix> Ds
	 */
	public void generateFirstDs(){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		
		//count the appearing times of each char and record its suffix
		for(int i = 0; i < inputSegments.size();i++){
			String segment = inputSegments.get(i);
			//each segment
			for(int k = 0; k < segment.length(); k++){
				String currentChar = segment.substring(k, k + 1);
				
				//the count of current char 
				Integer count = charAccessTimes.get(currentChar) == null ? 0 : charAccessTimes.get(currentChar);
				charAccessTimes.put(currentChar, count + 1);
				
				// the start position of current char
				int start = segment.indexOf(currentChar);
				if(start != k || start == segment.length() - 1){//current char should start at the k position and not the last position
				       //skip some chars which have appeared
					continue;
				}
				if(Ds.get(currentChar) == null){//the first time of current char appearing
					Ds.put(currentChar, new SubsequenceSuffix());
				}
				Ds.get(currentChar).addSuffix(segment.substring(start + 1));//add the suffix into Ds
			}
		}
		
		//each sub-sequence should appaear at least minSupport
		for(Map.Entry<String, Integer> entry: charAccessTimes.entrySet()){
			if(entry.getValue() < minSupport){
				System.out.println("DEBUG Remove: " + entry.getKey()+" appears " + entry.getValue());
				Ds.remove(entry.getKey());//remove those subsequence whose support less than minSupport
			}
			else{
				System.out.println("DEBUG Add: " + entry.getKey()+" appears " + entry.getValue());
				Ds.get(entry.getKey()).setOccurTimes(entry.getValue());
				Ds.get(entry.getKey()).setSubsequence(entry.getKey());
				//push it into stack
				System.out.println("DEBUG Push " + entry.getKey() + " into stack ");
				stk.push(Ds.get(entry.getKey()));
			}
		}
	}

	/**
	 * 抽象方法。DFS 产生候选频繁子序列集合（Frequent Subsequences），满足：
	 * 		1）相距不大于maxGap的访问子序列（没必要连续）
	 * 		2）出现次数满足frequent要求，即不小于minSupport
	 * 
	 * return:
	 *       Map<String, Integer> freSubsequences
	 * 		Map<Integer, Map<String, Integer>> freSubsequencesTier;
	 */
	public void myCandidateFreSubsequences(){
		while(!stk.empty( )){
			SubsequenceSuffix cur = stk.pop();
			String currentSubseq=cur.getSubsequence();
			int occurTimes=cur.getOccurTimes();

			System.out.println("DEBUG Pop " + currentSubseq + " from stack ");
				//-------------------------------------------------------------//
				//part I:
				//add currentSubseq into frequent candidate subseqence
				freSubsequences.put(currentSubseq, occurTimes);
				
				//add it into freSubsequencesTier
				int seqLen = currentSubseq.length();
				if(seqLen > maxSeqLength){
					maxSeqLength = seqLen;
				}
				if(freSubsequencesTier.get(seqLen) == null){
					freSubsequencesTier.put(seqLen, new HashMap<String, Integer>());
				}
				freSubsequencesTier.get(seqLen).put(currentSubseq, occurTimes);
		
		
				//-------------------------------------------------------------//
				//part II:
				// get all suffixes of currentSubseq
				Set<String> currentSuffixDs = Ds.get(currentSubseq).getSuffixes();
				//remove currentSubseq from Ds after procession
				Ds.remove(currentSubseq);
		
				// extend one char which appears at least minSupport times
				// from current the suffix set
				Set<String> oneCharFreSubseqs = generateOneCharFreSubseq(currentSuffixDs);
				// each char which appears frequently will be combined with currentSubseq
				for(String alpha: oneCharFreSubseqs){
					// don't consider "AA" pattern
					if(currentSubseq.endsWith(alpha)){
						continue;
					}
					
					// extend "currentSubseq+alpha" and
					// check the new extended subsequence is frequent or not
					// if yes, then generate the new suffix for the new freqent subseqence
					String newSubseq = currentSubseq + alpha;
					Set<String> newSuffixDs = new HashSet<String>();
					int cnt = 0;
					for(String suffix: currentSuffixDs){
						int position = suffix.indexOf(alpha);
						//System.out.println("DEBUG extend a char: " + alpha + " at position " + position);
		
						if(position == suffix.length() - 1 &&  position <= maxGap){//
							cnt++;
						}
						else if(position >= 0 && position <= maxGap){//the new suffix is extented within the limiit gap
							cnt++;
							newSuffixDs.add(suffix.substring(position + 1));//add the suffix into the set 
						}
						else{
							//skip
						}
					}
					
					//generate the new subsequence if its apearance time >= minSupport
					if(cnt>= minSupport){
						//put the new suffix set into Ds
						SubsequenceSuffix newSubseqSuffix =  new SubsequenceSuffix(newSubseq, cnt, newSuffixDs);
						Ds.put(newSubseq, newSubseqSuffix);
						
						System.out.println("DEBUG push " + newSubseq+ " into stack ");
						//push it into stack
						stk.push(newSubseqSuffix);
					}
				}
		}

	}


	public void candidateFreSubsequences(String currentSubseq, int occurTimes){
			//System.out.println("-------------begin processing a subsequence------------------");
			//add currentSubseq into frequent candidate subseqence
			freSubsequences.put(currentSubseq, occurTimes);
			
			//add it into freSubsequencesTier
			int seqLen = currentSubseq.length();
			if(seqLen > maxSeqLength){
				maxSeqLength = seqLen;
			}
			if(freSubsequencesTier.get(seqLen) == null){
				freSubsequencesTier.put(seqLen, new HashMap<String, Integer>());
			}
			freSubsequencesTier.get(seqLen).put(currentSubseq, occurTimes);
			
			// get all suffixes of currentSubseq
			Set<String> currentDs = Ds.get(currentSubseq).getSuffixes();
			
			// extend one char which appears at least minSupport times
			// from current the suffix set
			Set<String> oneCharFreSubseqs = generateOneCharFreSubseq(currentDs);
	
			//remove currentSubseq from Ds after procession
			Ds.remove(currentSubseq);
	
			
			// each char which appears frequently will be combined with currentSubseq
			for(String alpha: oneCharFreSubseqs){
				
				// don't consider "AA" pattern
				if(currentSubseq.endsWith(alpha)){
					continue;
				}
				
				// extend "currentSubseq+alpha" and
				// check the new extended subsequence is frequent or not
				// if yes, then generate the new suffix for the new freqent subseqence
				String newSeq = currentSubseq + alpha;
				Set<String> newSuffixDs = new HashSet<String>();
				int cnt = 0;
				for(String suffix: currentDs){
					int position = suffix.indexOf(alpha);
					System.out.println("DEBUG extend a char: " + alpha + " at position " + position);
	
					if(position == suffix.length() - 1 &&  position <= maxGap){//
						cnt++;
					}
					else if(position >= 0 && position <= maxGap){//the new suffix is extented within the limiit gap
						cnt++;
						newSuffixDs.add(suffix.substring(position + 1));//add the suffix into the set 
					}
					else{
						//skip
					}
				}
				
				//generate the new subsequence if its apearance time >= minSupport
				if(cnt>= minSupport){
					//put the new suffix set into Ds
					Ds.put(newSeq, new SubsequenceSuffix(newSeq, cnt, newSuffixDs));
					
					System.out.println("DEBUG remove a subsequence: " + currentSubseq + " and recurively call for new subsequence  "+ newSeq);
					// remove currentSubseq from Ds after procession
					//Ds.remove(currentSubseq);
					// recurisively call
					candidateFreSubsequences(newSeq, cnt);
				}
			}
			
			//finish processing currentSubseq
			//Ds.remove(currentSubseq);
			//System.out.println("-------------end processing a subsequence------------------");
			
			//next 
			SubsequenceSuffix nextSeq = getSeqFromDs();
			if(nextSeq != null){
				candidateFreSubsequences(nextSeq.getSubsequence(), nextSeq.getOccurTimes());
			}
		}

	
	
	/**
	 * 从Ds集合中挑选出一个子序列，作为处理使用
	 */
	public SubsequenceSuffix getSeqFromDs(){
		if(Ds.isEmpty()){
			return null;
		}
		return Ds.entrySet().iterator().next().getValue();
	}
	
	/**
	 * 从输入的segments中计算出长度为1的频繁子序列集合，并返回
	 */
	private Set<String> generateOneCharFreSubseq(Collection<String> segments){
		
		Map<String, Integer> charAccessTimes = new HashMap<String, Integer>();
		Set<String> oneCharFreSubseqs = new HashSet<String>();
		
		// 统计每个字符出现的次数
		for(String segment: segments){
			for(int k = 0; k < segment.length(); k++){
				String currentChar = segment.substring(k, k + 1);
				
				// 统计每个字符出现的次数
				Integer count = charAccessTimes.get(currentChar) == null ? 0 : charAccessTimes.get(currentChar);
				charAccessTimes.put(currentChar, count + 1);
			}
		}
		
		// 过滤掉出现次数小于minSupport的子序列
		for(Map.Entry<String, Integer> entry: charAccessTimes.entrySet()){
			if(entry.getValue() >= minSupport){
				oneCharFreSubseqs.add(entry.getKey());
			}
		}
		
		return oneCharFreSubseqs;
	}

}
