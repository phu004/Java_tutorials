package core;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainThread extends JFrame{
	
	//屏幕的分辨率
    public static int screen_w = 1024;
	public static int screen_h = 682;
	public static int half_screen_w = screen_w/2;
	public static int half_screen_h = screen_h/2;
	
	//用Jpanel作为画板
    public static JPanel panel;
    
	//使用一个int数组存处屏幕上像素的数值
	public static int[] screen;
	
	//屏幕图像缓冲区。它提供了在内存中操作屏幕中图像的方法
	public static BufferedImage screenBuffer;
	
	//记载目前已渲染的 帧数
    public static int frameIndex;
    
	//希望达到的每频之间的间隔时间 (毫秒)
	public static int frameInterval = 33;
	
	//cpu睡眠时间，数字越小说明运算效率越高
	public static int sleepTime;
	
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
		setTitle("Java软光栅教程 1");
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
		
		//把屏幕渲染成从天蓝色过度到橙色
		while(true) {
			
			int r_skyblue = 163, g_skyblue = 216, b_skyblue = 239; //天蓝色
			int r_orange = 255, g_orange = 128, b_orange = 0;   //橙色
			
			
			//把屏幕渲染成从天蓝色过度到橙色的动态画面
			for(int i = 0; i < screen_w; i++) {
				int p = (i + frameIndex*8)%screen_w;
				for(int j = 0; j < screen_h; j++) {
					float t1 = Math.abs((float)(half_screen_w - p)/half_screen_w);
					float t2 = 1f - t1;
					int r = (int)(r_skyblue*t1 + r_orange*t2);
					int g = (int)(g_skyblue*t1 + g_orange*t2);
					int b = (int)(b_skyblue*t1 + b_orange*t2);
					screen[i + j*screen_w] = (r << 16) | (g << 8) | b;
				}
			}
			
			//loop每运行一边，帧数就+1
			frameIndex++;
		    
			//计算当前的刷新率，并尽量让刷新率保持恒定。 
			if(frameIndex%30==0){
				double thisTime = System.currentTimeMillis();
				framePerSecond = (int)(1000/((thisTime - lastTime)/30));
				lastTime = thisTime;
			}
			sleepTime = 0;
			while(System.currentTimeMillis()-lastDraw<frameInterval){
				try {
					Thread.sleep(1);
					sleepTime++;
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
			lastDraw=System.currentTimeMillis();
			
			//显示当前刷新率
			Graphics2D g2 =(Graphics2D)screenBuffer.getGraphics(); 
			g2.setColor(Color.BLACK);
			g2.drawString("FPS: " + framePerSecond + "   "  +  "Thread Sleep: " + sleepTime +  "ms    ", 5, 15);
			
			//把图像发画到显存里，这是唯一要用到显卡的地方
			panel.getGraphics().drawImage(screenBuffer, 0, 0, this);
		}
		
	}
	
}
