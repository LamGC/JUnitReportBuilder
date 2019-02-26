package net.lamgc.report_builder.junit;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * 用于生成XML测试报告的类
 * 目前仅能用于生成单个测试类的报告
 */
public class XMLReportBuilder{
	
	//使用Dom来创建XML文件
	Document dom;
	//根元素
	Element root;
	//Properties元素
	Element propertiesE;
	
	//XML的输出路径
	File OutPath;
	
	/**
	 * 构造并设置一个JunitCore对象
	 * @param junit 要生成运行报告的JUnitCore对象
	 */
	public XMLReportBuilder(JUnitCore junit) {
		new XMLReportBuilder(junit, null);
	}
	
	/**
	 * 设置一个JUnitCore对象,并添加一组系统变量
	 * @param junit 要生成运行报告的JUnitCore
	 * @param properties 要添加的系统变量
	 */
	public XMLReportBuilder(JUnitCore junit, String[] properties) {
		//先初始化DOM对象
		dom = DocumentHelper.createDocument();
		root = dom.addElement("testsuite");
		propertiesE = root.addElement("properties");
		
		//添加Listener
		junit.addListener(getListener());

		if(properties != null) {
			//添加Property
			for(String property : properties) {
				addProperty(property);
			}
		}

	}
	
	/**
	 * 添加系统变量
	 * @param key 变量名
	 */
	public void addProperty(String key) {
		addProperty(key, null);
	}
	
	/**
	 * 添加系统变量或自定义变量
	 * @param key 变量名
	 * @param value 变量值,如果为空则添加系统变量
	 */
	public void addProperty(String key, String value) {
		if(value == null) {
			propertiesE.addElement("property")
				.addAttribute("name", key)
				.addAttribute("value", System.getProperty(key));
		}else {
			propertiesE.addElement("property")
				.addAttribute("name", key)
				.addAttribute("value", value);
		}
	}
	
	/**
	 * 获取用于生成XML报告的Listener对象
	 * @return 生成XML报告用的Listener对象
	 */
	private RunListener getListener() {
		return new RunListener() {
			
			//当前测试方法所属元素
			Element LastMethod;
			//计时
			long StartTime;
			
			//失败计次,用以区分error和failure
			int FailureCount;
			
			/**
			 * 在准备运行所有测试前调用
			 */
			@Override
			public void testRunStarted(Description description) {
				//System.out.println("[RB] 准备测试...");
				root.addAttribute("tests", String.valueOf(description.testCount()));
			}
			
			/**
			 * 准备测试某一项单元测试时调用
			 */
			@Override
			public void testStarted(Description description) {
				//System.out.println("[RB] 准备测试方法 " + description.getDisplayName());
				LastMethod = root.addElement("testcase")
					.addAttribute("name", description.getMethodName())
					.addAttribute("classname", description.getClassName());
				
				//记录测试开始的时间,计算耗时
				//这里要注意的地方是计时,由于JUnit4 并没有提供在方法完成后获得运行时间的方法,所以测试方法的计时是由XMLReportBuilder自行计时的,会不准确.
				StartTime = new Date().getTime();
			}
			
			/**
			 * 当测试方法被标注为Ignore,被忽略测试时调用
			 */
			@Override
			public void testIgnored(Description description) {
				//System.out.println("[RB] 方法 " + description.getDisplayName() + " 被忽略.");
				
				//由于方法被忽略,所以不需要记录LastElement
				root.addElement("testcase")
					.addAttribute("name", description.getMethodName())
					.addAttribute("classname", description.getClassName())
					.addAttribute("time", "0")
					.addElement("skipped");
			}
			
			/**
			 * 当单元测试中的断言不成立时调用 failure
			 */
			@Override
			public void testAssumptionFailure(Failure failure) {
				//System.out.println("[RB] 方法 " + failure.getDescription().getDisplayName() + " 断言不成立.");
				LastMethod.addElement("failure")
						  .addAttribute("message", failure.getMessage())
						  .addText(failure.getTrace());
				FailureCount++;
			}
			
			/**
			 * 当单元测试失败时调用 error
			 */
			@Override
			public void testFailure(Failure failure) {
				if(failure.getException() instanceof AssertionError) {
					//如果是断言导致的,丢给断言不成立事件处理
					testAssumptionFailure(failure);
					return;
				}
				//System.out.println("[RB] 方法 " + failure.getDescription().getDisplayName() + " 测试失败.");
				
				LastMethod.addElement("error")
						  .addAttribute("message", failure.getMessage())
						  .addText(failure.getTrace());
			}
			
			/**
			 * 当单元测试完成时调用,无论测试结果如何
			 */
			@Override
			public void testFinished(Description description) {
				long RunTime = new Date().getTime() - StartTime;
				
				//System.out.println("[RB] 方法 " + description.getDisplayName() + " 测试结束,耗时: " + RunTime + "ms.");
				//获得耗时
				LastMethod.addAttribute("time", new DecimalFormat("##0.000").format(RunTime / 1000D));
				
				//清空变量
				StartTime = 0;
				LastMethod = null;
			}
			
			/**
			 * 在所有测试完成时调用
			 */
			@Override
			public void testRunFinished(Result result) {
				//System.out.println("[RB] 运行结束,耗时: " + result.getRunTime() + "ms.");
				root.addAttribute("time", new DecimalFormat("##0.000").format(result.getRunTime() / 1000))
					.addAttribute("skipped", String.valueOf(result.getIgnoreCount()))
					.addAttribute("errors", String.valueOf(result.getFailureCount() - FailureCount))
					.addAttribute("failures", String.valueOf(FailureCount))
					.addAttribute("timestamp", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
				
				//自动保存
				if(OutPath != null) {
					try {
						write(new FileOutputStream(OutPath));
					} catch (IOException e) {
						//抛出运行时异常
						throw new RuntimeException("ReportBuilder save report failed!",e);
					}
				}
				
				//清空变量
				FailureCount = 0;
			}
		};
	}

	/**
	 * 设置路径以在测试结束后自动保存
	 * 尚未公开
	 * @param file 文件File对象
	 */
	void setAutoSave(File file) {
		this.OutPath = file;
	}
	
	/**
	 * 将XML测试报告输出到文件
	 * @param FilePath 文件的路径
	 * @throws IOException
	 */
	public void writeToFile(String FilePath) throws IOException {
		writeToFile(new File(FilePath));
	}
	
	/**
	 * 将XML测试报告输出到文件
	 * @param file 文件的File对象
	 * @throws IOException
	 */
	public void writeToFile(File file) throws IOException {
		write(new FileOutputStream(file));
	}
	
	/**
	 * 将XML报告输出到OutputStream,默认编码为UTF-8
	 * @param out 要输出的OutputStream对象
	 * @throws IOException 
	 */
	public void write(OutputStream out) throws IOException {
		new XMLWriter(out).write(dom);
	}
	
	/**
	 * 将XML报告输出到OutputStream,默认编码为UTF-8
	 * @param out 要输出的OutputStream对象
	 * @param newlines 是否换行
	 * @throws IOException 
	 */
	public void write(OutputStream out, boolean newlines) throws IOException {
		new XMLWriter(out, new OutputFormat(null, newlines)).write(dom);
	}
	
	/**
	 * 将XML报告输出到OutputStream
	 * @param out 要输出的OutputStream对象
	 * @param newlines 是否换行
	 * @param encoding 字符编码
	 * @throws IOException 
	 */
	public void write(OutputStream out, boolean newlines, String encoding) throws IOException {
		new XMLWriter(out, new OutputFormat(null, newlines, encoding)).write(dom);
	}
	
	/**
	 * 获取XML格式的报告.
	 * 务必在JUnit完成单元测试后再调用,否则会导致报告不完整的情况
	 * @return 报告的XML内容
	 */
	public String XML_toString() {
		return dom.asXML();
	}
	
}