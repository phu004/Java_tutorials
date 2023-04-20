

package core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainThread extends JFrame implements KeyListener{
	
	//屏幕的分辨率
    public static int screen_w = 1024;
	public static int screen_h = 682;
	public static int half_screen_w = screen_w/2;
	public static int half_screen_h = screen_h/2;
	public static int screenSize = screen_w * screen_h;
	
	//用Jpanel作为画板
    public static JPanel panel;
    
	//使用一个int数组存处屏幕上像素的数值
	public static int[] screen;
	
	//屏幕图像缓冲区。它提供了在内存中操作屏幕中图像的方法
	public static BufferedImage screenBuffer;
	
	//记载目前已渲染的 帧数
    public static int frameIndex;
    
	//希望达到的每频之间的间隔时间 (毫秒)
	public static int frameInterval = 32;
	
	//cpu睡眠时间，数字越小说明运算效率越高
	public static int sleepTime, averageSleepTime;
	
	//刷新率，及计算刷新率所用到一些辅助参数
	public static int framePerSecond;
	public static long lastDraw;
	public static double thisTime, lastTime;
	
	//程序的入口点
	public static void main(String args[]) {
		new MainThread();
	}
	
	public MainThread(){
		
		//弹出一个宽 为screen_w高为screen_h的Jpanel窗口，并把它放置屏幕中间。
		setTitle("Java软光栅教程 4");
		panel= (JPanel) this.getContentPane();
		panel.setPreferredSize(new Dimension(screen_w, screen_h));
		panel.setMinimumSize(new Dimension(screen_w,screen_h));
		panel.setLayout(null);     
		
		setResizable(false); 
		pack();
		setVisible(true);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width/2-this.getSize().width/2, dim.height/2-this.getSize().height/2);
    	setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	
    	//用TYPE_INT_RGB来创建BufferedImage，然后把屏幕的像素数组指向BufferedImage中的DataBuffer。
    	//这样通过改变屏幕的像素数组(screen[])中的数据就可以在屏幕中渲染出图像
    	screenBuffer =  new BufferedImage(screen_w, screen_h, BufferedImage.TYPE_INT_RGB);
		DataBuffer dest = screenBuffer.getRaster().getDataBuffer();
		screen = ((DataBufferInt)dest).getData();
		
		//初始化查找表
		LookupTables.init();
		
		//初始化光栅渲染器
		Rasterizer.init();
		
		//初始化视角
		Camera.init(0,0,0);
		
		//添加按键监听器
		addKeyListener(this);
		
		//做一个正方体
		float l = 0.5f;
		Vector3D[] vertices = {
		    new Vector3D(-l, -l, -l),
		    new Vector3D(l, -l, -l),
		    new Vector3D(l, l, -l),
		    new Vector3D(-l, l, -l),
		    new Vector3D(-l, -l, l),
		    new Vector3D(l, -l, l),
		    new Vector3D(l, l, l),
		    new Vector3D(-l, l, l)
		};
		//将正方体向前移动3.5f
		Vector3D localTranslation = new Vector3D(0, 0, 4f);
		int[] indices = {
		    0, 1, 2, 2, 3, 0,        // 前
		    1, 5, 6, 6, 2, 1,        // 右
		    5, 4, 7, 7, 6, 5,        // 后
		    4, 0, 3, 3, 7, 4,        // 左
		     3, 2, 6, 6, 7, 3,       // 上
		    4, 5, 1, 1, 0, 4         // 下
		};
		
		Vector3D[][]  cube = new Vector3D[indices.length/3][];
		for(int i = 0; i < cube.length; i++) {
			cube[i] = new Vector3D[] {vertices[indices[i*3+2]],
					                  vertices[indices[i*3+1]],
					                  vertices[indices[i*3]]};
		}
		
		int[] color = {0xFF0000, 0x00FF00, 0x0000FF, 0xFFFF00, 0xFF00FF, 0x00FFFF};
		
		
		//主循环
		while(true) {
			//更新视角
			Camera.update();
			
			//把背景渲染成天蓝色
			screen[0] = (163 << 16) | (216 << 8) | 239; //天蓝色
			for(int i = 1; i < screenSize; i+=i)
				System.arraycopy(screen, 0, screen, i, screenSize - i >= i ? i : screenSize - i);
			
			//画正方体
			Rasterizer.renderType = 0;
			Rasterizer.localRotationY = (frameIndex*2)%360;
			Rasterizer.localRotationX = (frameIndex*2)%360;
			Rasterizer.localRotationZ = (frameIndex*2)%360;
			localTranslation.y+= (Math.cos((float)frameIndex/8)*0.1);
			Rasterizer.localTranslation = localTranslation;
			for(int i =0; i < cube.length; i++) {
				Rasterizer.triangleVertices = cube[i];
				Rasterizer.triangleColor =  color[i/2];
				Rasterizer.rasterize();
			}
		
			//loop每运行一边，帧数就+1
			frameIndex++;
		    int totalSpeedTime = 0;
			while(System.currentTimeMillis()-lastDraw<frameInterval){
				try {
					Thread.sleep(1);
					totalSpeedTime++;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			sleepTime+=totalSpeedTime;
			lastDraw=System.currentTimeMillis();
			//计算当前的刷新率，并尽量让刷新率保持恒定。 
			if(frameIndex%30==0){
				double thisTime = System.currentTimeMillis();
				framePerSecond = (int)(1000/((thisTime - lastTime)/30));
				lastTime = thisTime;
				averageSleepTime=sleepTime/30;
				sleepTime = 0;
			}
			
			//显示当前刷新率
			Graphics2D g2 =(Graphics2D)screenBuffer.getGraphics(); 
			g2.setColor(Color.BLACK);
			g2.drawString("FPS: " + framePerSecond + "      "  +  "Thread Sleep: " + averageSleepTime +  "ms    ", 5, 15);
			
			//把图像发画到显存里，这是唯一要用到显卡的地方
			panel.getGraphics().drawImage(screenBuffer, 0, 0, this);
		}
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		if(e.getKeyChar() == 'w' || e.getKeyChar() == 'W')
			Camera.MOVE_FORWARD = true;
		else if(e.getKeyChar() == 's' || e.getKeyChar() == 'S')
			Camera.MOVE_BACKWARD = true;
		else if(e.getKeyChar() == 'a' || e.getKeyChar() == 'A')
			Camera.SLIDE_LEFT = true;
		else if(e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
			Camera.SLIDE_RIGHT = true;


		if(e.getKeyCode() == KeyEvent.VK_UP)
			Camera.LOOK_UP= true;
		else if(e.getKeyCode() == KeyEvent.VK_DOWN)
			Camera.LOOK_DOWN = true;
		else if(e.getKeyCode() == KeyEvent.VK_LEFT)
			Camera.LOOK_LEFT = true;
		else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
			Camera.LOOK_RIGHT = true;
		
	}

	@Override
	public void keyReleased(KeyEvent e) {
		if(e.getKeyChar() == 'w' || e.getKeyChar() == 'W')
			Camera.MOVE_FORWARD = false;
		else if(e.getKeyChar() == 's' || e.getKeyChar() == 'S')
			Camera.MOVE_BACKWARD = false;
		else if(e.getKeyChar() == 'a' || e.getKeyChar() == 'A')
			Camera.SLIDE_LEFT = false;
		else if(e.getKeyChar() == 'd' || e.getKeyChar() == 'D')
			Camera.SLIDE_RIGHT = false;

		if(e.getKeyCode() == KeyEvent.VK_UP)
			Camera.LOOK_UP= false;
		else if(e.getKeyCode() == KeyEvent.VK_DOWN)
			Camera.LOOK_DOWN = false;
		else if(e.getKeyCode() == KeyEvent.VK_LEFT)
			Camera.LOOK_LEFT = false;
		else if(e.getKeyCode() == KeyEvent.VK_RIGHT)
			Camera.LOOK_RIGHT = false;
		
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	
}
