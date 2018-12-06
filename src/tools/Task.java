package tools;

import java.util.Timer;
import java.util.TimerTask;

//定时器超时时所启动的动作
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
		callback.reSend();				//数据包重传
		time *= 2;						//计时器等待时长翻倍
		callback.cwndMinus();			//拥塞窗口减小
		timer.schedule(new Task(timer, callback, time), time);
	}

}
