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
	
	//使用一个float数组来存储屏幕的深度缓冲值
	public static float[] zBuffer;
	
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
	
	//渲染用到的贴图
    public static Texture[] textures;
	
	//程序的入口点
	public static void main(String[] args) {
		new MainThread();
	}
	
	public MainThread(){
		
		
		
		//弹出一个宽 为screen_w高为screen_h的Jpanel窗口，并把它放置屏幕中间。
		setTitle("Java软光栅教程 11");
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
		
		zBuffer = new float[screenSize];
		
		//初始化查找表
		LookupTables.init();
		
		//初始化光栅渲染器
		Rasterizer.init();
		
		//初始化视角
		Camera.init(0,1,0);
		
		//添加按键监听器
		addKeyListener(this);

		//开始一个守护线程，该线程将永远处于睡眠状态，用来稳定刷新率
		Thread   dt   =   new   Thread(new   DaemonThread() );
		dt.setDaemon(true);
		dt.start();
		
		//读取纹理贴图
		textures = new Texture[5];
		textures[0] = new Texture("background.jpg", 9, 9);
		textures[1] = new Texture("ground.jpg", 9, 9);
		textures[2] = new Texture("bunny.jpg", 11, 11);
		textures[3] = new Texture("capsule0.jpg", 11, 10);
		textures[4] = new Texture("bunny2.jpg", 11, 11);
		
		
		//定义光源
		Light lightSource1 = new Light(0, 300f, -100f, 0.6f);
		Light lightSource2 = new Light(0, 300f, -100f, 1f);
		
		
		float kd = 0.2f; float ks=0.6f;
		
		//由obj文件构建兔子模型
		Mesh bunnyMesh = new Mesh("objs/bunny.obj", "clockwise");
		VertexBufferObject[] bunnies = new VertexBufferObject[4];
		for(int i = 0; i < bunnies.length; i++) {
			bunnies[i] = new VertexBufferObject(bunnyMesh, lightSource1, kd, ks);
			if(i == 0)
				bunnies[i].textureIndex =3;
			else if(i == 2)
				bunnies[i].textureIndex =4;
			else
				bunnies[i].textureIndex =2;
			bunnies[i].renderType = VertexBufferObject.barycentric_textured;
			bunnies[i].scale = 7f;
		}
		
		//构建背景贴图的模型
		Vector3D[] backgroundVertices = new Vector3D[] {new Vector3D(-4,5.9f,12), new Vector3D(-4,-2.1f,12), new Vector3D(4,-2.1f,12), new Vector3D(4,5.9f,12)};
		Vector3D[] backgroundNormals = new Vector3D[] {new Vector3D(0,0,-1), new Vector3D(0,0,-1),new Vector3D(0,0,-1),new Vector3D(0,0,-1)};
		Vector3D[][] backgroundUVDirections = new Vector3D[2][];
		backgroundUVDirections[0] = new Vector3D[] {new Vector3D(1f,0,0), new Vector3D(0, -1f,0)};
		backgroundUVDirections[1] = new Vector3D[] {new Vector3D(1f,0,0), new Vector3D(0, -1f,0)};
		int[] backgroundIndices = new int[] {0,2,1,0,3,2};
		VertexBufferObject background = new VertexBufferObject(backgroundVertices, backgroundUVDirections, backgroundNormals, backgroundIndices, lightSource2, 0, 0);
		background.textureIndex = 0;
		background.renderType = VertexBufferObject.textured;
		background.textureScale = new float[][] {new float[] {0.125f, 0.125f},  new float[] {0.125f, 0.125f}};
		
	
		//构建地面贴图的模型
		Vector3D[] groundVertices = new Vector3D[] {new Vector3D(-20,-2.1f,30), new Vector3D(-20,-2.1f,-10), new Vector3D(20,-2.1f,-10), new Vector3D(20,-2.1f,30)};
		Vector3D[] groundNormals = new Vector3D[] {new Vector3D(0,1,0), new Vector3D(0,1,0),new Vector3D(0,1,0),new Vector3D(0,1,0)};
		Vector3D[][] groundUVDirections = new Vector3D[2][];
		groundUVDirections[0] = new Vector3D[] {new Vector3D(1f,0,0), new Vector3D(0, 0f,-1)};
		groundUVDirections[1] = new Vector3D[] {new Vector3D(1f,0,0), new Vector3D(0, 0f,-1)};
		int[] groundIndices = new int[] {0,2,1,0,3,2};
		VertexBufferObject ground = new VertexBufferObject(groundVertices, groundUVDirections, groundNormals, groundIndices, lightSource2, 0, 0);
		ground.textureIndex = 1;
		ground.renderType = VertexBufferObject.textured;
		ground.textureScale = new float[][] {new float[] {0.25f, 0.25f},  new float[] {0.25f, 0.25f}};
		
		
		//主循环
		while(true) {
			
			
			
			//三角形数归零
			triangleCount = 0;
			
			//更新视角
			Camera.update();
			
			//清零深度缓冲
			zBuffer[0] = 0;
			for(int i = 1; i < screenSize; i+=i)
				System.arraycopy(zBuffer, 0, zBuffer, i, screenSize - i >= i ? i : screenSize - i);
			
			//把屏幕渲染成天蓝色
			screen[0] = (163 << 16) | (216 << 8) | 239; //天蓝色
			for(int i = 1; i < screenSize; i+=i)
				System.arraycopy(screen, 0, screen, i, screenSize - i >= i ? i : screenSize - i);
			
			Rasterizer.prepare();
			
			//渲染兔子模型
		
			bunnies[0].localRotationY = (frameIndex)%360;
			bunnies[0].localTranslation.set(1.2f, -2.1f, 7.7f);
			bunnies[0].triangleColor = 0xffffff;
		
			bunnies[1].localTranslation.set(0.3f, -2.1f, 8.9f);
			bunnies[1].localRotationY = 40;
			bunnies[1].triangleColor = 0x006B6B;
			
			bunnies[2].localRotationY = 360 -(frameIndex*3%360);
			bunnies[2].localTranslation.set(-1.1f, -2.1f, 8.4f);
			bunnies[2].triangleColor = 0xE56B3C;
			
			bunnies[3].localRotationY = (frameIndex*2+ 145)%360;
			bunnies[3].localTranslation.set(0f, -2.1f, 7.7f);
			bunnies[3].triangleColor = 0xB361B9;
			
			for(int i = 0; i < bunnies.length; i++) {
			
				Rasterizer.addVBO(bunnies[i]);
			}
			
			//渲染背景
			Rasterizer.addVBO(background);

			//渲染对面
			Rasterizer.addVBO(ground);
			
			Rasterizer.renderScene();
			
			
	
			//loop每运行一边，帧数就+1
			frameIndex++;
			
			//尽量让刷新率保持恒定。
		    int mySleepTime = 0;
		    int processTime = (int)(System.currentTimeMillis()-lastDraw);
		    if(processTime < frameInterval && lastDraw!= 0) {
		    	mySleepTime = frameInterval-processTime;
		    	try {
					Thread.sleep(0);
					
					
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
			g2.drawString("频数: " + framePerSecond + "      "  + "三角形总数： " + triangleCount, 5, 15);
			
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
