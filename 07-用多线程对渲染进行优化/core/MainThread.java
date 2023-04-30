

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
	
	//总共渲染的三角形数
	public static int triangleCount;
	
	//程序的入口点
	public static void main(String args[]) {
		new MainThread();
	}
	
	public MainThread(){
		
		//弹出一个宽 为screen_w高为screen_h的Jpanel窗口，并把它放置屏幕中间。
		setTitle("Java软光栅教程 7");
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
		
		
		//做一个甜甜圈的场景
		float R = 1.0f; // radius of the torus
		float r = 0.3f; // radius of the tube

		int numSides = 256; // number of sides around the tube
		int numRings = 256; // number of rings around the torus

		Vector3D[] vertices = new Vector3D[numSides * numRings];
		int[] indices = new int[numSides * numRings * 6];

		int index = 0;

		for (int i = 0; i < numRings; i++) {
		    float u = (float)i / numRings * 2.0f * (float)Math.PI;
		    for (int j = 0; j < numSides; j++) {
		        float v = (float)j / numSides * 2.0f * (float)Math.PI;
		        float x = (R + r * (float)Math.cos(v)) * (float)Math.cos(u);
		        float y = (R + r * (float)Math.cos(v)) * (float)Math.sin(u);
		        float z = r * (float)Math.sin(v);
		        vertices[index++] = new Vector3D(x, y, z);
		    }
		}

		index = 0;

		for (int i = 0; i < numRings; i++) {
		    for (int j = 0; j < numSides; j++) {
		        int nexti = (i + 1) % numRings;
		        int nextj = (j + 1) % numSides;
		        int a = i * numSides + j;
		        int b = i * numSides + nextj;
		        int c = nexti * numSides + nextj;
		        int d = nexti * numSides + j;
		        indices[index++] = a;
		        indices[index++] = b;
		        indices[index++] = c;
		        indices[index++] = c;
		        indices[index++] = d;
		        indices[index++] = a;
		    }
		}
		
		VertexBufferObject tortus1 = new VertexBufferObject(vertices, indices);
		VertexBufferObject tortus2 = new VertexBufferObject(vertices, indices);
		VertexBufferObject tortus3 = new VertexBufferObject(vertices, indices);
		VertexBufferObject tortus4 = new VertexBufferObject(vertices, indices);
			
		//主循环
		while(true) {
				
			//更新视角
			Camera.update();
			
			//把背景渲染成天蓝色
			screen[0] = (163 << 16) | (216 << 8) | 239; //天蓝色
			for(int i = 1; i < screenSize; i+=i)
				System.arraycopy(screen, 0, screen, i, screenSize - i >= i ? i : screenSize - i);
			
			
			Rasterizer.prepare();
			
			//画甜甜圈1
			tortus1.triangleColor = 0xCD5C5C;
			tortus1.localRotationY = (frameIndex*2)%360;
			tortus1.localRotationX = (frameIndex*2)%360;
			tortus1.localRotationZ = (frameIndex*2)%360;
			tortus1.localTranslation.set(0.5f, -0.5f, 4.5f);
			Rasterizer.addVBO(tortus1);
			
			//画甜甜圈2
			tortus2.triangleColor = 0x008B8B;
			tortus2.localRotationY = 180;
			tortus2.localRotationX = 0;
			tortus2.localRotationZ = 0;
			tortus2.localTranslation.set(-0.2f, -0.2f, 4.7f);
			Rasterizer.addVBO(tortus2);
			
			//画甜甜圈3
			tortus3.triangleColor = 0xFF7F50;
			tortus3.localRotationY = (frameIndex*2+ 90)%360;
			tortus3.localRotationX = (frameIndex*2+ 180)%360;
			tortus3.localRotationZ = (frameIndex*2+ 90)%360;
			tortus3.localTranslation.set(-1.2f, -0.6f, 5f);
			Rasterizer.addVBO(tortus3);
			
			//画甜甜圈4
			tortus4.triangleColor = 0xD77DE2;
			tortus4.localRotationY = (frameIndex*2+ 45)%360;
			tortus4.localRotationX = (frameIndex*2+ 180)%360;
			tortus4.localRotationZ = (frameIndex*2+ 270)%360;
			tortus4.localTranslation.set(-0.1f, 0.5f, 4.3f);
			Rasterizer.addVBO(tortus4);
			
			Rasterizer.renderScene();
			
		
			//loop每运行一边，帧数就+1
			frameIndex++;
			
			//尽量让刷新率保持恒定。
		    int mySleepTime = 0;
			while(System.currentTimeMillis()-lastDraw<frameInterval){
				try {
					Thread.sleep(1);
					mySleepTime++;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			sleepTime+=mySleepTime;
			lastDraw=System.currentTimeMillis();
			//计算当前的刷新率 
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
			g2.drawString("频数: " + framePerSecond + "      "  +  "CPU使用率: " + (frameInterval-averageSleepTime)*100/frameInterval +  "%       " + "三角形总数： " + triangleCount, 5, 15);
			
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