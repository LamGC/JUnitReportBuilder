# JUnitReportBuilder
一个用来生成JUnit XML测试报告的工具库<br/>
A Tool Library for Generating JUnit Test Reports<br/>
中文  |  [English(Not finished, need help)](#)
-----------
## 介绍
- 这个用来做什么？<br/>
   JUnitReportBuilder是一个用于在脱离Maven等构建工具的情况下生成JUnit报告的,你可以通过ReportBuilder,给调用JUnit运行测试的代码旁边加几行代码,就可以收集你所需要的XML格式的测试报告了！

- JUnitReportBuilder是如何简单生成XML测试报告的？<br/>
   JUnit提供了用于获取测试情况的监听器`RunListener`,通过`RunListener`可以获取测试运行过程里的情况,通过事件来生成XML测试报告
   
- JUnitReportBuilder的XML测试报告格式是按照什么规则生成的？<br/>
   格式来源[点击前往](http://help.catchsoftware.com/display/ET/JUnit+Format),此报告经过测试,支持使用在Jenkins.
## 怎么使用JUnitReportBuilder？
例如main方法是这样的:
```
  public static void main(String[] args){
    JUnitCore junit = new JUnitCore();
    //等待测试的测试类数组
    Class[] TestClass = new Class[]{Test1.class, Test2.class};
     
    for(Class Test : TestClass){
      //构造一个XML报告生成器
      XMLReportBuilder builder = new XMLReportBuilder(junit);
      //运行测试
      junit.run(Test);
      try {
        //将XML测试报告保存到文件中
        builder.writeToFile(new File("TestReport/TEST_" + test.getSimpleName() + ".xml"));
      } catch (IOException e) {
        ...
      }
    }
    ....
  } 
```
这样就好了！
