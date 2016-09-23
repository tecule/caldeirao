package com.sinosoft.openstack.telemetry;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;


//  {
//	  "alarm_actions": [
//	    "log://"
//	  ],
//	  "ok_actions": [],
//	  "name": "cpu_hi2",
//	  "severity": "low",
//	  "timestamp": "2016-09-13T06:45:14.909554",
//	  "enabled": true,
//	  "state": "insufficient data",
//	  "state_timestamp": "2016-09-13T06:45:14.909554",
//	  "threshold_rule": {
//	    "meter_name": "cpu_util",
//	    "evaluation_periods": 1,
//	    "period": 60,
//	    "statistic": "avg",
//	    "threshold": 60.0,
//	    "query": [
//	      {
//	        "field": "resource_id",
//	        "value": "e986ad24-54a0-4dcb-9047-fd90af5f6317",
//	        "op": "eq"
//	      }
//	    ],
//	    "comparison_operator": "ge",
//	    "exclude_outliers": false
//	  },
//	  "alarm_id": "6a4fb93e-30bd-4804-a303-f29e541ff629",
//	  "time_constraints": [],
//	  "insufficient_data_actions": [],
//	  "repeat_actions": false,
//	  "user_id": "99e8e45981344fffaaa537082f6ee973",
//	  "project_id": "e3eca810b4d94dffbe21ff20a7ed138e",
//	  "type": "threshold",
//	  "description": "cpu_hi_desc"
//	}

@JsonIgnoreProperties(ignoreUnknown = true)
public class Alarm {
	// ok_actions
	// insufficient_data_actions
	// repeat_actions
	// time_constraints

	@JsonProperty("alarm_id")
	private String alarmId;
	private String name;
	private String description;
	private boolean enabled;
	private String type;
	private String severity;
	private String state;
	@JsonProperty("threshold_rule")
	private ThresholdRule thresholdRule;
	@JsonProperty("alarm_actions")
	private List<String> alarmActions;
	private Date timestamp;
	@JsonProperty("state_timestamp")
	private Date stateTimestamp;
	@JsonProperty("user_id")
	private String userId;
	@JsonProperty("project_id")
	private String projectId;

	public String getAlarmId() {
		return alarmId;
	}
	public void setAlarmId(String alarmId) {
		this.alarmId = alarmId;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}

	public String getSeverity() {
		return severity;
	}
	public void setSeverity(String severity) {
		this.severity = severity;
	}
	
	public String getState() {
		return state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public ThresholdRule getThresholdRule() {
		return thresholdRule;
	}
	public void setThresholdRule(ThresholdRule thresholdRule) {
		this.thresholdRule = thresholdRule;
	}

	public List<String> getAlarmActions() {
		return alarmActions;
	}
	public void setAlarmActions(List<String> alarmActions) {
		this.alarmActions = alarmActions;
	}
	
	public Date getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public Date getStateTimestamp() {
		return stateTimestamp;
	}
	public void setStateTimestamp(Date stateTimestamp) {
		this.stateTimestamp = stateTimestamp;
	}

	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getProjectId() {
		return projectId;
	}
	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}
}
