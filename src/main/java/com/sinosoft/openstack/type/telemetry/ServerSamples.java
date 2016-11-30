package com.sinosoft.openstack.type.telemetry;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ServerSamples {
	@JsonProperty("meter_name")
	private String meterName;
	@JsonProperty("time_series")
	private List<String> timeSeries;
	private List<Float> samples;
	
	public String getMeterName() {
		return meterName;
	}
	public void setMeterName(String meterName) {
		this.meterName = meterName;
	}
	public List<String> getTimeSeries() {
		return timeSeries;
	}
	public void setTimeSeries(List<String> timeSeries) {
		this.timeSeries = timeSeries;
	}
	public List<Float> getSamples() {
		return samples;
	}
	public void setSamples(List<Float> samples) {
		this.samples = samples;
	}
}
