package cn.edu.zju.lau.cminer.model;

/**
 * 关联规则对象：
 * 		history -> prediction，并包含其发生的权重confidence。
 * 		closed frequent subsequence拆分为history和prediction，它们也都是frequent subsequence。
 * 
 * @author yuki lau
 * @date 2013-11-07
 */
public class Rule {
	
	private String history;
	private String prediction;
	private int support;		// 可以生成该规则的所有closed frequent subsequence中的最大support
	private float confidence;	// support(prediction)/support(history)
	
	public Rule(){
		
	}
	
	public Rule(String history, String prediction, int support, float confidence){
		this.history = history;
		this.prediction = prediction;
		this.support = support;
		this.confidence = confidence;
	}

	public String toString(){
		
		StringBuilder sb = new StringBuilder("{");
		sb.append("rule=").append(history).append("->").append(prediction);
		sb.append(", support=").append(support);
		sb.append(", confidence=").append(confidence);
		sb.append("}");
		
		return sb.toString();
	}
	
	public String getHistory() {
		return history;
	}

	public void setHistory(String history) {
		this.history = history;
	}

	public String getPrediction() {
		return prediction;
	}

	public void setPrediction(String prediction) {
		this.prediction = prediction;
	}

	public int getSupport() {
		return support;
	}

	public void setSupport(int support) {
		this.support = support;
	}

	public float getConfidence() {
		return confidence;
	}

	public void setConfidence(float confidence) {
		this.confidence = confidence;
	}
}
