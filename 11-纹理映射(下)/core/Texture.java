package core;

import java.awt.Image;
import java.io.IOException;
import java.awt.image.PixelGrabber;

import javax.imageio.ImageIO;

public class Texture {
	//用于存储纹理像素RGB颜色的数组
	public int[] texture;
	
	//纹理的尺寸
	public int height, width, heightMask, widthMask, widthBits, heightBits;
	
	public Texture(String file, int widthBits , int heightBits){
		String imageFolder = "../image/";
		Image img = null;
		try {
			img = ImageIO.read(getClass().getResource(imageFolder + file));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.widthBits = widthBits;
		this.heightBits = heightBits;

		height = (int)Math.pow(2, heightBits);
		width = (int)Math.pow(2, widthBits);
		
		heightMask = height -1;
		widthMask = width - 1;
		
		texture = new int[width*height];
		
		//把图片转换为像素
		PixelGrabber pg = new PixelGrabber(img, 0, 0, width, height, texture, 0, width);
		try {
			pg.grabPixels();
		}catch(Exception e){
			System.out.println(e);
		}
	}
}
