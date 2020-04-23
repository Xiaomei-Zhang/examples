package expression;

public class ExpressionEvaluationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public ExpressionEvaluationException (String msg, Exception e) {
		super (msg, e);
	}
	public ExpressionEvaluationException (String msg) {
		super(msg);
	}

}
