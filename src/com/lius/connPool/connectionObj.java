package com.lius.connPool;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

import com.lius.models.connParamObj;
/**
 * <p>数据路连接对象类</p>
 * @author lius
 *
 */
public class connectionObj {
	public static final int notInit = 0;    //未被初始化
	public static final int inited = 1;	    //已经初始化，可使用
	public static final int timeOut = 2;	//该连接句柄对象已超时
	public static final int destroyed = 3;  //该连接句柄对象申请销毁
	public static final int free = 4;       //该对象连接句柄已释放

	
	private connParamObj connParam;     //数据库连接配置参数类
	//是否申请更新 -1 不申请更新，5.申请更新所有连接句柄对象
	public static int update = -1;
	public boolean isUpdate = true;
	//连接句柄
	private Connection conn;  
	private Boolean isUesred = false; //是否在使用
	private int State = notInit; //该连接句柄对象状态
	private long birthDate;	//该连接句柄生成时间
	//打印日志类
	private static Logger logger = Logger.getLogger(Thread.
			currentThread().getName());
	public connectionObj() {
		// TODO Auto-generated constructor stub
		init();
	}
	/**
	 * 初始化连接
	 * @param url
	 * @param user
	 * @param password
	 * @return
	 */
	public Connection init() {		
		try {
		conn=DriverManager.getConnection(connParam.url, connParam.user, connParam.password);
		this.State = connectionObj.inited;
	} catch (SQLException e) {
		logger.info(e.getMessage());
	}
		return conn;	
	}
	
	//释放连接句柄资源
	public void freeResourceConnection(){
		if(this.State==connectionObj.destroyed && this.conn!=null) {
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.info(e.getMessage());
			}
		}
	}
	
	public static int getUpdate() {
		return update;
	}
	public static void setUpdate(int update) {
		connectionObj.update = update;
	}
	public Boolean getIsUesred() {
		return isUesred;
	}
	public void setIsUesred(Boolean isUesred) {
		this.isUesred = isUesred;
	}
	public int getState() {
		return State;
	}
	public void setState(int state) {
		State = state;
	}
	public long getBirthDate() {
		return birthDate;
	}
	public void setBirthDate(long birthDate) {
		this.birthDate = birthDate;
	}
	public Connection getConn() {
		return conn;
	} 
	
	
	
}
