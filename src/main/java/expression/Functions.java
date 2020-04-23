package expression;

import static tech.tablesaw.aggregate.AggregateFunctions.sum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import meta.DataItemDef;
import tech.tablesaw.api.ColumnType;
import tech.tablesaw.api.DateTimeColumn;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.NumericColumn;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import tech.tablesaw.selection.Selection;

public class Functions {
	/**
	 * sum - 对原始数据项（对应于原始数据表中的一列）求和
	 * @param col - 原始数据表（比如“人员数据-HR”）中的一列求和
	 * @return 求和后的累计值
	 */
	public static Double sum(NumericColumn col) {
		return col.sum();
	}

	/**
	 * sumif - 对原始数据项（对应于原始数据表中的一列）先过滤再求和
	 *         过滤条件的表达式的格式是：列名 操作符 值
	 * @param table - 原始数据处理表
	 * @param col_name - 过滤条件原始数据表的列名
	 * @param operator - 操作符
	 * @param val - 值
	 * @param sum_col_name - 做求和操作对应的列名
	 * @return 求和后的累计值
	 */
	public static Double sumif(DataSheetWrapper table, String col_name, String operator, Object val,
			String sum_col_name) {
		String sum_title = table.getDef().get(sum_col_name).getTitle();
		Selection sel = getSelection(table, col_name, operator, val);
		if (sel != null) {
			Table t = ((Table) table.unwrap(table)).where(sel);
			return t.numberColumn(sum_title).sum();
		}
		return new Double(0);
	}

	/**
	 * join 
	 * @param table1： 原始数据表1
	 * @param col_name1： 原始数据表1中使用的关联列的名字
	 * @param table2：原始数据表2
	 * @param col_name2: 原始数据表2中使用的关联列的名字
	 * @return 关联后的表
	 */
	public static DataSheetWrapper join(DataSheetWrapper table1, String col_name1, DataSheetWrapper table2,
			String col_name2) {
		String tile_name1 = table1.getDef().get(col_name1).getTitle();
		String tile_name2 = table2.getDef().get(col_name2).getTitle();
		Table t1 = (Table) DataSheetWrapper.unwrap(table1);
		Table t2 = (Table) DataSheetWrapper.unwrap(table2);
		return (DataSheetWrapper) DataSheetWrapper.wrap(t1.joinOn(tile_name1).inner(t2, tile_name2, true),
				table1.getDef());
	}

	private static Selection getSelection(DataSheetWrapper table, String col_name, String operator, Object val) {
		Selection sel = null;
		String title = table.getDef().get(col_name).getTitle();
		if ("=".equals(operator)) {
			sel = ((Table) table.unwrap(table)).stringColumn(title).isEqualTo((String) val);
		} else if ("within".equals(operator)) {
			Column col = ((Table) table.unwrap(table)).column(title);
			if (col.type() == ColumnType.LOCAL_DATE_TIME) {
				Predicate<LocalDateTime> isInMonthYear = new Predicate<LocalDateTime>() {

					@Override
					public boolean test(LocalDateTime value) {
						String timeframe = val.toString();
						return value != null && (timeframe.equals(String.valueOf(value.getYear()))
								|| timeframe
										.equals(String.format("%s-%s", value.getYear(), value.getMonth().getValue()))
								|| timeframe.equals(String.format("%s-%s-%s", value.getYear(), value.getMonthValue(),
										value.getDayOfMonth())));
					}
				};
				sel = ((DateTimeColumn) col).eval(isInMonthYear);

			}
		}
		return sel;
	}

	/**
	 * count - 原始数据项对应的行数
	 * @param col - 原始数据项对应的列
	 * @return 行数
	 */
	public static Integer count(Column col) {
		return col.size();
	}

	/**
	 * countif - 原始数据表中符合过滤条件的行的总数
	 *            过滤条件的表达式的格式是：列名 操作符 值 
	 * @param table - 原始数据表
	 * @param col_name - 过滤条件中的列名
	 * @param operator - 过滤条件中的操作符
	 * @param val - 过滤条件中的值
	 * @param count_col_name - 计数列的行数
	 * @return 总数
	 */
	public static Integer countif(DataSheetWrapper table, String col_name, String operator, Object val,
			String count_col_name) {
		Selection sel = getSelection(table, col_name, operator, val);
		String count_title = table.getDef().get(count_col_name).getTitle();
		if (sel != null) {
			Table t = ((Table) table.unwrap(table)).where(sel);
			return count(t.column(count_title));
		}
		return 0;
	}

	/**
	 * iffunc - 按条件评估表达式
	 * @param condition - 条件
	 * @param valForTrue - 条件为真时评估的表达式
	 * @param valForFalse - 条件为假时评估的表达式
	 * @return 表达式评估后的结果
	 */
	public static Object iffunc(Boolean condition, Object valForTrue, Object valForFalse) {
		if (condition) {
			return valForTrue;
		} else {
			return valForFalse;
		}
	}

	/**
	 * round 
	 * @param d - 待四舍五入的数字
	 * @param digits - 四舍五入需要保留的位数
	 * @return 四舍五入后的结果
	 */
	public static Double round(BigDecimal d, int digits) {
		return d.setScale(digits, RoundingMode.HALF_UP).doubleValue();
	}

	/**
	 * sumby - 分堆累计
	 * @param table - 数据表
	 * @param groupByColName - 分堆基于的列
	 * @param sumColName - 累计的列
	 * @param defaultTimeStr - 缺省时间标示
	 * @return 累计的结果
	 */
	public static List<ValueTuple> sumby(DataSheetWrapper table, String groupByColName, String sumColName, String defaultTimeStr) {
		List<ValueTuple> ret = new ArrayList<ValueTuple>();
		Table t = (Table) ((DataSheetWrapper) table).unwrap(table);
		Map<String, DataItemDef> dataItemDefs = ((DataSheetWrapper) table).getDef();
		DataItemDef groupByDef = dataItemDefs.get(groupByColName);
		if (groupByDef == null)
			throw new ExpressionEvaluationException("invalid parameter " + groupByColName);
		Table resultTable = t.summarize(sumColName, sum).by(groupByDef.getTitle());
		for (int i = 0; i < resultTable.rowCount(); i++) {
			Row r = resultTable.row(i);
			ValueTuple valueTuple = new ValueTuple();
			DataItemDef orgColDef = findOrgColDef(resultTable.columnNames(), dataItemDefs);
			DataItemDef dateColDef = findDateColDef(resultTable.columnNames(), dataItemDefs);
			if (orgColDef != null) {
				if (orgColDef.getType().equals("People")) {
					valueTuple.setOrgId(String.valueOf(r.getInt(orgColDef.getTitle())));
				} else {
					valueTuple.setOrgId(String.valueOf(r.getString(orgColDef.getTitle())));
				}
			}
			String timeStr = defaultTimeStr;
			if (dateColDef != null) 
				timeStr = r.getString(dateColDef.getTitle());
			valueTuple.setForTimePeriod(timeStr);
			valueTuple.setValue(r.getObject(String.format("Sum [%s]", sumColName)));
			if (!(valueTuple.getOrgId() != null && valueTuple.getOrgId().equals("-")))
				ret.add(valueTuple);
		}
		return ret;
	}

	private static DataItemDef findDateColDef(List<String> columnTitles, Map<String, DataItemDef> dataItemDefs) {
		for (String columnTitle : columnTitles) {
			DataItemDef def = findDefByTitle(columnTitle, dataItemDefs);
			if (def != null && (def.getType().equals("WDate") || def.getType().equals("WMonth"))) {
				return def;
			}
		}
		return null;
	}

	private static DataItemDef findDefByTitle(String columnTitle, Map<String, DataItemDef> dataItemDefs) {
		for (DataItemDef def : dataItemDefs.values()) {
			if (def != null && def.getTitle().equals(columnTitle)) {
				return def;
			}
		}
		return null;
	}

	private static DataItemDef findOrgColDef(List<String> columnTitles, Map<String, DataItemDef> dataItemDefs) {
		for (String columnTitle : columnTitles) {
			DataItemDef def = findDefByTitle(columnTitle, dataItemDefs);
			if (def != null && (def.getType().equals("People") || def.getType().equals("Team"))) {
				return def;
			}
		}
		return null;
	}

	/**
	 * expandPerson - 将两个和人员有关的表（物理原始数据表或是在内存中的表)做关联
	 * @param t1 - 表1
	 * @param t2 - 表2
	 * @return 关联后的表
	 */
	public static DataSheetWrapper associateByPerson(Object t1, Object t2) {
		Table table1 = null;
		Table table2 = null;
		String joinKey1 = null;
		String joinKey2 = null;
		Map<String, DataItemDef> defs = null;
		if ((t1 instanceof DataSheetWrapper || t1 instanceof ValueTupleListWrapper)
				&& (t2 instanceof DataSheetWrapper || t2 instanceof ValueTupleListWrapper)) {
			if (t1 instanceof ValueTupleListWrapper) {
				table1 = convertToTable((ValueTupleListWrapper) t1);
				joinKey1 = "employee_id";
				defs = ((ValueTupleListWrapper) t1).getDataItemDefs();
			} else {
				table1 = (Table) DataSheetWrapper.unwrap(t1);
				joinKey1 = getPersonColumnName(((DataSheetWrapper) t1).getDef(), table1.name());
				defs = ((DataSheetWrapper) t1).getDef();
			}
			if (t2 instanceof ValueTupleListWrapper) {
				table2 = convertToTable((ValueTupleListWrapper) t2);
				joinKey2 = "employee_id";
			} else {
				table2 = (Table) DataSheetWrapper.unwrap(t2);
				joinKey2 = getPersonColumnName(((DataSheetWrapper) t2).getDef(), table2.name());
			}
			return new DataSheetWrapper(table1.joinOn(joinKey1).inner(table2, joinKey2, true), defs);

		}
		throw new ExpressionEvaluationException("Invalid parameters");
	}

	private static Table convertToTable(ValueTupleListWrapper t1) {
		List<ValueTuple> values = ((ValueTupleListWrapper) t1).getWrappedVal();
		Map<String, DataItemDef> dataItemDefs = ((ValueTupleListWrapper) t1).getDataItemDefs();
		Table t = Table.create();
		t.addColumns(IntColumn.create("employee_id"), StringColumn.create("wdate"), DoubleColumn.create("value"));
		for (ValueTuple tuple : ((ValueTupleListWrapper) t1).getWrappedVal()) {
			Row r = t.appendRow();
			if (tuple.getOrgId() != null)
				r.setInt("employee_id", Integer.valueOf(tuple.getOrgId()).intValue());
			if (tuple.getForTimePeriod() != null)
				r.setString("wdate", tuple.getForTimePeriod());
			r.setDouble("value", ((BigDecimal) tuple.getValue()).doubleValue());
		}
		return t;
	}

	/**
	 * iteratePerRow - 对数据表中的每一行按给出的表达式做评估
	 * @param table - 数据表
	 * @param expression - 表达式
	 * @return 一个在内存中的表结构，其中包括原表中可能有的key 字段： 组织的标示（项目，班组或是个人）， 时间的标示，和“value” 字段（表达式评估的结果）。 
	 *         如果原表中没有组织的标示的字段，对应的属性就不设置；如果原表中没有时间标示的字段，对应的属性就设置成缺省的此次计算对应的时间批次
	 */
	public static ValueTupleListWrapper iteratePerRow(DataSheetWrapper table, String expression, String defaultTimeStr) {
		List<ValueTuple> ret = new ArrayList<ValueTuple>();
		ExpressionEvaluator evaluator = new GroovyExpressionEvaluator();
		Table t = (Table) table.unwrap(table);
		for (int i = 0; i < t.rowCount(); i++) {
			Row r = t.row(i);
			evaluator.getEvaluationCtx().put("row", new RowWrapper(r, table.getDef()));
			Object v = evaluator.evaluate(expression, (new Date()).toString());
			ValueTuple valueTuple = constructTuple(table, r, v, defaultTimeStr);
			ret.add(valueTuple);
		}

		return new ValueTupleListWrapper(ret, table.getDef());
	}

	private static ValueTuple constructTuple(DataSheetWrapper table, Row r, Object v, String defaultTimeStr) {
		Map<String, DataItemDef> dataItemDefs = table.getDef();
		String personColName = getPersonColumnName(dataItemDefs, ((Table) table.unwrap(table)).name());
		String dateKeyColName = getDateKeyColumnName(dataItemDefs, ((Table) table.unwrap(table)).name());
		ValueTuple vTuple = new ValueTuple();
		String dateKey = defaultTimeStr;
		if (personColName != null && !personColName.isEmpty()) {
			int personKey = r.getInt(personColName);
			vTuple.setOrgId(String.valueOf(personKey));
		}
		if (dateKeyColName != null && !dateKeyColName.isEmpty()) {
			dateKey = r.getString(dateKeyColName);
		}
		vTuple.setForTimePeriod(dateKey);
		vTuple.setValue(v);
		return vTuple;
	}

	private static String getDateKeyColumnName(Map<String, DataItemDef> dataItemDefs, String tableTitle) {
		for (DataItemDef def : dataItemDefs.values()) {
			if (tableTitle.equals(def.getTable_title())
					&& (def.getType().equals("WDate") || def.getType().equals("WMonth"))) {
				return def.getTitle();
			}
		}
		return null;
	}

	private static String getPersonColumnName(Map<String, DataItemDef> dataItemDefs, String tableTitle) {
		if ("人员数据-HR".equals(tableTitle)) {
			return "公司工号";
		}
		for (DataItemDef def : dataItemDefs.values()) {
			if (tableTitle.equals(def.getTable_title()) && def.getType().equals("People")) {
				return def.getTitle();
			}
		}
		return null;
	}
}
