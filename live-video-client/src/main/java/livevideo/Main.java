package livevideo;

import java.awt.Dimension;
import java.awt.Graphics2D;
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
	private BufferedImage bi;
	public AVIOutputStream aviOutputStream = null;

	public Recorder(String host) throws Exception {
		socket = new Socket(host, 8888);
		objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
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

	public void sendImage(File f) throws IOException {
		SerializableFile serializableFile = new SerializableFile(f);
		objectOutputStream.writeObject(serializableFile);
		objectOutputStream.flush();
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
}

public class Main {
	public static int fps = 10;

	public static void main(String[] args) throws Exception {
		final Webcam web = Webcam.getDefault();
		web.setViewSize(new Dimension(640, 480));
		web.open();
		final Recorder recorder = new Recorder(args[0]);
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
							// fisierDeScriere.delete();
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
