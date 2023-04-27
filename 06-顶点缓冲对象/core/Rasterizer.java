package core;

//只用于三角形的渲染
public class Rasterizer {
	//设置屏幕的分辨率
	public static int screen_w = MainThread.screen_w;
	public static int screen_h = MainThread.screen_h;
	public static int half_screen_w = MainThread.half_screen_w;
	public static int half_screen_h = MainThread.half_screen_h;
	public static int screenSize = screen_w * screen_h;
	
	//屏幕的像素组
	public static int[] screen = MainThread.screen;
	
	//屏幕的深度缓冲
	public static float[] zBuffer = MainThread.zBuffer;
	
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
 
    //用于扫描三角形填充区域的两个数组，每行有两个值，分别表示描线的起点和终点的 x 坐标
  	public static int[] xLeft = new int[screen_h], 
  			           xRight = new int[screen_h];
  	
  	//用于扫描三角形深度的两个数组，每行有两个值，分别表示描线的起点和终点的z值
  	public static float[] zLeft = new float[screen_h], 
  			             zRight = new float[screen_h];
  	
    //用于记录三角形顶点的深度值
  	public static float[] vertexDepth = new float[4];
  	
    //三角形扫描线最高和最低的位置
  	public static int scanUpperPosition, scanLowerPosition;
  	
    //三角形的最高和最低, 最左和最右的位置
  	public static float leftMostPosition, rightMostPosition, upperMostPosition, lowerMostPosition;
  	
    //三角形的颜色
  	public static int triangleColor;
  	
  	//三角形的类型
  	public static int renderType;
  	
    //Z裁剪平面离视角原点的距离
  	public static float nearClipDistance = 0.01f;
  	
  	//判断三角形是否被Z裁剪平面裁剪
  	public static boolean needToBeClipped;
  	
  	//三角形所在对象本身坐标系进行的平移变换
  	public static Vector3D localTranslation;
  	
  	//三角形所在对象本身坐标系的旋转变换
  	public static int localRotationX, localRotationY, localRotationZ;
  	
    //辅助渲染计算的矢量类变量
  	public static Vector3D surfaceNormal, edge1, edge2, tempVector1, clippedVertices[];
  	
  	//判断三角形是否与屏幕的左边和右边相切
  	public static boolean isClippingRightOrLeft;
  	
  	//顶点缓冲对象
  	public static VertexBufferObject VBO;
  	
 	//用以存储变换后的顶点的容器
  	public static Vector3D[] updatedVertexBuffer;
  	
  	//渲染器可一次性处理顶点的最多个数
  	public static int maxNumberOfVertex = 262144;
  	
  	
  	
    //初始化光栅渲染器
  	public static void init(){
  		//初始化三角形变换后的顶点
		updatedVertices = new Vector3D[]{
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0),
		};
		
		//初始本身坐标系进行的平移变换
		localTranslation = new Vector3D(0,0,0);
		
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
		
		//初始化存储顶点变换的容器
		updatedVertexBuffer = new Vector3D[maxNumberOfVertex];
		for(int i = 0; i < maxNumberOfVertex; i ++) {
			updatedVertexBuffer[i] = new Vector3D(0,0,0);
		}
  	}
  	
    //光栅渲染器的入口
  	public static void rasterize(){
  		//变换三角形的顶点
  		transformVertices();
  		
  		//对顶点缓冲对象中的三角形进行渲染
  		for(int i = 0; i < VBO.triangleCount; i++) {
  			
  			//用索引缓冲来构建三角形
  			updatedVertices[0] = updatedVertexBuffer[VBO.indexBuffer[i*3+2]];
  			updatedVertices[1] = updatedVertexBuffer[VBO.indexBuffer[i*3+1]];
  			updatedVertices[2] = updatedVertexBuffer[VBO.indexBuffer[i*3]];
  		  		
	  		//测试三角形是否该被渲染出来
	  		if(testHidden() == true)
	  			continue;
	  		
	  		MainThread.triangleCount++;
	  		
	  		//将在三角形在z裁键平面的部分裁剪掉
	  		clipZNearPlane();
	  		
	  		//将三角形转换为扫描线
	  		scanTriangle();
	  	
	  	    //给三角形的像素着色
	  		renderTriangle();
  		}
  		
  		
  	}
  	
    //变换三角形的顶点
  	public static void transformVertices(){
  		
  		//从预先算好的三角函数表中获取变换角度所需的函数值
  		float local_sinX = LookupTables.sin[localRotationX]; 
		float local_cosX = LookupTables.cos[localRotationX];
		float local_sinY = LookupTables.sin[localRotationY];
		float local_cosY = LookupTables.cos[localRotationY];
		float local_sinZ = LookupTables.sin[localRotationZ];
		float local_cosZ = LookupTables.cos[localRotationZ];
	  	
  		float global_sinY = LookupTables.sin[360-Camera.Y_angle];
		float global_cosY = LookupTables.cos[360-Camera.Y_angle];
		float global_sinX = LookupTables.sin[360-Camera.X_angle]; 
		float global_cosX = LookupTables.cos[360-Camera.X_angle];
		
		for(int i = 0; i < VBO.vertexCount; i++) {
			updatedVertexBuffer[i].set(VBO.vertexBuffer[i]);
			
			//将顶点缓冲中的顶点按其所在对象本身坐标系进行变换  	
			updatedVertexBuffer[i].rotate_X(local_sinX, local_cosX);
			updatedVertexBuffer[i].rotate_Y(local_sinY, local_cosY);
			updatedVertexBuffer[i].rotate_Z(local_sinZ, local_cosZ);
			updatedVertexBuffer[i].add(localTranslation);
			
			//把顶点缓冲中的顶点按视角变换的反方向用来变换
			updatedVertexBuffer[i].subtract(Camera.position);
			updatedVertexBuffer[i].rotate_Y(global_sinY, global_cosY);
			updatedVertexBuffer[i].rotate_X(global_sinX, global_cosX);
		}
  	}
  	
  	//测试隐藏面
  	public static boolean testHidden() {
  		//测试 1: 如果三角形的顶点全部在Z裁剪平面后面，则这个三角形可视为隐藏面
  		boolean allBehindClippingPlane = true;
  		for(int i = 0; i < 3; i++) {
  			if(updatedVertices[i].z >= nearClipDistance) {
  				allBehindClippingPlane = false;
  				break;
  			}
  		}
  		if(allBehindClippingPlane)
  			return true;
  			
  		//测试 2: 计算三角形表面法线向量并检查其是否朝向视角
  		edge1.set(updatedVertices[1]);
  		edge1.subtract(updatedVertices[0]);
  		edge2.set(updatedVertices[2]);
  		edge2.subtract(updatedVertices[0]);
  		surfaceNormal.cross(edge1, edge2);
  		float dotProduct  = surfaceNormal.dot(updatedVertices[0]);
  		//如果不朝向视角， 则这个三角形可视为隐藏面
  		if(dotProduct >= 0)
  			return true;
  		
  		
  		//测试 3: 判断三角形是否在屏幕外
  		for(int j = 0; j < 3; j++) {
  			//用投影公式计算顶点在屏幕上的2D坐标
			vertices2D[j][0] = half_screen_w + updatedVertices[j].x*screenDistance/updatedVertices[j].z; 
			vertices2D[j][1] = half_screen_h - updatedVertices[j].y*screenDistance/updatedVertices[j].z;
			
			//获得顶点的深度值
			vertexDepth[j] = 1f/updatedVertices[j].z;
  		}
  		
  		leftMostPosition = screen_w;
		rightMostPosition = -1;
		upperMostPosition = screen_h;
		lowerMostPosition = -1;
  		for(int i = 0; i < 3; i++) {
			//计算这个三角形的最左边和最右边
			if(vertices2D[i][0] <= leftMostPosition)
				leftMostPosition = vertices2D[i][0];
			if(vertices2D[i][0] >= rightMostPosition)
				rightMostPosition = vertices2D[i][0];
			
			//计算这个三角形的最上边和最下边
			if(vertices2D[i][1] <= upperMostPosition)
				upperMostPosition = vertices2D[i][1];
			if(vertices2D[i][1] >= lowerMostPosition)
				lowerMostPosition = vertices2D[i][1];
		}
  		
  		//如果这个三角形的最左边或最右或最上或最下都没有被重新赋值，那么这个三角形肯定在屏幕范围之外，所以不对其进行渲染。
  		if(leftMostPosition == screen_w ||  rightMostPosition == -1 || upperMostPosition == screen_h || lowerMostPosition == -1) {
  			return true;
  		}
  		
  		//判断三角形是否和屏幕的左边和右边相切
  		isClippingRightOrLeft = false;
  		if(leftMostPosition < 0 || rightMostPosition >= screen_w)
  			isClippingRightOrLeft = true;
  		
  			
  		return false;
  	}
  	
  	 //裁剪三角形在z裁剪平面后的部分
  	public static void clipZNearPlane() {
  		//一般情况下三角形顶点数为3，裁剪后有可能变为4。
  		verticesCount = 0;
  		 
  		needToBeClipped = false;
  		
  		for(int i = 0; i < 3; i++){
  		    //如果顶点在裁剪平面之前，则不做任何改动
			if(updatedVertices[i].z >= nearClipDistance){
				clippedVertices[verticesCount].set(updatedVertices[i]);
				verticesCount++;
			} 
			//如果顶点在裁剪平面之后，则需要对三角形进行裁剪
			else{
				needToBeClipped = true;
				
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
  		
  		//如果三角形被裁剪平面裁剪则要重新计算一遍和顶点的有关的参数
  		if(needToBeClipped) {
  			for(int i = 0; i < verticesCount; i++) {
	  			//用投影公式计算顶点在屏幕上的2D坐标
				vertices2D[i][0] = half_screen_w + clippedVertices[i].x*screenDistance/clippedVertices[i].z; 
				vertices2D[i][1] = half_screen_h - clippedVertices[i].y*screenDistance/clippedVertices[i].z;
				
				//获得顶点的深度值
				vertexDepth[i] = 1f/clippedVertices[i].z;
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
        //初始化扫描线最高， 最低， 最左， 最右的位置
		scanUpperPosition = screen_h;
		scanLowerPosition = -1;
		
		//扫描三角形的每一个边
		for(int i = 0; i < verticesCount; i++){  
			float[] vertex1 = vertices2D[i];    //获取第一个顶点
			float[] vertex2;
			
			float depth1 = vertexDepth[i];  //获取第一个顶点的深度值
			float depth2;
		
			if(i == verticesCount -1 ){    //如果已经处理到最后一个顶点
				vertex2 = vertices2D[0];   //则第二个点为第一个顶点
				depth2 = vertexDepth[0];
			}else{
				vertex2 = vertices2D[i+1];   //否则第二个顶点为下一个顶点
				depth2 = vertexDepth[i+1];
			}

			boolean downwards = true;   //默认是下降的边

			if (vertex1[1]> vertex2[1]) {    //如果第一个顶点低于第二个顶点
				downwards = false;           //则为上升的边
				float[] temp = vertex1;      //互换两个点，这样可以保证扫描始终从上往下扫
				vertex1 = vertex2;
				vertex2 = temp;
				
				float tempDepth = depth1;   //调动两个点所对应的深度值
				depth1 = depth2;
				depth2 = tempDepth;
			}
			
			// 忽略水平边
			float dy = vertex2[1] - vertex1[1];
			if(dy ==  0) {
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
			
			
			//计算边的x值变化梯度
			float gradient = (vertex2[0] - vertex1[0]) /dy;
			
			//计算边的深度变化的梯度
			float dz_y = (depth2-depth1)/dy;	
		
			//用线性插值算出这条边高位的x初始值
			float startX = ((vertex1[0]) +  (startY - vertex1[1]) * gradient);
			
			//如果顶点的x值小于0则认该三角形与屏幕的左侧相切
			if(startX < 0 ) {
				isClippingRightOrLeft = true;
			}
			
			float tempZ= depth1  - vertex1[1]*dz_y + startY*dz_y;
			for (int y=startY; y<=endY; y++, startX+=gradient, tempZ+=dz_y) {
			    //把下降边的x值存到扫描线的右边，反之就放到扫描线的左面
				if(downwards){                
					xRight[y] = (int)startX;
					zRight[y] = tempZ;
				}else{
					xLeft[y] = (int)startX;
					zLeft[y] = tempZ;
				}
			}
		}
		
		//当三角形与屏幕的两边相切的时候，我们需要对扫描线数组的两个边缘进行修改，确保它们的值都在0和screen_w之间
		if(isClippingRightOrLeft) {
			int x_left, x_right;
			boolean xLeftInView, xRightInView;
			for(int y = scanUpperPosition; y <= scanLowerPosition; y++){   //对扫描线进行逐行处理
				x_left = xLeft[y];
				x_right = xRight[y];
				
				xLeftInView = x_left >=0 && x_left < screen_w;       //左缘是否在屏幕内
				xRightInView = x_right >0 && x_right < screen_w;     //右缘是否在屏幕内
				
				if(xLeftInView && xRightInView)        //如果都在屏幕内就不做任何处理
					continue;
				
				if(x_left >= screen_w  || x_right <= 0){   //如果出现不符合逻辑的左缘或右缘，那么把这条扫描线的长度设为0以免被渲染出来
					xLeft[y] = 0;
					xRight[y] = 0;
					continue;
				}
				
				float dx =  x_right - x_left;          //算出扫描线的长度
				float dz = zRight[y] - zLeft[y];       //算出扫描线左缘和右缘之间的深度差
	
				if(!xLeftInView){           //如果左缘比屏幕的左边还要左
					xLeft[y] = 0;           //就把左缘设在屏幕的最左端
					zLeft[y] = (zLeft[y] + dz /dx * (0 - x_left) ) ;  //用线性插值算出左缘的深度值
				
				}
				
				if(!xRightInView){          //如果右缘比屏幕的右边还要右
					xRight[y] = screen_w;   //就把右缘设在屏幕的最右端
					zRight[y] = (zRight[y] - dz /dx * (x_right - screen_w));   //用线性插值算出右缘的深度值
				}
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
			
			float z_Left = zLeft[i];
		    float z_Right = zRight[i];
		    
			float dz = (z_Right- z_Left)/(x_right - x_left);  //算出这条扫描线上深度值改变的梯度
			
			x_left+=i * screen_w;
			x_right+=i * screen_w;
			
			for(int j = x_left;  j < x_right; j++, z_Left+=dz){
				if(zBuffer[j] < z_Left) {         //如果深度浅于深度缓冲上的值
					zBuffer[j] = z_Left;          //就更新深度缓冲上的值
					screen[j] = triangleColor;    //并给屏幕上的这个像素上色
				}
			}
		}
	}
}