package com.lius.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 配置文件读写工具
 * @author lius
 *
 */
public class propertiesReader {
	
	//配置文件位置
	private String propertiesFilePath = "";
	//配置文件类
	private Properties properties=new Properties();
	//打印日志类
	private static Logger logger = Logger.getLogger(
			Thread.currentThread().getName());
	//是否初始化标记
	private boolean flag = false;
	/**
	 * <p>有参构造</p>
	 * <p>1.传参</p>
	 * <p>2.初始化</p>
	 * @param propertiesFilePath
	 */
	public propertiesReader(String propertiesFilePath) {
		super();
		this.propertiesFilePath = propertiesFilePath;
		try {
			init();
			flag = !this.properties.isEmpty();//配置文件参数为空标志配置文件初始化失败！
			if (flag) logger.info(String.format("[%s]配置文件初始化完成！",
					this.propertiesFilePath));
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/**
	 * 初始化properties
	 * @throws IOException 
	 */
	private void init() throws IOException {
		//获取配置文件的二进制流
		
		InputStream inStream = new FileInputStream(this.propertiesFilePath);
		//配置文件不存在或获取配置文件的二进制流数据为空，抛出异常
		if(!new File(this.propertiesFilePath).exists() || inStream==null) {
			throw new IOException(String.format("[%s]配置文件不存在", 
					this.propertiesFilePath));//抛出异常
		}
		//加载配置文件
		properties.load(inStream);
		
	}
	/**
	 * <p>判断该配置文件是否包含该属性名的属性值</p>
	 * @param propertyName
	 * @return
	 */
	public boolean isExists(String propertyName) {
		return properties.containsKey(propertyName);
	}
	/**
	 * 更新配置文件
	 * @param property
	 * @param value
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public boolean set(String property,String value) throws FileNotFoundException, IOException {
		if(flag) {
			this.properties.setProperty(property, value);
			this.properties.store(new FileOutputStream(propertiesFilePath), "");
			this.properties.load(new FileInputStream(propertiesFilePath));
			logger.info(String.format("[%s]配置文件更新[key:%s,value:%s]配置参数", this.propertiesFilePath,property,value));
			return true;
		}else logger.info("更新配置参数失败,请初始化配置文件...");
		return false;
		
	}
	/**
	 * 获取配置文件的属性值
	 * @param property
	 * @return
	 */
	public String get(String property) {
		if(flag) {
			String value =  this.properties.getProperty(property);
			logger.info(String.format("[%s]配置文件获取 %s 配置参数:%s", this.propertiesFilePath,property,value));
			return value;
		}else logger.info(initFail());
		return "";
	}
	/**
	 * 获取配置文件状态:
	 * 【update】更新配置参数到运行内存
	 * 【other】不会发生动作
	 * @return
	 */
	public String getStatus() {
		if(flag && this.properties.containsKey("state")) {
			String value = properties.getProperty("state");
			logger.info(String.format("[%s]配置文件获取配置文件状态:%s", this.propertiesFilePath,value));
			return value;
		}else logger.info(flag?
				String.format("配置文件[%s]不存在state属性值", this.propertiesFilePath):
					initFail());
		return "";
	}
	/**
	 * 初始化失败返回的字符串结果值
	 */
	private String initFail() {
		return "获取配置参数失败,请初始化配置文件...";
	}
}
