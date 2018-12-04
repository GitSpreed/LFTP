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

public class LFTPSend extends LFTP {
	
	private InputStream in;

	public LFTPSend(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList<Packet> list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	protected void sendFile() {
		byte[] data = new byte[Packet.MAX_PACKET_LENGTH - Packet.MIN_PACKET_LENGTH];
		int bytesNum = 0;
		try {
			while(!this.isWindowFull() && (bytesNum = in.read(data)) != -1 ) {
				Packet packet = new Packet(getSrcPort(), getDstPort(), false, false, false, getSeqNum(), getAckNum(), getFwnd(), Arrays.copyOf(data, bytesNum));
				this.send(packet);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/* TODO check the logic*/
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
			}
		}
	}
	
	public void setFilePath(String path) {
		File file = new File(path);
		if (!file.exists()) {
			this.setFinished(true);
		}
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		this.sayHello();
		while (!this.isFinished()) {
			this.sendFile();
			try {
				this.receiveFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
