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
	
	//视角的原点到屏幕的距离 （以像素为单位）， 这个值越大视角就越狭窄。常用的值为屏宽的2/3
    public static int screenDistance = 815;
    
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
  	
    //Z裁剪平面离视角原点的距离
  	public static float nearClipDistance = 0.01f;
  	
  	//三角形所在对象本身坐标系进行的平移变换
  	public static Vector3D localTranslation;
  	
  	//三角形所在对象本身坐标系的旋转变换
  	public static int localRotationX, localRotationY, localRotationZ;
  	
  	
    //辅助渲染计算的矢量类变量
  	public static Vector3D surfaceNormal, edge1, edge2, tempVector1, clippedVertices[];
  	
  	
    //初始化光栅渲染器
  	public static void init(){
  		//初始化三角形变换后的顶点
		updatedVertices = new Vector3D[]{
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0),
		};
		
		//初始化辅助渲染的临时定义的矢量变量
		surfaceNormal = new Vector3D(0,0,0);
		edge1 = new Vector3D(0,0,0);
		edge2 = new Vector3D(0,0,0);
		tempVector1 = new Vector3D(0,0,0);
		clippedVertices = new Vector3D[]{
				new Vector3D(0,0,0), 
				new Vector3D(0,0,0),
				new Vector3D(0,0,0),
				new Vector3D(0,0,0)
			};
		
  	}
  	
    //光栅渲染器的入口
  	public static void rasterize(){
  		//变换三角形的顶点
  		transformVertices();
  		
  		//提前终止对隐藏面的渲染
  		if(testHidden() == true)
  			return;
  		
  		//将在三角形在z裁键平面的部分裁剪掉
  		clipZNearPlane();
  		
  		//将三角形转换为扫描线
  		scanTriangle();
  	
  	    //给三角形的像素着色
  	    renderTriangle();
  	}
  	
    //变换三角形的顶点
  	public static void transformVertices(){
  		
  		float local_sinX = LookupTables.sin[localRotationX]; 
		float local_cosX = LookupTables.cos[localRotationX];
		float local_sinY = LookupTables.sin[localRotationY];
		float local_cosY = LookupTables.cos[localRotationY];
		float local_sinZ = LookupTables.sin[localRotationZ];
		float local_cosZ = LookupTables.cos[localRotationZ];
	  	
  		float global_sinY = LookupTables.sin[(360-Camera.Y_angle)%360];
		float global_cosY = LookupTables.cos[(360-Camera.Y_angle)%360];
		float global_sinX = LookupTables.sin[(360-Camera.X_angle)%360]; 
		float global_cosX = LookupTables.cos[(360-Camera.X_angle)%360];
		
		for(int i = 0; i < 3; i++){
			
			updatedVertices[i].set(triangleVertices[i]);
			
			//将三角形按其所在对象本身坐标系进行变换  	
			updatedVertices[i].rotate_X(local_sinX, local_cosX);
			updatedVertices[i].rotate_Y(local_sinY, local_cosY);
			updatedVertices[i].rotate_Z(local_sinZ, local_cosZ);
			updatedVertices[i].add(localTranslation);
			
			
			//把三角形的原有顶点按视角变换的反方向用来变换
			updatedVertices[i].subtract(Camera.position);
			updatedVertices[i].rotate_Y(global_sinY, global_cosY);
			updatedVertices[i].rotate_X(global_sinX, global_cosX);
		}
  	}
  	
  	//测试隐藏面
  	public static boolean testHidden() {
  		//如果三角形的顶点全部在Z裁剪平面后面，则这个三角形可视为隐藏面
  		boolean allBehindClippingPlane = true;
  		for(int i = 0; i < 3; i++) {
  			if(updatedVertices[i].z >= nearClipDistance) {
  				allBehindClippingPlane = false;
  				break;
  			}
  		}
  		if(allBehindClippingPlane)
  			return true;
  		
  		// 计算三角形表面法线向量并检查其是否朝向视角
  		edge1.set(updatedVertices[1]);
  		edge1.subtract(updatedVertices[0]);
  		edge2.set(updatedVertices[2]);
  		edge2.subtract(updatedVertices[0]);
  		surfaceNormal.cross(edge1, edge2);
  		float dotProduct  = surfaceNormal.dot(updatedVertices[0]);
  		//如果不朝向视角， 则这个三角形可视为隐藏面
  		if(dotProduct >= 0)
  			return true;
  
  		return false;
  	}
  	
  	 //裁剪三角形在z裁剪平面后的部分
  	public static void clipZNearPlane() {
  		//一般情况下三角形顶点数为3，裁剪后有可能变为4。
  		verticesCount = 0;
  		  
  		for(int i = 0; i < 3; i++){
  		    //如果顶点在裁剪平面之前，则不做任何改动
			if(updatedVertices[i].z >= nearClipDistance){
				clippedVertices[verticesCount].set(updatedVertices[i]);
				verticesCount++;
			} 
			//如果顶点在裁剪平面之后，则需要对三角形进行裁剪
			else{
				//找到前一个顶点（即三角形边上与当前顶点相邻的顶点）
				int index = (i+3 - 1)%3;
				if(updatedVertices[index].z >= nearClipDistance){
					//如果前一个顶点在裁剪平面的前面，  就找出当前顶点和前一个顶点之间的线段在裁剪平面的交点
					approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index]);
					verticesCount++;
				}
				//找到后一个顶点（即三角形边上与当前顶点相邻的另一个顶点）
				index = (i+1)%3;
				if(updatedVertices[index].z >= nearClipDistance){
					//如果后一个顶点在裁剪平面的前面，  就找出当前顶点和后一个顶点之间的线段在裁剪平面的交点
					approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index]);
					verticesCount++;
				}
			}
		}
  	}
  	
  	//找出两点之间的线段在裁剪平面的交点
  	public static void approximatePoint(int index, Vector3D behindPoint, Vector3D frontPoint){
  		
  		//交点在线段间位置的比例
		tempVector1.set(frontPoint.x - behindPoint.x, frontPoint.y - behindPoint.y, frontPoint.z - behindPoint.z);
		float ratio = (frontPoint.z- nearClipDistance)/tempVector1.z;
		
		//线段方向矢量乘以这个比例，就可以得到交点的位置
		tempVector1.scale(ratio);
		clippedVertices[index].set(frontPoint.x, frontPoint.y, frontPoint.z);
		clippedVertices[index].subtract(tempVector1);
	}
  	
  
    //将三角形转换为扫描线
  	public static void scanTriangle() {
  		//用投影公式计算顶点在屏幕上的2D坐标
  		for(int i = 0; i < verticesCount; i++) {
			vertices2D[i][0] = half_screen_w + clippedVertices[i].x*screenDistance/clippedVertices[i].z; 
			vertices2D[i][1] = half_screen_h - clippedVertices[i].y*screenDistance/clippedVertices[i].z;
		}
  		
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