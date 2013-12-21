package livevideo;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.Socket;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import org.apache.commons.io.FileUtils;

import com.github.sarxos.webcam.Webcam;

class SerializableFile implements Serializable {
	
	private String fileName;

	private static final long serialVersionUID = 1L;
	private byte[] array;

	public SerializableFile(File file) throws IOException {
		fileName = file.getName();		
		this.array = FileUtils.readFileToByteArray(file);
	}
	
	public String getName() {
		return fileName;
	}

	public byte[] getBytes() {
		return array;
	}
}

class Recorder extends JFrame {
	private static final long serialVersionUID = 1L;
	private JLabel picLabel;
	private Socket socket;
	private ObjectOutputStream objectOutputStream;
	private BufferedImage bi = null;
	public AVIOutputStream aviOutputStream = null;
	
	public Recorder(String host) throws Exception {
		//ImageOutputStream x = new ImageOutputStream();
//		aviOutputStream = new AVIOutputStream(new File("video.avi"), AVIOutputStream.VideoFormat.RLE, 8);
		//aviOutputStream = new AVIOutputStream(new File("video.avi"), AVIOutputStream.VideoFormat.JPG, 24);
		socket = new Socket(host, 8888);
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
		picLabel = new JLabel();
		picLabel.setSize(640, 480);
		add(picLabel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void setImage(BufferedImage bufferedImage) throws IOException {
		picLabel.setIcon(new ImageIcon(bufferedImage));
		bi=bufferedImage;

	}
	public void addVideoImage() throws IOException{
		aviOutputStream.writeFrame(bi);
	}
	public void changeOut() throws IOException{
		aviOutputStream.close();
	}
	
	public void sendImage(File f) throws IOException {
		SerializableFile serializableFile  = new SerializableFile(f);
		objectOutputStream.writeObject(serializableFile);
		objectOutputStream.flush();		
	}
}

public class Main {
	
	public static long fps = 100;
	
	public static void main(String[] args) throws Exception {
		final Webcam web = Webcam.getDefault();
		web.setViewSize(new Dimension(640, 480));
		web.open();
		//System.out.println("ARGS: " + args[0] + " " + args[1]);
		final Recorder recorder = new Recorder(args[0]);
		recorder.setSize(640, 480);
		recorder.setVisible(true);
		(new Thread(new Runnable() {
			public void run() {
				while (true) {
					try {
						
						recorder.setImage(web.getImage());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		})).start();
		
		(new Thread(new Runnable() {
			public void run() {
				try {
//					recorder.aviOutputStream = new AVIOutputStream(new File("video.avi"), AVIOutputStream.VideoFormat.RLE, 8);
					File fisierDeScriere = new File("video.avi");
//					if(!fisierDeScriere.createNewFile()) {
//						throw new Exception("create failed");
//					}
					recorder.aviOutputStream = new AVIOutputStream(fisierDeScriere, AVIOutputStream.VideoFormat.JPG, 8);
					long videoFragmentTime = System.currentTimeMillis();
					long frameTime = System.currentTimeMillis();
					while (true) {
						if(System.currentTimeMillis() - videoFragmentTime > 10000){
							recorder.aviOutputStream.close();
							String trueFileName = "video"+System.currentTimeMillis()+".avi";
							fisierDeScriere.renameTo(new File(trueFileName));
							fisierDeScriere = new File(trueFileName);
							recorder.sendImage(fisierDeScriere);							
							fisierDeScriere.delete();
							fisierDeScriere = new File("video.avi");
							recorder.aviOutputStream = new AVIOutputStream(fisierDeScriere, AVIOutputStream.VideoFormat.JPG, 8);
							videoFragmentTime = System.currentTimeMillis();
						}
						if (System.currentTimeMillis() - frameTime > 1000/fps) {
							recorder.addVideoImage();
							frameTime = System.currentTimeMillis();
						}
						
						
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.out.println(e.getMessage());
					e.printStackTrace();
					return;
				}
			}
		})).start();
	}
}