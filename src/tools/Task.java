package tools;

import java.util.Timer;
import java.util.TimerTask;

public class Task extends TimerTask {
	
	private Timer timer = null;
	private LFTPSend callback = null;
	private int time = 0;
	
	public Task(Timer timer, LFTPSend callback, int time) {
		super();
		this.timer = timer;
		this.callback = callback;
		this.time = time;
	}

	@Override
	public void run() {
		callback.reSend();
		time *= 2;
		timer.schedule(new Task(timer, callback, time), time);
	}

}
