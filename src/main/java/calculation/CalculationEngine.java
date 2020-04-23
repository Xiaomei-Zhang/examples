package calculation;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import excel.XlsxReader;
import expression.DataSheetWrapper;
import expression.ExpressionEvaluator;
import expression.Functions;
import expression.GroovyExpressionEvaluator;
import expression.NTEException;
import expression.NTEObject;
import expression.ValueTupleListWrapper;
import meta.DataItemDef;
import meta.ItemDef;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.xlsx.XlsxReadOptions;

public class CalculationEngine {
	private Map<String, DataItemDef> rawDataDefs;
	private Map<String, ItemDef> itemDefs;
	private Map<String, String> tableNameTitleMap;
	private ExpressionEvaluator evaluator;

	public CalculationEngine(Map<String, DataItemDef> rawDataDefs, Map<String, ItemDef> itemDefs) {
		this.rawDataDefs = rawDataDefs;
		populateTableNameLookup();
		this.itemDefs = itemDefs;
		initEvaluator();

	}

	private void populateTableNameLookup() {
		this.tableNameTitleMap = new HashMap<String, String>();
		for (DataItemDef def : this.rawDataDefs.values()) {
			tableNameTitleMap.put(def.getTable_title(), def.getTable());
		}

	}

	private void initEvaluator() {
		this.evaluator = new GroovyExpressionEvaluator();
		for (String itemName : itemDefs.keySet()) {
			evaluator.getEvaluationCtx().put(itemName, new NTEObject());
		}
	}

	public List<ItemValue> calculate(String timeFrame, Source source) {
		// put timeFrame, i.e 2020-1, 2020, 2020-1-12 into evaluation context
		evaluator.getEvaluationCtx().put("calculate_timeframe", timeFrame);
		Map<String, Object> itemVals = new HashMap<String, Object>();
		List<Table> tables;
		try {
			tables = new XlsxReader().readMultiple(XlsxReadOptions.builder(source).sheetIndex(0).build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// add raw data to calculation context
		for (Table table : tables) {
			// System.out.println(table);
			evaluator.getEvaluationCtx().put(this.tableNameTitleMap.get(table.name()),
					new DataSheetWrapper(table, this.rawDataDefs));
			System.out.println(String.format("add %s", this.tableNameTitleMap.get(table.name())));
		}

		int uncalculated = this.itemDefs.size();
		for (String itemName : this.itemDefs.keySet()) {
			itemVals.put(itemName, null);
		}
		while (uncalculated != 0) {
			System.out.println("To be evaluated:" + uncalculated);
			uncalculated = calculate_internal(evaluator, this.itemDefs, itemVals, uncalculated);
		}
		outputCalResult(itemVals);
		return new ArrayList(itemVals.values());
	}

	private void outputCalResult(Map<String, Object> itemVal) {
		List<List<ItemValue>> item_vals = new ArrayList<List<ItemValue>>(itemVal.size());
		for (String name : itemVal.keySet()) {
			item_vals.add(null);
		}
		for (String name : itemVal.keySet()) {
			ItemDef def = this.itemDefs.get(name);
			ItemValue val = new ItemValue();
			val.setDef(def);

			// round to 2
			Object obj = itemVal.get(name);
			if (obj instanceof BigDecimal) {
				obj = Functions.round((BigDecimal) obj, 2);
			} else if (obj instanceof Double) {
				obj = Functions.round(BigDecimal.valueOf((Double) obj), 2);
			}
			val.setVal(obj);
			List<ItemValue> vals = item_vals.get(def.getOrder() - 1);
			if (vals == null) {
				vals = new ArrayList<ItemValue>(2);
				vals.add(null);
				vals.add(null);
				item_vals.set(def.getOrder() - 1, vals);
			}
			if (def.getCalculate() == null || def.getCalculate().isEmpty()) {
				vals.add(0, val);
			} else
				vals.add(1, val);
		}
		System.out.println("---------------RESULT------------");
		for (List<ItemValue> vals : item_vals) {
			if (vals != null) {
				boolean hasPrintedTitle = false;
				for (ItemValue val : vals) {
					if (val != null) {
						if (!hasPrintedTitle) {
							System.out.print(String.format("%s\t :", val.getDef().getTitle()));
							hasPrintedTitle = true;
						}
						System.out.print(String.format("%s\t", val.getVal()));
					}
				}
				System.out.print("\n");
			}

		}
	}

	private int calculate_internal(ExpressionEvaluator evaluator, Map<String, ItemDef> itemMetaMap,
			Map<String, Object> itemVal, int uncalculated) {
		int count = uncalculated;
		for (String name : itemVal.keySet()) {
			if (itemVal.get(name) == null || itemVal.get(name) instanceof NTEObject) {
				String exp = itemMetaMap.get(name).getExpression();
				System.out.println("evaluating " + name + "=" + exp);
				try {
					Object ret = evaluator.evaluate(exp, name);
					if (ret != null && !(ret instanceof NTEObject)) {
						System.out.println("科目" + name + ":" + ret);
						evaluator.getEvaluationCtx().put(name, ret);
						if (ret instanceof ValueTupleListWrapper)
							ret = ((ValueTupleListWrapper) ret).getWrappedVal();
						itemVal.put(name, ret);
						count--;
					}
				} catch (NTEException e) {
					;
				}
			}
		}
		if (count == uncalculated) {
			outputCalResult(itemVal);
			throw new RuntimeException("calculation doesn't go forward");
		}
		return count;

	}

}
