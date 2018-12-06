package tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class LFTPGet extends LFTP {
	
	private ArrayList<Packet> cache = new ArrayList<Packet>();
	private ArrayList<Integer> indexTable = new ArrayList<Integer>();
	
	private String downloadPath = "download/";
	private String fileName = null;

	public LFTPGet(InetAddress dstAddr, int srcPort, int dstPort, int UDPDstPort, Object listLock, MyList list,
			Object socketLock, DatagramSocket socket) throws SocketException {
		super(dstAddr, srcPort, dstPort, UDPDstPort, listLock, list, socketLock, socket);
	}
	
	protected void receiveFile() {
		boolean flag = true;
		Packet packet = null;
		while((packet = receive()) != null || flag) {
			if (packet == null) {

				try {
					synchronized (this) {
						this.wait();
					}	
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {

				flag = false;
				this.updateLastByteRecv(packet.getAckNum());
				this.setFinished(packet.isFIN());
				if (packet.isSYN() && packet.isACK()) {
					this.setDstPort(packet.getSrcPort());
					this.setAckNum(packet.getSeqNum() + 1);
				} else {
					this.updateAckNum(packet.getSeqNum(), packet.getSeqNum() + packet.getData().length);
				}
				this.sendBack();
				if (!packet.isFIN() && !packet.isSYN()) {
					cache.add(packet);
				}
				
				/* write cache into file */
				Iterator<Packet> iter = cache.iterator();
				while (iter.hasNext()) {
					Packet temp = iter.next();
					if (temp.getSeqNum() < this.getAckNum()) {
						packet.save(this.getId());
						indexTable.add(packet.getSeqNum());
						iter.remove();
					}
				}
			}
		}
	}
	
	private void mergeFile() {
		System.out.println("Thread " + this.getId() + "> " + "begin merge the file");
		try {
			File inFile = null;
			FileInputStream in = null;
			byte[] data = new byte[1500];
			
			File outFile = new File(downloadPath + fileName);
			if (!outFile.exists()) {
				outFile.createNewFile();
			}
			FileOutputStream out = new FileOutputStream(outFile);
			Collections.sort(indexTable, new Comparator< Integer >() {
			    @Override
			    public int compare(Integer lhs, Integer rhs) {
			        if ( lhs > rhs ) {
			            return 1;
			        } else if (lhs < rhs){
			            return -1;
			        } else {
			        	return 0;
			        }
			    }
			});
			for (int iter : indexTable) {
				inFile = new File("Cache/" + this.getId() + "@" + iter + ".cache");
				in = new FileInputStream(inFile);
				int len = in.read(data);
				in.close();
				out.write(data, 0, len);
				inFile.delete();
			}
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println("Thread " + this.getId() + "> " + "end merge the file");
		
	}
	
	public void sayHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		System.out.println("Thread " + this.getId() + "> " + "say \"hello\" to " + this.getDstAddr().toString() + " with random seqNum(" + seqNum + ")");
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, false, false, false, seqNum, 0, getFwnd(), fileName.getBytes());
		seqNum += fileName.getBytes().length;
		this.setSeqNum(seqNum);
		this.send(packet);
	}
	
	public void replyHello() {
		int seqNum = (int)(1 + Math.random() * 1000);
		System.out.println("Thread " + this.getId() + "> " + "reply \"hello\" to " + this.getDstAddr().toString() + " with random seqNum(" + seqNum + ")");
		Packet packet = new Packet(getSrcPort(), getDstPort(), true, true, false, false, seqNum++, getAckNum(), getFwnd(), new byte[1]);
		this.setSeqNum(seqNum);
		this.send(packet);
	}
	
	public void setDownloadPath(String path) {
		this.downloadPath = new String(path);
	}
	
	public void setFileName(String name) {
		this.fileName = new String(name);
	}
	
	@Override
	public void run() {
		System.out.println("Thread " + this.getId() + "> " + "start");
		while (!isSendOver()) {
			this.receiveFile();
		}
		
		this.mergeFile();
		System.out.println("Thread " + this.getId() + "> " + "end");
		this.setEnd(true);
	}

}
