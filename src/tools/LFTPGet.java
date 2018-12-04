package tools;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;

public class LFTPGet extends LFTP {
	
	private ArrayList<Packet> cache = new ArrayList<Packet>();

	public LFTPGet(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList<Packet> list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	protected void receiveFile() throws IOException {
		boolean flag = true;
		Packet packet = null;
		while((packet = receive()) != null || flag) {
			if (packet == null) {
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				flag = false;
				this.updateAckNum(packet.getSeqNum(), packet.getSeqNum() + packet.getData().length);
				this.updateLastByteRecv(packet.getAckNum());
				this.setFinished(packet.isFIN());
				this.sendBack();
				/*TODO write cache into file */
			}
		}
	}
	
	@Override
	public void run() {
		this.replyHello();
		while (!this.isFinished()) {
			try {
				this.receiveFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}

}
