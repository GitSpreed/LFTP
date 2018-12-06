package tools;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class LFTP extends Thread{

	private InetAddress dstAddr;
	private int dstPort;
	private int srcPort;
	private int UDPDstPort;
	private DatagramSocket socket;
	
	private boolean isStart = false;
	private boolean isFinished = false;
	private boolean sendOver = false;
	
	private Object listLock;
	private Object socketLock;

	private MyList list;
	
	private int fwnd = 20000;
	private double cwnd = Packet.MIN_PACKET_LENGTH;
	private int threshold = Integer.MAX_VALUE;
	private int seqNum = 0;
	private int ackNum = 0;
	
	protected int lastByteRecv = 0;
	private boolean end = false;
	
	private Map<Integer, Integer> map = new HashMap<Integer, Integer>();
	
	public LFTP(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList list, Object socketLock, DatagramSocket socket) throws SocketException {
		this.dstAddr = dstAddr;
		this.srcPort = srcPort;
		this.dstPort = dstPort;
		this.UDPDstPort = UDPDstPort;
		this.socket = socket;
		this.listLock = listLock;
		this.list = list;
		this.socketLock = socketLock;
		this.socket = socket;
	}
	
	//增大拥塞窗口
	protected void cwndPlus() {
		//拥塞窗口小于阈值，指数级增长
		if (cwnd < threshold) {
			cwnd += Packet.MIN_PACKET_LENGTH;
		//拥塞窗口大于阈值，线性增长
		} else {
			cwnd += Packet.MIN_PACKET_LENGTH / 200;
		}
	}
	
	//减小拥塞窗口
	protected void cwndMinus() {
		threshold = (int) cwnd / 2;
		cwnd = threshold;
	}
	
	
	//参数为一个LFTP数据包，该函数将LFTP数据包封装在UDP数据包中，并发送出去
	protected void send(Packet data){
		synchronized(socketLock) {
			DatagramPacket datagramPacket = new DatagramPacket(data.getBytes(), data.getLength(), dstAddr, UDPDstPort);
			try {
				socket.send(datagramPacket);
				System.out.println("Thread " + this.getId() + "> " + "Send Packet: SrcPort=" + data.getSrcPort() + " DstPort=" + data.getDstPort()
									+ " seq=" + data.getSeqNum() + " ack=" + data.getAckNum() + " "
									+ data.isSYN() + " " + data.isACK() + " " +  data.isFIN() + " " + data.isREQ());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	//从数据包队列中取出发给该线程的数据包
	protected Packet receive(){
		synchronized(listLock) {
			Packet temp = null;
			Iterator<Packet> iter = list.iterator();
			while (iter.hasNext()) {
				temp = iter.next();
				if (temp.getDstPort() == srcPort) {
					iter.remove();
					System.out.println("Thread " + this.getId() + "> " + "Receive Packet SrcPort=" + temp.getSrcPort() + " DstPort=" + temp.getDstPort()
										+ " seq=" + temp.getSeqNum() + " ack=" + temp.getAckNum() + " "
										+ temp.isSYN() + " " + temp.isACK() + " " +  temp.isFIN() + " " + temp.isREQ());
					return temp;
				}
			}
			return null;
		}
	}
	
	//收到一个数据包，返回一个ACK确认包
	protected void sendBack() {
		Packet packet = new Packet(srcPort, dstPort, false, true, sendOver, false, seqNum++, ackNum, fwnd, new byte[1]);
		this.send(packet);
	}
	
	//数据发送完毕，发送一个结束确认包FIN
	protected Packet sendFin() {
		Packet packet = new Packet(srcPort, dstPort, false, false, true, false, seqNum++, ackNum, fwnd, new byte[1]);
		this.send(packet);
		return packet;
	}
	
	//更新收到的最大有序字节的序号
	protected boolean updateAckNum(int seq, int ack) {
		if (seq == ackNum) {
			ackNum = ack;
			Integer n = map.get(ackNum);
			while (n != null) {
				map.remove(ackNum);
				ackNum = n + 1;
				n = map.get(ackNum);
			}
		} else {
			if (map.get(seq) != null) return false;
			map.put(seq, ack);
		}

		if (isFinished && map.isEmpty()) {
			sendOver = true;
		}
		return true;
	}
	
	//更新接收端返回的ACK
	protected boolean updateLastByteRecv(int byteNum) {
		if (lastByteRecv == byteNum) {
			return true;
		} else if (lastByteRecv < byteNum) {
			lastByteRecv = byteNum;
		}
		if (isFinished && lastByteRecv == seqNum) {
			sendOver = true;
		}
		return false;
	}
	
	//判断发送窗口是否已满
	protected boolean isWindowFull() {
		return (seqNum - lastByteRecv) > Math.min(fwnd, cwnd);
	}

	//未实现的线程run函数，具体在其子类中实现
	@Override
	public void run() throws UnsupportedOperationException{
		throw new UnsupportedOperationException();
	}
	
	//getter and setter
	
	public int getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(int srcPort) {
		this.srcPort = srcPort;
	}

	public int getFwnd() {
		return fwnd;
	}

	public void setFwnd(int fwnd) {
		this.fwnd = fwnd;
	}

	public int getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(int seqNum) {
		this.seqNum = seqNum;
	}
	
	public InetAddress getDstAddr() {
		return dstAddr;
	}

	public void setDstAddr(InetAddress dstAddr) {
		this.dstAddr = dstAddr;
	}

	public int getDstPort() {
		return dstPort;
	}

	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}
	
	public int getUDPDstPort() {
		return UDPDstPort;
	}

	public void setUDPDstPort(int uDPDstPort) {
		UDPDstPort = uDPDstPort;
	}

	public int getAckNum() {
		return ackNum;
	}

	public void setAckNum(int ackNum) {
		this.ackNum = ackNum;
	}
	
	public boolean isFinished() {
		return isFinished;
	}

	public void setFinished(boolean isFinished) {
		this.isFinished = isFinished;
	}

	public boolean isStart() {
		return isStart;
	}

	public void setStart(boolean isStart) {
		this.isStart = isStart;
	}

	public boolean isSendOver() {
		return sendOver;
	}
	
	protected void setSendOver(boolean sendOver) {
		this.sendOver = sendOver;
	}

	public boolean isEnd() {
		return end;
	}

	protected void setEnd(boolean end) {
		this.end = end;
	}
}
