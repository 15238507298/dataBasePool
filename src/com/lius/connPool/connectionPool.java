package com.lius.connPool;

/**
 * <p>数据库连接池管理类</p>
 * @author lius
 */

import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.lius.models.connParamObj;
import com.lius.utils.propertiesReader;

public class connectionPool {

	// 单例模式
	private static connectionPool connectionPoolInstance = null;

	// 数据库连接池容器
	private static CopyOnWriteArrayList<connectionObj> connectionObjList = new CopyOnWriteArrayList<>();

	// 数据库连接池动态更新配置文件的时间
	private static long dynamicUpdateTime = new Date().getTime();

	private connParamObj connParam; // 数据库连接配置参数类
	private static int minPoolSize = 0;// 最小连接池数量
	private static int maxPoolSize = 0;// 最大连接池数量
	private static int defaultSize = 6;// 默认连接池数量
	private static boolean state = false;// 配置文件状态是否更新
	private static long timeOutNumber = Integer.MAX_VALUE;// 超时时间
	private static propertiesReader dBProperties = null;// 数据库连接池配置文件类
	// 打印日志类
	private static Logger logger = Logger.getLogger(Thread.currentThread().getName());

	public void reserveConnection(Optional<connectionObj> connObj) {
		connObj.ifPresent(conn -> conn.setIsUesred(false));
		logger.info(String.format("[%s]连接句柄对象归还成功...",connObj));
	}

	/**
	 * <p>
	 * 从数据库连接池获取数据库连接对象
	 * </p>
	 * 
	 * @return
	 */
	public synchronized Optional<connectionObj> getConnection() {

		topManage();
//		loopConnectionObjManager();

		Optional<connectionObj> connObjItem = connectionObjList.stream().filter(
				connObj -> !connObj.getIsUesred() && connObj.getState() == connectionObj.inited && connObj.isUpdate)
				.findFirst();
		
		Connection conn = null;
		if (connObjItem.isPresent()) {
			connectionObj connItem = connObjItem.get();
			connItem.setIsUesred(true);
			connObjItem = Optional.ofNullable(connItem);
		}

		return connObjItem;
	}

	// 数据库连接池容器轮询管理器
	private void loopConnectionObjManager() {
		
		//
		if (connectionObjList.size() <= 0) {
			loopSetConnectionObjList(connectionPool.defaultSize);
		}
		// 对数据库连接池容器中的连接句柄进行初始化以及对已释放数据库连接资源的的对象执行过滤操作
		connectionObjList = connectionObjList.parallelStream().map(connObj -> {
			int states = connObj.getState();

			if (connObj.getUpdate() == 5)
				handleBusy(connObj, connectionObj.inited);

			// 如果连接句柄对象创建时间超时,会标记该连接句柄对象状态为超时状态
			if (new Date().getTime() - connObj.getBirthDate() >= 1000 * 60 * 5) {
				connObj.setState(connectionObj.timeOut);
			}

			switch (states) {
			case connectionObj.notInit:
				handleBusy(connObj, connectionObj.inited);
				break;
			case connectionObj.inited:
				break;
			case connectionObj.timeOut:
				handleBusy(connObj, connectionObj.inited);
				break;
			case connectionObj.destroyed:
				if (connObj.getIsUesred())
					break;
				connObj.freeResourceConnection();// 释放资源
				connObj.setState(connectionObj.free);
				break;
			}
			return connObj;
		}).filter(connObj -> connObj.getState() == connectionObj.inited || connObj.isUpdate)
				.collect(Collectors.toCollection(CopyOnWriteArrayList::new));
	}

	/**
	 * 获取数据库连接池实例方法(单例模式)
	 * 
	 * @return
	 */
	public static connectionPool getConnectionPool(String propertiesFilePath) {
		connectionPool.connectionPoolInstance = connectionPool.connectionPoolInstance == null
				? new connectionPool(propertiesFilePath)
				: connectionPool.connectionPoolInstance;
		return connectionPool.connectionPoolInstance;
	}

	/**
	 * <p>
	 * 定时数据库连接池管理器
	 * </p>
	 */
	private void topManage() {
		loopConnectionObjManager();

		long value = connectionObjList.stream().filter(
				connObj -> !connObj.getIsUesred() && connObj.getState() == connectionObj.inited && connObj.isUpdate)
				.count();

		if (value <= 0 && connectionObjList.size() + defaultSize <= maxPoolSize) {
			if (value < 4)
				loopSetConnectionObjList(defaultSize);
		} else {
			loopSetConnectionObjList(maxPoolSize - connectionObjList.size());
		}
	}

	private connectionPool(String propertiesFilePath) {
		super();
		// TODO Auto-generated constructor stub
		try {
			init(propertiesFilePath);
			logger.info("数据库连接池初始化完成...");
			new Thread(() -> {
				while (true) {
					try {
						Thread.sleep(4000);
						topManage();
						dynamicUpdateProperties();// 动态更新配置文件方法
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						logger.info(e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			// TODO Auto-generated catch block
			logger.info(e.getMessage());
		}
	}

	// 初始化数据库连接池管理类
	private void init(String propertiesFilePath) throws FileNotFoundException, IOException {
		// 加载数据库连接池配置文件
		dBProperties = new propertiesReader(propertiesFilePath);
		// 解析数据库连接池配置文件
		connParam.driverPath = propertiesSetValue("driver", connParam.driverPath);
		connParam.url = propertiesSetValue("url", connParam.url);
		connParam.user = propertiesSetValue("user", connParam.user);
		connParam.password = propertiesSetValue("password", connParam.password);
		connectionPool.minPoolSize = Integer.parseInt(propertiesSetValue("minPoolSize", connectionPool.minPoolSize+""));
		connectionPool.maxPoolSize = Integer.parseInt(propertiesSetValue("maxPoolSize", connectionPool.maxPoolSize+""));
		connectionPool.state = propertiesSetValue("state", "").equals("update") ? true : false;
		connectionPool.timeOutNumber = new Long(propertiesSetValue("minPoolSize", connectionPool.timeOutNumber+""));
		if (connectionPool.minPoolSize > 0) {
			// 创建数据库连接加入到数据库连接池容器中
			loopSetConnectionObjList(connectionPool.minPoolSize - connectionObjList.size());
		}
	}

	/**
	 * <p>
	 * 获取配置文件的参数值,不存在该参数,返回原值
	 * </p>
	 * 
	 * @param <T>
	 * @param propertyName
	 * @param c
	 * @return
	 */
	private <T> T propertiesSetValue(String propertyName, T c) {
		if (dBProperties.isExists(propertyName)) {
			return (T) dBProperties.get(propertyName);
		}
		return c;
	}

	/**
	 * <p>
	 * 如果该连接句柄为在使用状态,不对该连接句柄采取动作
	 * </p>
	 * 
	 * @param connObj
	 * @param state
	 * @return
	 */
	private connectionObj handleBusy(connectionObj connObj, int state) {
		if (connObj.getIsUesred()) {
			connObj.isUpdate = false;
			return connObj;
		}
		connObj.init();
		connObj.setBirthDate(new Date().getTime());
		connObj.setState(state);
		connObj.isUpdate = true;
		return connObj;
	}

	/**
	 * <p>
	 * 创建数据库连接加入到数据库连接池容器中
	 * </p>
	 * 
	 * @param poolSize
	 */
	private void loopSetConnectionObjList(int poolSize) {
		//超出线程最大数,禁止创建新连接句柄对象
		if(connectionObjList.size()+poolSize>connectionPool.maxPoolSize) {
			return;
		}
		// 创建数据库连接加入到数据库连接池容器中
		for (int i = 0, w = poolSize; i < w; i++) {
			// 创建连接句柄对象实例
			connectionObj connObj = new connectionObj();
			logger.info(String.format("创建数据库连接句柄对象[%S]", connObj));
			// 将数据库连接加入到数据库连接池容器中
			connectionObjList.add(connObj);
		}
	}

	/**
	 * <p>
	 * 动态更新数据库连接池配置文件
	 * </p>
	 */
	private void dynamicUpdateProperties() {
		if (new Date().getTime() - connectionPool.dynamicUpdateTime >= 1000 * 5) {
			if (dBProperties.get("state").equals("update")) {
				logger.info("检测到数据库连接池配置文件发生变更,重新加载数据库连接池");
				// 设置所有数据库连接句柄对象update状态置为5
				connectionObjList = connectionObjList.parallelStream().map(connObj -> {
					connObj.setUpdate(5);
					return connObj;
				}).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
				// 重新获取最新动态更新数据库连接配置时间
				connectionPool.dynamicUpdateTime = new Date().getTime();
				dBProperties.set("state", "~update");
			}
		}

	}

}
