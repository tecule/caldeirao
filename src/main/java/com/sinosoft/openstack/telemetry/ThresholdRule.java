package com.sinosoft.openstack.telemetry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ThresholdRule {
	// exclude_outliers

	@JsonProperty("meter_name")
	private String meterName;
	@JsonProperty("comparison_operator")
	private String comparisonOperator;
	private float threshold;
	private String statistic;
	private int period;
	@JsonProperty("evaluation_periods")
	private int evaluationPeriods;
	private List<Query> query;
	
	public String getMeterName() {
		return meterName;
	}
	public void setMeterName(String meterName) {
		this.meterName = meterName;
	}
	
	public String getComparisonOperator() {
		return comparisonOperator;
	}
	public void setComparisonOperator(String comparisonOperator) {
		this.comparisonOperator = comparisonOperator;
	}
	
	public float getThreshold() {
		return threshold;
	}
	public void setThreshold(float threshold) {
		this.threshold = threshold;
	}
	
	public String getStatistic() {
		return statistic;
	}
	public void setStatistic(String statistic) {
		this.statistic = statistic;
	}
	
	public int getPeriod() {
		return period;
	}
	public void setPeriod(int period) {
		this.period = period;
	}
	
	public int getEvaluationPeriods() {
		return evaluationPeriods;
	}
	public void setEvaluationPeriods(int evaluationPeriods) {
		this.evaluationPeriods = evaluationPeriods;
	}
	
	public List<Query> getQuery() {
		return query;
	}
	public void setQuery(List<Query> query) {
		this.query = query;
	}
}
