package livevideo;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
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
	private String token;

	public SerializableFile() {

	}

	public SerializableFile(String token) {
		this.token = token;
	}

	public SerializableFile(File file, String token) throws IOException {
		this.token = token;
		fileName = file.getName();
		this.array = FileUtils.readFileToByteArray(file);
	}

	public String getName() {
		return fileName;
	}

	public String getToken() {
		return token;
	}

	public byte[] getBytes() {
		return array;
	}
}

class Recorder extends JFrame {
	private static final long serialVersionUID = 1L;
	private JLabel picLabel;
	private BufferedImage bi;
	private String host;
	private int port;
	private String token;
	public AVIOutputStream aviOutputStream = null;

	private void retrieveToken() throws Exception {
		Socket socket = new Socket(host, port);
		ObjectOutputStream oos = new ObjectOutputStream(
				socket.getOutputStream());
		oos.writeObject(new SerializableFile("0"));
		oos.flush();
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		SerializableFile sf = (SerializableFile) ois.readObject();
		token = sf.getToken();
		socket.close();
	}

	public Recorder(String host, int port) throws Exception {
		this.host = host;
		this.port = port;
		retrieveToken();
		picLabel = new JLabel();
		picLabel.setSize(640, 480);
		add(picLabel);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}

	public void setImage(BufferedImage bufferedImage) throws IOException {
		picLabel.setIcon(new ImageIcon(horizontalflip(bufferedImage)));
		bi = bufferedImage;
	}

	public void addVideoImage() throws IOException {
		if (bi == null) {
			return;
		}
		aviOutputStream.writeFrame(bi);
	}

	public void changeOut() throws IOException {
		aviOutputStream.close();
	}

	public void sendImage(File f) throws Exception {
		sendSF(new SerializableFile(f, token));
	}

	public static BufferedImage horizontalflip(BufferedImage img) {
		int w = img.getWidth();
		int h = img.getHeight();
		BufferedImage dimg = new BufferedImage(w, h, img.getColorModel()
				.getTransparency());
		Graphics2D g = dimg.createGraphics();
		g.drawImage(img, 0, 0, w, h, w, 0, 0, h, null);
		g.dispose();
		return dimg;
	}

	public void sendSF(SerializableFile sf) throws Exception {
		Socket socket = new Socket(host, port);
		ObjectOutputStream oos = new ObjectOutputStream(
				socket.getOutputStream());
		oos.writeObject(sf);
		oos.flush();
		socket.close();
	}
}

public class Main {
	public static int fps = 10;

	public static void main(String[] args) throws Exception {
		String host;
		int port;
		if (args.length < 2) {
			port = 8888;
			if (args.length == 0)
				host = "livevideo-elb-receive-926360022.eu-west-1.elb.amazonaws.com";
			else
				host = args[0];
		} else {
			host = args[0];
			port = Integer.parseInt(args[1]);
		}
		host = InetAddress.getByName(host).getHostAddress();
		final Webcam web = Webcam.getDefault();
		web.setViewSize(new Dimension(640, 480));
		web.open();
		final Recorder recorder = new Recorder(host, port);
		recorder.setSize(640, 480);
		recorder.setVisible(true);
		final Thread videoMaker = (new Thread(new Runnable() {
			public void run() {
				try {
					File fisierDeScriere = new File("video.avi");
					recorder.aviOutputStream = new AVIOutputStream(
							fisierDeScriere, AVIOutputStream.VideoFormat.JPG,
							24);
					recorder.aviOutputStream.setFrameRate(fps);
					long videoFragmentTime = System.nanoTime();
					long frameTime = System.nanoTime();
					long md = 1000;
					md *= 1000;
					md *= 1000;
					while (true) {
						if (System.nanoTime() - videoFragmentTime >= 10 * md) {
							recorder.aviOutputStream.close();
							String trueFileName = "video"
									+ System.currentTimeMillis() + ".avi";
							fisierDeScriere.renameTo(new File(trueFileName));
							fisierDeScriere = new File(trueFileName);
							recorder.sendImage(fisierDeScriere);
							fisierDeScriere.delete();
							fisierDeScriere = new File("video.avi");
							recorder.aviOutputStream = new AVIOutputStream(
									fisierDeScriere,
									AVIOutputStream.VideoFormat.JPG, 24);
							recorder.aviOutputStream.setFrameRate(fps);
							videoFragmentTime = System.nanoTime();
						}

						if (System.nanoTime() - frameTime >= md / fps) {
							recorder.addVideoImage();
							frameTime = System.nanoTime();
						}
					}
				} catch (Exception e) {
					System.out.println(e.getMessage());
					e.printStackTrace();
					return;
				}
			}
		}));
		(new Thread(new Runnable() {
			public void run() {
				try {
					recorder.setImage(web.getImage());
					videoMaker.start();
					while (true)
						recorder.setImage(web.getImage());
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		})).start();
	}
}
