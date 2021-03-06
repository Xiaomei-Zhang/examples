import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import expression.ExpressionEvaluator;
import expression.GroovyExpressionEvaluator;
import expression.NTEObject;

/*
 * This Java source file was auto generated by running 'gradle init --type java-library'
 * by 'xiaomeizhang' at '3/29/20 9:17 PM' with Gradle 2.5-rc-2
 *
 * @author xiaomeizhang, @date 3/29/20 9:17 PM
 */

import lombok.extern.slf4j.Slf4j;
@Slf4j
public class LibraryTest {
    @Test public void testSomeLibraryMethod() {
    	ExpressionEvaluator evaluator = new GroovyExpressionEvaluator ();
    	
    	//把所有基础数据加到ScriptContext中
    	List<Double> va = new ArrayList<Double>();
    	va.add(1.23);
    	va.add(3.44);
    	evaluator.getEvaluationCtx().put("v_a", va);
    	evaluator.getEvaluationCtx().put("v_b", new NTEObject());
    	
    	Object result = evaluator.evaluate("sum(v_a)*v_b", "科目123");
    	
    	log.info("result={}", result);
    	
    	
    }
}
