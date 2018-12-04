package tools;

import java.util.ArrayList;

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
