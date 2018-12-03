package tools;

import java.util.ArrayList;

public class MyList<E> extends ArrayList<E> {
	
	private static MyList<?> _instance = null;
	
	private MyList() {
		super();
	}
	
	public MyList<?> getInstance() {
		if (_instance == null) {
			_instance = new MyList<E>();
		}
		return _instance;
	}
}
