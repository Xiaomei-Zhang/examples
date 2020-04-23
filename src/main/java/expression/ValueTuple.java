package expression;

import lombok.Data;

@Data
public class ValueTuple {
	//can be person/project/team's id
	private String orgId;
	//for which time period this value is calculated
	private String forTimePeriod;
	private Object value;

}
