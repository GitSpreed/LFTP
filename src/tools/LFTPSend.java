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

public class LFTPSend extends LFTP {
	
	private InputStream in = null;
	private Map<Integer, Packet> cache = new HashMap<>();
	private int count = 0;
	
	public LFTPSend(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	/* TODO correct some logic error*/
	protected void sendFile() {
		byte[] data = new byte[Packet.MAX_PACKET_LENGTH - Packet.MIN_PACKET_LENGTH];
		int bytesNum = 0;
		try {
			while(!this.isWindowFull() && (bytesNum = in.read(data)) != -1 ) {
				Packet packet = new Packet(getSrcPort(), getDstPort(), false, false, false, true, getSeqNum(), getAckNum(), getFwnd(), Arrays.copyOf(data, bytesNum));
				cache.put(packet.getSeqNum(), packet);
				this.setSeqNum(getSeqNum() + bytesNum);
				this.send(packet);
			}
			System.out.println("read file with " + bytesNum + " bytes");
			if (bytesNum == -1) {
				this.setFinished(true);
				Packet fin = sendFin();
				cache.put(fin.getSeqNum(), fin);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected void receiveFile() {
		boolean flag = true;
		Packet packet = null;
		Timer timer = null;
		while((packet = receive()) != null || flag) {
			if (packet == null) {
				try {
					timer = new Timer();
					timer.schedule(new Task(timer, this, 1000), 1000);
					synchronized (this) {
						wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				flag = false;
				/* TODO set right cancel */
				if (timer != null) {
					timer.cancel();
				}
				System.out.println("ReceiveFile: syn=" + packet.isSYN() + " ack=" + packet.isACK());
				
				if (packet.isSYN() && packet.isACK()) {
					System.out.println("setDstPort: " + packet.getSrcPort());
					this.setDstPort(packet.getSrcPort());
					this.setAckNum(packet.getSeqNum() + 1);
					this.setStart(true);
				}
				
				if (packet.isFIN()) {
					this.setFinished(true);
				}
				
				if (this.updateLastByteRecv(packet.getAckNum())) {
					count++;
				} else {
					count = 1;
				}
				
				//Quick reSend
				if (count == 3) {
					this.reSend();
				}
			}
		}
	}
	
	public void setFilePath(String path) {
		File file = new File(path);
		if (!file.exists()) {
			this.setFinished(true);
			System.out.println("file not exist:" + path);
			return ;
		}
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public boolean reSend() {
		Packet packet = cache.get(lastByteRecv);
		if (packet != null) {
			this.send(packet);
			return true;
		}
		return false;
	}
	
	public void sayHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, false, false, true, seqNum++, 0, getFwnd(), new byte[1]);
		this.setSeqNum(seqNum);
		cache.put(seqNum, packet);
		this.send(packet);
	}
	
	
	public void replyHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, true, false, true, seqNum++, getAckNum(), getFwnd(), new byte[1]);
		this.setSeqNum(seqNum);
		cache.put(seqNum, packet);
		this.send(packet);
	}
	
	
	@Override
	public void run() {

		System.out.println("begin to run");
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
		System.out.println("end to run");
	}

}
