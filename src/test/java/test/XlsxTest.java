/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;

import calculation.CalculationEngine;
import calculation.ItemValue;
import excel.XlsxReader;
import meta.DataItemDef;
import meta.ItemDef;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.io.Source;
import tech.tablesaw.io.xlsx.XlsxReadOptions;

public class XlsxTest {

	@Test
	public void test() throws IOException {
		File file = new File("testdata/dataimport.xlsx");
		Source source = new Source(file);
		Map<String, DataItemDef> dataItemDefs = readRawDataDefs();
		Map<String, ItemDef> itemDefs = readItemDefs();
		validateReportsDef(itemDefs, dataItemDefs);
		 CalculationEngine engine = new CalculationEngine(dataItemDefs,
		 itemDefs);
		 List<ItemValue> vals = engine.calculate(LocalDateTime.of(2020,1,15,0,0).toString(),source);

	}

	public void validateReportsDef(Map<String, ItemDef> itemDefs, Map<String, DataItemDef> dataItemDefs)
			throws IOException {
		List<String> errorMsg = new ArrayList<String>();
		Table report_def_table;
		try {
			report_def_table = new XlsxReader()
					.read(XlsxReadOptions.builder("testdata/view.xlsx").sheetIndex(0).build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Map<String, String> parentReportMap = new HashMap<String, String>();
		List<String> reportsList = new ArrayList<String>();

		for (int i = 0; i < report_def_table.rowCount(); i++) {
			Row row = report_def_table.row(i);
			String name = row.getString("name");
			reportsList.add(name);
		}
		for (int i = 0; i < report_def_table.rowCount(); i++) {
			Row row = report_def_table.row(i);
			String name = row.getString("name");
			String itemsStr = row.getString("items");
			String drilldownStr = row.getString("drill");
			Map<String, String> drilldown = null;
			if (drilldownStr != null && !drilldownStr.trim().isEmpty()) {
				try {
					drilldown = JSON.parseObject(drilldownStr, Map.class);
				} catch (JSONException e) {
					System.out.println("JSONException on '" + drilldownStr + "'");
				}
			}

			String[] items = itemsStr.split(",");

			if (items.length > 0) {
				errorMsg.addAll(validateItems(name, items, itemDefs, dataItemDefs));
				if (drilldown != null)
					errorMsg.addAll(validateDrill(report_def_table, name, drilldown, items, itemDefs, dataItemDefs,
							reportsList, parentReportMap));
			}


		}
		if (reportsList.size() > 3) {
			for (String report : reportsList) {
				if (!parentReportMap.containsKey(report)) {
					String e = String.format("Report %s is not used", report);
					errorMsg.add(e);

				}
			}
		}
		if (errorMsg.size() > 1) {
			for (String e : errorMsg) {
				System.out.println(e);
			}
		}

	}

	private List<String> validateDrill(Table table, String name, Map<String, String> drilldown, String[] items,
			Map<String, ItemDef> itemDefs, Map<String, DataItemDef> dataItemDefs, List<String> reportList,
			Map<String, String> parentReportMaps) {
		List<String> errors = new ArrayList<String>();
		for (String key : drilldown.keySet()) {
			boolean bValidItem = false;
			if ("*".equals(key)) {
				bValidItem = true;
			} else {
				for (String item : items) {
					if (key.trim().equals(item.trim())) {
						bValidItem = true;
					}
				}
			}
			if (!bValidItem) {
				String error = String.format("Report %s 'drill'中key %s没有在'items'定义中存在", name, key);
				errors.add(error);
			} else {
				String drill_to_report = drilldown.get(key.trim());
				if (!table.stringColumn("name").contains(drill_to_report.trim())) {
					String e = String.format("Report %s 'drill'中%s的drilldown %s 应该是一个视图，但是不存在", name, key,
							drill_to_report);
					errors.add(e);
				}
				reportList.remove(drill_to_report);
				parentReportMaps.put(drill_to_report, name);
			}
		}
		return errors;
	}

	private List<String> validateItems(String name, String[] items, Map<String, ItemDef> itemDefs,
			Map<String, DataItemDef> dataItemDefs) {
		List<String> ret = new ArrayList<String>();
		for (String item : items) {
			if (!item.isEmpty()) {
				if (!itemDefs.containsKey(item.trim()) && !dataItemDefs.containsKey(item.trim())) {
					ret.add(String.format("Report %s 'items' refer to %s which has not been defined as 科目 or 原始数据",
							name, item));
				}
			}
		}
		return ret;
	}

	private Map<String, ItemDef> readItemDefs() {
		Map<String, ItemDef> itemDefs = new HashMap<String, ItemDef>();
		Table cal_table;
		try {
			cal_table = new XlsxReader().read(XlsxReadOptions.builder("testdata/a.xlsx").sheetIndex(0).build());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println(cal_table);
		for (int i = 0; i < cal_table.rowCount(); i++) {
			Row row = cal_table.row(i);
			ItemDef itemMeta = new ItemDef();
			String name = row.getString("name");
			String title = row.getString("title");
			String params = row.getString("parameter");
//			String calculate = row.getString("calculate");
			JSONObject jsonObj = JSON.parseObject(params);
			int order = row.getInt("order");
			if (name != null && !name.isEmpty()) {
				itemMeta.setName(name);
				itemMeta.setTitle(title);
				itemMeta.setExpression(jsonObj.getString("value_formula"));
				itemMeta.setParameters(params);
				itemMeta.setOrder(order);
//				itemMeta.setCalculate(calculate);
				itemDefs.put(name, itemMeta);
			}

		}
		return itemDefs;
	}

	private Map<String, DataItemDef> readRawDataDefs() throws IOException {
		Table metadata_table = new XlsxReader()
				.read(XlsxReadOptions.builder("testdata/metadata.xlsx").sheetIndex(0).build());
		System.out.println(metadata_table);
		Map<String, DataItemDef> rawDataDefs = new HashMap<String, DataItemDef>();
		for (int i = 0; i < metadata_table.rowCount(); i++) {
			Row row = metadata_table.row(i);
			DataItemDef itemDef = new DataItemDef();
			String name = row.getString("name");
			rawDataDefs.put(name, itemDef);
			itemDef.setName(name);
			itemDef.setTitle(row.getString("title"));
			itemDef.setTable(row.getString("table"));
			itemDef.setTable_title(row.getString("table_title"));
			itemDef.setType(row.getString("data_type"));
		}
		return rawDataDefs;
	}
}