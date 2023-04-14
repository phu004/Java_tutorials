package core;

//只用于三角形的渲染
public class Rasterizer {
	//设置屏幕的分辨率
	public static int screen_w = MainThread.screen_w;
	public static int screen_h = MainThread.screen_h;
	public static int half_screen_w = MainThread.half_screen_w;
	public static int half_screen_h = MainThread.half_screen_h;
	
	//屏幕的像素组
	public static int[] screen = MainThread.screen;
	
	//视角的原点到屏幕的距离 （以像素为单位）， 这个值越大视角就越狭窄。常用的值为屏宽的一半
    public static int screenDistance = screen_w/2;
    
    
    //未经变换的三角形顶点
  	public static Vector3D[] triangleVertices;
  	
  	//变换后的三角形顶点
  	public static Vector3D[] updatedVertices;
  	
    //三角形的顶点数, 一般为3。 但当三角形与视角的z平面相切的时候有可能会变成4个 。
  	public static int verticesCount = 3;
  	
    //三角形变换后的顶点投影在屏幕上的2D坐标
  	public static float[][] vertices2D = new float[4][2];
 
    //用于扫描三角形的两个数组，每行有两个值，分别表示描线的起点和终点的 x 坐标
  	public static int[] xLeft = new int[screen_h], xRight = new int[screen_h];
  	
    //三角形扫描线最高和最低的位置
  	public static int scanUpperPosition, scanLowerPosition;
  	
    //三角形的颜色
  	public static int triangleColor;
  	
  	//三角形的类型
  	public static int renderType;
  	
    //初始化光栅渲染器
  	public static void init(){
  		//初始化三角形变换后的顶点
		updatedVertices = new Vector3D[]{
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0),
			new Vector3D(0,0,0),
		};
  	}
  	
  //光栅渲染器的入口
  	public static void rasterize(){
  		//变换三角形的顶点
  		transformVertices();
  		
  		//将三角形转换为扫描线
  		scanTriangle();
  	
  	    //给三角形的像素着色
  	    renderTriangle();
  	}
  	
    //变换三角形的顶点
  	public static void transformVertices(){
  		//由于本节是渲染静态的三角形所以就不用变换了
		updatedVertices[0].set(triangleVertices[0]);
		updatedVertices[1].set(triangleVertices[1]);
		updatedVertices[2].set(triangleVertices[2]);
  	
		//用投影公式求出顶点在屏幕上的2D坐标
		for(int i = 0; i < verticesCount; i++) {
			vertices2D[i][0] = half_screen_w + updatedVertices[i].x*screenDistance/updatedVertices[i].z; 
			vertices2D[i][1] = half_screen_h - updatedVertices[i].y*screenDistance/updatedVertices[i].z;
		}
  	}
  	
    //将三角形转换为扫描线
  	public static void scanTriangle() {
  		//初始化扫描线最高和最低的位置
		scanUpperPosition = screen_h;
		scanLowerPosition = -1;
		
		//计算扫描线用到的辅助参数
		int temp_x;
	
		//扫描三角形的每一个边
		for(int i = 0; i < verticesCount; i++){  
			float[] vertex1 = vertices2D[i];    //获取第一个顶点
			float[] vertex2;
			
			if(i == verticesCount -1 ){    //如果已经处理到最后一个顶点
				vertex2 = vertices2D[0];   //则第二个点为第一个顶点
			}else{
				vertex2 = vertices2D[i+1];   //否则第二个顶点为下一个顶点
			}

			boolean downwards = true;   //默认是下降的边

			
			if (vertex1[1]> vertex2[1]) {    //如果第一个顶点低于第二个顶点
				downwards = false;           //则为上升的边
				float[] temp = vertex1;      //互换两个点，这样可以保证扫描始终从上往下扫
				vertex1 = vertex2;
				vertex2 = temp;
			}
			
			// 忽略水平边
			float dy = vertex2[1] - vertex1[1];
			if (dy == 0) {
				continue;
			}
			
			//得到这条边的最高和最低位
			int startY = Math.max((int)(vertex1[1]) + 1, 0);
			int endY = Math.min((int)(vertex2[1]), screen_h-1);
			
			//更新扫描线整体的最高和最低的位置
			if(startY < scanUpperPosition )
				scanUpperPosition = startY;

			if(endY > scanLowerPosition)
				scanLowerPosition = endY;
				
		
			//计算边的梯度,把浮点转换为整数增加运算效率
			float gradient = (vertex2[0] - vertex1[0])* 2048 /dy;
			int g = (int)(gradient);
			
			int startX = (int)((vertex1[0] *2048) +  (startY - vertex1[1]) * gradient);
			for (int y=startY; y<=endY; y++) {
				temp_x = startX>>11;   //把大小还原
	
			    //把下降边的x值存到扫描线的右边，反之就放到扫描线的左面
				if(downwards){                
					if(temp_x <= screen_w -1)
						xRight[y] = temp_x;
					else
						xRight[y] = screen_w;
				}else{
					if(temp_x >= 0)
						xLeft[y] = temp_x;
					else
						xLeft[y] = 0;
				}
				startX+=g;
			}
		}
  	}
  	

	//给三角形的像素着色
	public static void renderTriangle() {
		//根据三角形的类别选择不同渲染方法
		if(renderType == 0)
			rendersolidTriangle();
	
	}
	
	//画单色的三角形
	public static void rendersolidTriangle() {
		//逐行渲染扫描线
		for(int i = scanUpperPosition; i <= scanLowerPosition; i++){
			int x_left = xLeft[i] ;
			int x_right = xRight[i];
		
			x_left+=i * screen_w;
			x_right+=i * screen_w;
		
			for(int j = x_left; j < x_right; j++){
				screen[j] = triangleColor;
			}
		}

	}
}
