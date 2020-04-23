这是一个示例演示如何利用将groovy script engine内嵌到Java程序, 并用tablesaw做为内存中的dataframe来做数据转换和计算
1. src/test/java/XlsxTest是个示例来演示一个计算场景 - 在客服队列运营和财务的所有数据之上，通过使用expression来做数据的转换和处理
   - 环境setup
   这个场景中有三个Excel workbook分别做为数据源，元数据
   A. data/data_prep.xlsx有所有的数据，它有12张sheet，每个sheet可以看成是一个数据表
   B. data/dataitem_meta.xlsx是描述数据源形状（数据表，字段）的元数据，基于这个元数据，我们可以理解数据表的结构和他们之间的关系
   C. data/cal2.xlsx也是元数据，它描述了在这个场景中，我们想做什么样的数据转换，和数据组合。 sheet中的每一行是一个统计科目，它的计算和统计逻辑用表达式来表达
   - 入口test()
   它主要是初始化CalculationEngine,然后把数据源传入。
2. CalculationEngine
   CalculationEngine的计算逻辑如下：
   A.科目的计算逻辑可以被看成是一个规则，所有科目就组成了一个规则集。因为科目之间可以通过表达式互相引用，所以正确的，唯一的计算路径很难确定。所以这里采取规则引擎的原理。
   就是在不停的循环，在每个循环中计算可以计算的科目。循环在达到两个条件之一的时候会终止：
      1. 所有科目都被成功地评估完成了。 -SUCCESS
      2. 本次循环没有能够评估任何一个还未能计算的的科目，就是没有任何进展。 -ERROR，标志着科目的配置有问题，导致规则集并不收敛
3. Expression
   A. ExpressionContext可以看成是Groovy引擎的评估的堆栈， 它有所有可以在表达式中使用的变量
      1）所有的科目都在ExpressionContext中，以便它们可以互相引用。 还没有被评估的科目的值被初始为NTEObject.
      2）来自data/data_prep.xlsx的原始数据也在ExpressionContext中，它们被按照数据表组织成12个“表对象”，背后其实是tablesaw的Table对象和Column对象，以此方便支持在过滤，join，groupby的数据聚合，转换操作。
        （tablesaw的用户手册请见https://jtablesaw.github.io/tablesaw/userguide/tables）
   B. 自定义的公式（sum,sumif,etc)背后是一个静态类Functions (expression.Functions)    
