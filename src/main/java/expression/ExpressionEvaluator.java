package expression;


import java.util.Map;

public interface ExpressionEvaluator {
	void setEvaluationCtx(Map<String, Object> evaluationCtx);
	Object evaluate(String expression, String location);
	Map<String, Object> getEvaluationCtx();
	@SuppressWarnings("rawtypes")
	Class parseGroovyClass(String body, String name);
}
