/*
 赤い紙が青く見える件に関して
 matToBufferedImage() で BGR → RGB の変換を適切に行う ことで、赤が青く映る問題が修正されたんですね！🚀✨

📌 今後のポイント
✅ OpenCV の Mat はデフォルトで BGR なので、Java の BufferedImage（RGB）と変換が必要
✅ BGR → RGB の変換は Imgproc.COLOR_BGR2RGB だけでは完全でないこともある
✅ matToBufferedImage() でデータの並びを正しく変換するのが重要！

PCで画面を録画する方法はいくつかあります。以下に主な方法を紹介します。

1. Windowsの「ゲームバー」を使う（Windows 10/11）
手順:
キーボードで【Win + G】を押して、ゲームバーを起動します。
表示されるウィンドウ内の「録画開始」ボタンをクリックすると、画面の録画が始まります。
録画停止は同じくゲームバーから行えます。

 */


package cameratest2;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraColorJudge extends JFrame {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new CameraColorJudge();
    }

    CameraColorJudge() {
        setTitle("USB Camera Color Detection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 480);

        CameraPanel panel = new CameraPanel();
        add(panel);

        setVisible(true);
    }

    public class CameraPanel extends JPanel implements MouseListener, MouseMotionListener {
        private VideoCapture camera;
        private BufferedImage image;
        private int mouseX = 0, mouseY = 0;
        private Color mouseColor = Color.BLACK;
        private String detectedColor = "不明"; // 判定した色の名前

        public CameraPanel() {
            camera = new VideoCapture(1); // USBカメラに接続（適宜カメラ名やIDを変更）
            
            addMouseListener(this);
            addMouseMotionListener(this);

            new Thread(() -> {
                Mat frame = new Mat();

                while (true) {
                    if (!camera.read(frame)) {  
                        System.out.println("⚠ フレームが取得できませんでした！");
                        continue;
                    }

                    if (frame.empty()) {
                        System.out.println("⚠ 空のフレームを取得しました！");
                        continue;
                    } else {
//                        System.out.println("✅ フレーム取得成功！サイズ: " + frame.width() + "x" + frame.height());
                    }

                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2RGB); // 先にBGR → RGB に変換
                    Imgproc.resize(frame, frame, new Size(640, 480)); // 変換後にリサイズ
                    image = matToBufferedImage(frame);

                    repaint();

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            }

            g.setColor(mouseColor);
            g.fillOval(mouseX - 10, mouseY - 10, 20, 20);

            // 判定した色の情報を画面上に描画
            Color textColor = (mouseColor.getRed() + mouseColor.getGreen() + mouseColor.getBlue() > 400) ? Color.BLACK : Color.WHITE;
            g.setColor(textColor);
            g.drawString("X: " + mouseX + " Y: " + mouseY, 20, 20);
            g.drawString("Color: " + mouseColor, 20, 40);
            g.drawString("判定結果: " + detectedColor, mouseX + 15, mouseY - 15); // マウスのすぐ横に表示
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();

            if (image != null && mouseX >= 0 && mouseX < image.getWidth() && mouseY >= 0 && mouseY < image.getHeight()) {
                int rgb = image.getRGB(mouseX, mouseY);
                mouseColor = new Color(rgb);

                // 色の名前を取得して変数に保存
                detectedColor = getColorName(mouseColor.getRed(), mouseColor.getGreen(), mouseColor.getBlue());
            }

            repaint(); // 画面を更新
        }

        private String getColorName(int r, int g, int b) {
            if (r > 120 && g < 100 && b < 100) {
                return "赤";
            } 
            else if (g > 120 && r < 150 && b < 150) {
                return "緑";
            }
            
            else if (b > 120 && r < 90 && g < 150) {
                return "青";
            } 
            
            else if (r > 200 && g > 130 && b < 100) {
                return "黄色";
            } 
            
            else if (r < 50 && g < 50 && b < 50) {
                return "黒";
            } 
            
            else if (r > 170 && g > 170 && b > 170) {
                return "白";
            }
            return "不明";
        }

        /*
         * ✅ OpenCV の Mat はデフォルトで BGR なので、Java の BufferedImage（RGB）と変換が必要
		    BGR → RGB の変換は Imgproc.COLOR_BGR2RGB だけでは完全でないこともある
         */
        private BufferedImage matToBufferedImage(Mat mat) {
            int width = mat.width();
            int height = mat.height();
            int channels = mat.channels();
            byte[] sourcePixels = new byte[width * height * channels];
            mat.get(0, 0, sourcePixels);

            // OpenCVのBGRをRGBに変換
            for (int i = 0; i < sourcePixels.length; i += 3) {
                byte temp = sourcePixels[i]; // Bを保存
                sourcePixels[i] = sourcePixels[i + 2]; // RをBの位置へ
                sourcePixels[i + 2] = temp; // BをRの位置へ
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            final byte[] targetPixels = ((java.awt.image.DataBufferByte) image.getRaster().getDataBuffer()).getData();
            System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
            return image;
        }


        @Override public void mouseClicked(MouseEvent e) {}
        @Override public void mouseEntered(MouseEvent e) {}
        @Override public void mouseExited(MouseEvent e) {}
        @Override public void mousePressed(MouseEvent e) {}
        @Override public void mouseReleased(MouseEvent e) {}
        @Override public void mouseDragged(MouseEvent e) {}
    }
}
