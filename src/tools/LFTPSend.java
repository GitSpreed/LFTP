package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

//LFTP发送端
public class LFTPSend extends LFTP {
	
	public static final String defaultPath = "Resource/";			//发送文件的默认路径
	
	private String fileName = null;							//发送文件的文件名
	private InputStream in = null;							//读取发送文件的字节流
	private Map<Integer, Packet> cache = new HashMap<>();		//发送数据包的缓存
	private int count = 0;									//用于计数，记录接收到多少个重复的ACK
	
	private Timer timer = null;								//计时器，用于超时重发
	
	//构造函数
	public LFTPSend(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	//发送文件
	protected void sendFile() {
		byte[] data = new byte[Packet.MAX_PACKET_LENGTH - Packet.MIN_PACKET_LENGTH];
		int bytesNum = 0;
		try {
			//发送窗口未满且文件没有发送完毕时，发送数据包
			while(!this.isWindowFull() && (bytesNum = in.read(data)) != -1 ) {
				Packet packet = new Packet(getSrcPort(), getDstPort(), false, false, false, true, getSeqNum(), getAckNum(), getFwnd(), Arrays.copyOf(data, bytesNum));
				cache.put(packet.getSeqNum(), packet);
				this.setSeqNum(getSeqNum() + bytesNum);
				this.send(packet);
			}
			if (bytesNum == -1) {
				this.setFinished(true);
				Packet fin = sendFin();
				cache.put(fin.getSeqNum(), fin);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//接收ACK确认包
	protected void receiveFile() {
		boolean flag = true;
		Packet packet = null;
		
		timer = new Timer();
		//启动重传计时器
		timer.schedule(new Task(timer, this, 1000), 1000);
		
		while((packet = receive()) != null || flag) {
			if (packet == null) {
				try {
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				flag = false;
				if (timer != null) {
					timer.cancel();
				}
				
				if (packet.isSYN() && packet.isACK()) {
					this.setDstPort(packet.getSrcPort());
					this.setAckNum(packet.getSeqNum() + 1);
					this.setStart(true);
				}
				
				if (this.updateLastByteRecv(packet.getAckNum())) {
					count++;
				} else {
					count = 1;
					this.cwndPlus();
				}

				//接收到三个重复ACK，快速重传
				if (count >= 3) {
					this.reSend();
				}
			}
		}
	}
	
	//设置发送文件的文件名，同时创建读取该文件的字节流对象
	public void setFileName(String name) {
		File file = new File(defaultPath + name);
		this.fileName = new String(name);
		if (!file.exists()) {
			this.setFinished(true);
			System.out.println("Thread " + this.getId() + "> " + "file not exist:" + name);
			return ;
		}
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	//重发最后一个未确认的数据包
	public boolean reSend() {
		Packet packet = cache.get(lastByteRecv);
		System.out.println("Thread " + this.getId() + "> " + "resend the packet " + lastByteRecv);
		if (packet != null) {
			this.send(packet);
			return true;
		}
		return false;
	}
	
	//发送连接请求，第一次握手
	public void sayHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		System.out.println("Thread " + this.getId() + "> " + "say \"hello\" to " + this.getDstAddr().toString() + " with random seqNum(" + seqNum + ")");
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, false, false, true, seqNum, 0, getFwnd(), fileName.getBytes());
		seqNum += fileName.getBytes().length;
		this.setSeqNum(seqNum);
		cache.put(seqNum, packet);
		this.send(packet);
	}
	
	//回复连接请求，第二次握手
	public void replyHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		System.out.println("Thread " + this.getId() + "> " + "reply \"hello\" to " + this.getDstAddr().toString() + " with random seqNum(" + seqNum + ")");
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, true, false, true, seqNum++, getAckNum(), getFwnd(), new byte[1]);
		this.setSeqNum(seqNum);
		cache.put(seqNum, packet);
		this.send(packet);
	}
	
	
	//发送端的运行方式
	@Override
	public void run() {

		System.out.println("Thread " + this.getId() + "> " + "start");
		while(!this.isStart()) {
			this.receiveFile();
		}
		while (!this.isFinished()) {
			this.sendFile();
			this.receiveFile();
		}
		if (in != null) {
			try {
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		while (!isSendOver()) {
			this.receiveFile();
		}
		System.out.println("Thread " + this.getId() + "> " + "end");
		this.setEnd(true);
	}

}
