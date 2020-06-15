package com.lius.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.lius.connPool.connectionPool;

public class testPool {
	
	private static AtomicInteger s = new AtomicInteger(0);
	private static String propertyFilePath = "src/com/lius/resources/pool.properties";
	private static int paralNums = 1000; 
	private static CountDownLatch countDownMainLatch = new CountDownLatch(paralNums);

	private static CountDownLatch countDownLatch = new CountDownLatch(paralNums);
	private connectionPool connPool;
	public static void main(String[] args) throws InterruptedException {
		connectionPool connPool = connectionPool.getConnectionPool(propertyFilePath);
//		connectionObj conn = connPool.getConnection().get();
		
		Thread.sleep(3000);
		
		for(int i =0;i<paralNums;i++) {
			new Thread(()->{
				try {
					countDownLatch.await();
					System.out.println("开始查询...");
					query(connPool);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
			}).start();
			countDownLatch.countDown();
		}
		countDownMainLatch.await();	
	}
	
	
	private static void query(connectionPool connPool) {
		connPool.getConnection().ifPresent(conn->{
			Connection connection = conn.getConn();
			try {
				PreparedStatement pre = connection.prepareStatement("select * from users");
				ResultSet resultSet = pre.executeQuery();
				
				
				while(resultSet.next()) {
					System.out.println(String.format("[index:%d] id[%s],name[%s],password[%s]",
							s.addAndGet(1),resultSet.getString("id"),resultSet.getString("name"),
							resultSet.getString("password")));
				}
				connPool.reserveConnection(Optional.of(conn));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		countDownMainLatch.countDown();

	}
}
