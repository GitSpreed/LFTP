package tools;

import java.util.ArrayList;


//用于存储接收到的LFTP包，可以看做一个消息队列，采用单例模式
public class MyList extends ArrayList<Packet> {
	
	private static MyList _instance = null;
	
	private MyList() {
		super();
	}
	
	public static MyList getInstance() {
		if (_instance == null) {
			_instance = new MyList();
		}
		return _instance;
	}
}
