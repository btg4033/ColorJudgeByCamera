
package cameracolorjudge;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

public class CameraColorJudge extends JFrame {
    private JButton connectButton = new JButton("接続");//切断ボタンと
    private CameraPanel cameraPanel;  // カメラ映像用パネル
    private boolean isCameraConnected = false; // カメラ接続フラグ
    
    // 座標・色情報を表示するラベルフィールド
    private JLabel mouseXLabel = new JLabel("mouseX: 0 ");
    private JLabel mouseYLabel = new JLabel("mouseY: 0 ");
    private JLabel colorRLabel = new JLabel("R: 0 ");
    private JLabel colorGLabel = new JLabel("G: 0 ");
    private JLabel colorBLabel = new JLabel("B: 0 ");
    private JLabel colorJudge = new JLabel("判定結果： ");
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new CameraColorJudge();
    }

    CameraColorJudge() {
        setTitle("USB Camera Color Detection");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(640, 600);
        
        // レイアウトを BorderLayout に設定
        setLayout(new BorderLayout());
        
        // カメラパネルを作成し、BorderLayoutの中央 (`Center`) に配置
        cameraPanel = new CameraPanel();
        add(cameraPanel, BorderLayout.CENTER);

        
        // ボタン用パネルを作成し、BorderLayoutの上部 (`South`) に配置
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(connectButton);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // 色、座標情報パネルを作成し、BorderLayoutの下部 (`North`) に配置
        JPanel colorInfoPanel = new JPanel(new FlowLayout());
        colorInfoPanel.add(mouseXLabel);
        colorInfoPanel.add(mouseYLabel);
        colorInfoPanel.add(colorRLabel);
        colorInfoPanel.add(colorGLabel);
        colorInfoPanel.add(colorBLabel);
        colorInfoPanel.add(colorJudge);
        add(colorInfoPanel, BorderLayout.NORTH);
        
        // 接続ボタンのクリックイベントを追加
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!isCameraConnected) { // カメラが未接続なら接続する
                    cameraPanel.connectToCamera();
                    isCameraConnected = true;
                    connectButton.setText("切断"); // ボタンのテキストを「切断」に変更
                } else { // すでに接続されている場合は切断する
                    cameraPanel.disconnectToCamera();
                    isCameraConnected = false;
                    connectButton.setText("接続"); // ボタンのテキストを元に戻す
                    connectButton.setEnabled(true); // 再び押せるようにする
                }
            }
        });

        setVisible(true);
    }
//    @Override
//    public void actionPerformed(ActionEvent ae) {
//    	//接続ボタンを押したとき。接続ボタンはカメラに接続していない場合のみ押せる。
//    	if(ae.getSource() == connectButton && isCameraConnected==false) {
//   
//    	}
//    }
    
    public class CameraPanel extends JPanel implements MouseListener, MouseMotionListener  {
        private VideoCapture camera;
        private BufferedImage image;
        private int mouseX = 0, mouseY = 0;
        private Color mouseColor = null;//黒の場合Color.BLACK
        private String detectedColor = "不明"; // 判定した色の名前
        private static boolean isCameraConnected = false; // カメラ接続フラグ

        public CameraPanel() {
            setSize(640, 480); // カメラ映像のサイズを設定
        }
        
        private void disconnectToCamera() {
            if (camera != null && camera.isOpened()) {
                isCameraConnected = false; // カメラ解放直前でfalse にする
                camera.release(); // カメラを解放
                System.out.println("カメラ接続を解除しました。");
            }
        }

        
        private void connectToCamera() {
            if (camera != null && camera.isOpened()) {
            	System.out.println("カメラは既に接続済み。");
            	return;  // すでに接続済みなら何もしない
            }
            
            camera = new VideoCapture(1); // USBカメラに接続（適宜カメラ名やIDを変更）
            isCameraConnected = true;
            
            addMouseListener(this);
            addMouseMotionListener(this);

            new Thread(() -> {
                Mat frame = new Mat();

                while (isCameraConnected) {
                    if (!camera.read(frame)) {
                        System.out.println("isCameraConnected："+isCameraConnected);
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
            
            // カメラ接続後にマウス動作によらない周期的な色判定スレッドを開始
            startPeriodicColorCheck();
        }
        
        //カメラ接続後、周期的に色の判定
        private void startPeriodicColorCheck() {
            new Thread(() -> {
                while (isCameraConnected) {
                    try {
                        Thread.sleep(1000);  // 1秒ごとに色を判定

                        if (image != null) {
                            int imgWidth = image.getWidth();
                            int imgHeight = image.getHeight();

                            // 範囲チェックを追加
                            if (mouseX >= 0 && mouseX < imgWidth && mouseY >= 0 && mouseY < imgHeight) {
                                int rgb = image.getRGB(mouseX, mouseY);
                                mouseColor = new Color(rgb);
                                detectedColor = getColorName(mouseColor.getRed(), mouseColor.getGreen(), mouseColor.getBlue());
                            } else {
                                detectedColor = "なし"; // 画像外なら「なし」
                            }
                            
                            repaint();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        

//        @Override
//        protected void paintComponent(Graphics g) {
//            super.paintComponent(g);
//            if (image != null) {
//                g.drawImage(image, 0, 0, this);
//            }
//
//            g.setColor(mouseColor);
//            g.fillOval(mouseX - 10, mouseY - 10, 20, 20);
//
//            // 判定した色の情報を画面上に描画
//            Color textColor = (mouseColor.getRed() + mouseColor.getGreen() + mouseColor.getBlue() > 400) ? Color.BLACK : Color.WHITE;
//            g.setColor(textColor);
//            g.drawString("X: " + mouseX + " Y: " + mouseY, 20, 20);
//            g.drawString("Color: " + mouseColor, 20, 40);
//            g.drawString("判定結果: " + detectedColor, mouseX + 15, mouseY - 15); // マウスのすぐ横に表示
//        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image != null) {
                g.drawImage(image, 0, 0, this);
            }

            // カメラ接続時のみマウス位置の円を描画
            if (isCameraConnected) {
                g.setColor(mouseColor);
                g.fillOval(mouseX - 10, mouseY - 10, 20, 20);
            }
            // 文字の色を変更する処理
            if ("白".equals(detectedColor)) {
                g.setColor(Color.BLACK);  // 白のときは黒文字
            } else {
                g.setColor(Color.WHITE);  // それ以外は白文字
            }
            
            // フォントサイズを 1.5 倍に変更
            g.setFont(g.getFont().deriveFont(g.getFont().getSize() * 1.5f));

            //上記g.setColorで以下の表示色を決定
            g.drawString("判定結果: " + detectedColor, mouseX + 15, mouseY - 15); // マウスのすぐ横に表示
        }


        @Override
        public void mouseMoved(MouseEvent e) {
            mouseX = e.getX();
            mouseY = e.getY();

            if (image != null) {
                int imgWidth = image.getWidth();
                int imgHeight = image.getHeight();

                // 範囲チェック：imageの範囲内でのみ色を取得
                if (mouseX >= 0 && mouseX < imgWidth && mouseY >= 0 && mouseY < imgHeight) {
                    int rgb = image.getRGB(mouseX, mouseY);
                    mouseColor = new Color(rgb);

                    // 色の名前を取得して変数に保存
                    detectedColor = getColorName(mouseColor.getRed(), mouseColor.getGreen(), mouseColor.getBlue());

                    // 色情報を更新
                    mouseXLabel.setText("mouseX: " + mouseX);
                    mouseYLabel.setText("mouseY: " + mouseY);
                    colorRLabel.setText("R: " + mouseColor.getRed());
                    colorGLabel.setText("G: " + mouseColor.getGreen());
                    colorBLabel.setText("B: " + mouseColor.getBlue());
                    colorJudge.setText("判定結果: " + detectedColor); // 判定結果をラベルにセット
                } else {
                    // 画像外なら色判定しない
                    detectedColor = "なし";
                    colorJudge.setText("判定結果: なし");
                }
            }

            repaint(); // 画面を更新
        }


        private String getColorName(int r, int g, int b) {
            if (r > 120 && g < 100 && b < 100) {
                return "赤";
            } 
            else if (g > 100 && r < 150 && b < 150) {
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
//        private String getColorName(int r, int g, int b) {
//            int max = Math.max(r, Math.max(g, b));  // RGB の最大値
//            int min = Math.min(r, Math.min(g, b));  // RGB の最小値
//            int diff = max - min; // 色の強さ（コントラスト）
//
//            // 白の判定（すべて200以上、差が少ない）
//            if (r > 200 && g > 200 && b > 200 && diff < 30) {
//                return "白";
//            }
//            // 黒の判定（すべて50以下）
//            if (r < 50 && g < 50 && b < 50) {
//                return "黒";
//            }
//            // 灰色の判定（全体的に暗く、差が少ない）
//            if (max < 170 && diff < 40) {
//                return "灰色";
//            }
//            // 赤の判定（赤が一番強く、かつ緑と青がそれほど強くない）
//            if (r > 160 && r > g + 40 && r > b + 40) {
//                return "赤";
//            }
//            // 緑の判定（緑が一番強く、かつ赤と青がそれほど強くない）
//            if (g > 160 && g > r + 40 && g > b + 40) {
//                return "緑";
//            }
//            // 青の判定（青が一番強く、かつ赤と緑がそれほど強くない）
//            if (b > 160 && b > r + 40 && b > g + 40) {
//                return "青";
//            }
//            // 黄色の判定（赤と緑が強く、青が弱い）
//            if (r > 180 && g > 150 && b < 100) {
//                return "黄色";
//            }
//
//            return "不明";
//        }


//         matToBufferedImageメソッドではOpenCV用Matフォーマット画像をJava標準Swing用のBufferedImageに変換
//         (注) OpenCV の Mat はデフォルトで BGR なので、Java の BufferedImage（RGB）と変換が必要
//		    BGR → RGB の変換は Imgproc.COLOR_BGR2RGB だけでは完全でないこともある
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
