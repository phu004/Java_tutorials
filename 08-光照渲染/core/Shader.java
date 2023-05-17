package core;

public class Shader extends Thread{
	//着色器的名字
	public String name;
	
	//着色器是否在工作
	public boolean isWorking;
	
	//线程的锁
	public  Object myLock;

	//渲染器分配给着色器的顶点缓冲对象在其数组中的的开始和结束的位置
	public int VBOsStart, VBOsEnd;
	
	//当前正在被处理的顶点缓冲对象
	public VertexBufferObject VBO;
	
	//变换后的三角形顶点
  	public Vector3D[] updatedVertices;
  	
  	//辅助渲染计算的变量
  	public Vector3D surfaceNormal, edge1, edge2, tempVector1, clippedVertices[];
  	
  	//辅助渲染计算顶点亮度的变量
  	public Vector3D vertexNormal, lightDirection, reflectionDirection, viewDirection;
  	public float[] clippedLight = new float[4];
  	
  	//三角形变换后的顶点的亮度
  	public float[] vertexLightLevel = new float[4];
  	
  	//三角形变换后的顶点投影在屏幕上的2D坐标
  	public float[][] vertices2D = new float[4][2];
  	
	//视角的原点到屏幕的距离 （以像素为单位）， 这个值越大视角就越狭窄。常用的值为屏宽的2/3
    public int screenDistance = 815;
    
    //Z裁剪平面离视角原点的距离
  	public float nearClipDistance = 0.01f;
    
    //用于记录三角形顶点的深度值
  	public float[] vertexDepth = new float[4];
  	
  	//设置屏幕的分辨率
  	public int screen_w = MainThread.screen_w;
  	public int screen_h = MainThread.screen_h;
  	public int half_screen_w = MainThread.half_screen_w;
  	public int half_screen_h = MainThread.half_screen_h;
  	public int screenSize = screen_w * screen_h;
  	
  	//三角形扫描线最高和最低的位置
  	public  int scanUpperPosition, scanLowerPosition;
  	
    //三角形的最高和最低, 最左和最右的位置
  	public  float leftMostPosition, rightMostPosition, upperMostPosition, lowerMostPosition;
  	
  	//判断三角形是否与屏幕的左边和右边相切
  	public  boolean isClippingRightOrLeft;
  	
  	//三角形的顶点数, 一般为3。 但当三角形与视角的z平面相切的时候有可能会变成4个 。
  	public  int verticesCount = 3;
  	
  	//判断三角形是否被Z裁剪平面裁剪
  	public  boolean needToBeClipped;
  	
  	//用于扫描三角形填充区域的两个数组，每行有两个值，分别表示描线的起点和终点的 x 坐标
  	public  int[] xLeft = new int[screen_h], 
  			      xRight = new int[screen_h];
  	
  	//用于扫描三角形深度的两个数组，每行有两个值，分别表示描线的起点和终点的z值
  	public  float[] zLeft = new float[screen_h], 
  			        zRight = new float[screen_h];
  	
  	//用于扫描三角形亮度的两个数组，每行有两个值，分别表示描线的起点和终点的亮度值
  	public  float[] lightLeft = new float[screen_h], 
		            lightRight = new float[screen_h];
  	
  	//屏幕的深度缓冲
  	public float[] zBuffer;
  	
  	//屏幕的像素组
  	public int[] screen;
  	
    //三角形的颜色
  	public int triangleColor;
  	
  	//渲染的三角形数
  	public int triangleCount;
  	
  	
	//构造函数
	public Shader(int[] screen, String name) {
		myLock = new Object();
		this.name = name;
		
		//初始化深度缓冲
		zBuffer = new float[screenSize];
		
		//初始化屏幕
		if(screen == null) {
			this.screen = new int[screenSize];
		}else {
			this.screen = screen;
		}
		
		//初始化三角形变换后的顶点
		updatedVertices = new Vector3D[]{
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0), 
			new Vector3D(0,0,0),
		};
		
		//初始化辅助用来计算裁剪平面的矢量
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
		
		//初始化辅助用来计算顶点亮度的矢量
		vertexNormal = new Vector3D(0,0,0);
		lightDirection = new Vector3D(0,0,0);
		reflectionDirection = new Vector3D(0,0,0);
		viewDirection = new Vector3D(0,0,0);
	}
	

	@Override
	public void run() {
		
		while(true){
			
			synchronized (this) {
				try {	
					
					synchronized (myLock) {
						myLock.notify();
						isWorking = false;
					}
					wait();    //等待渲染器发布新的命令
					
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
			//锁被打开后, 下面运行渲染任务
			
			//把深度缓冲归零
			zBuffer[0] = 0;
			
			//把三角形数归零
			triangleCount = 0;
			
			
	
			
			for(int i = 1; i < screenSize; i+=i)
				System.arraycopy(zBuffer, 0, zBuffer, i, screenSize - i >= i ? i : screenSize - i);
		
			//对渲染器分配给自己的顶点缓冲对象进行渲染
			for(int j = VBOsStart; j < VBOsEnd; j++) {
	  			VBO = Rasterizer.VBOs[j];
	  			triangleColor = VBO.triangleColor;
	  			
	  				  			
	  			//变换三角形的顶点
	  	  		transformVertices(VBO);
	  	  	
	  	  		
		  		//对顶点缓冲对象中的三角形进行渲染
		  		for(int i = 0; i < VBO.triangleCount; i++) {
		  			
		  			
		  			int firstVertex = VBO.indexBuffer[i*3+2];
		  			int secondVertex = VBO.indexBuffer[i*3+1];
		  			int thirdVertex = VBO.indexBuffer[i*3];
		  			
		  			//用索引缓冲来构建三角形
		  			updatedVertices[0] = VBO.updatedVertexBuffer[firstVertex];
		  			updatedVertices[1] = VBO.updatedVertexBuffer[secondVertex];
		  			updatedVertices[2] = VBO.updatedVertexBuffer[thirdVertex];
		  			
		  			vertexLightLevel[0] = VBO.vertexLightLevelBuffer[firstVertex];
		  			vertexLightLevel[1] = VBO.vertexLightLevelBuffer[secondVertex];
		  			vertexLightLevel[2] = VBO.vertexLightLevelBuffer[thirdVertex];
		  			
			  		//测试三角形是否该被渲染出来
		  			boolean isHidden = testHidden();
		  			if(isHidden)
			  			continue;
		  			
			  		//将在三角形在z裁键平面的部分裁剪掉
			  		isHidden = clipZNearPlane();		
			  		if(isHidden)
			  			continue;
			  		
			  		triangleCount++;
			  		
			  	    //给三角形的像素着色
			  		renderTriangle(VBO.renderType);
			  		
		  		}
	  		}	
		}
	}
	
	//变换三角形的顶点
  	public  void transformVertices(VertexBufferObject VBO){
  		
  		//从预先算好的三角函数表中获取变换角度所需的函数值
  		float local_sinX = LookupTables.sin[VBO.localRotationX]; 
		float local_cosX = LookupTables.cos[VBO.localRotationX];
		float local_sinY = LookupTables.sin[VBO.localRotationY];
		float local_cosY = LookupTables.cos[VBO.localRotationY];
		float local_sinZ = LookupTables.sin[VBO.localRotationZ];
		float local_cosZ = LookupTables.cos[VBO.localRotationZ];
	  	
  		float global_sinY = LookupTables.sin[360-Camera.Y_angle];
		float global_cosY = LookupTables.cos[360-Camera.Y_angle];
		float global_sinX = LookupTables.sin[360-Camera.X_angle]; 
		float global_cosX = LookupTables.cos[360-Camera.X_angle];
		
		for(int i = 0; i < VBO.vertexCount; i++) {
			
			//将顶点缓冲中的顶点恢复初始值
			VBO.updatedVertexBuffer[i].set(VBO.vertexBuffer[i]);
			
			//将顶点缓冲中的顶点按其所在对象本身坐标系进行转换  	
			VBO.updatedVertexBuffer[i].rotate_X(local_sinX, local_cosX);
			VBO.updatedVertexBuffer[i].rotate_Y(local_sinY, local_cosY);
			VBO.updatedVertexBuffer[i].rotate_Z(local_sinZ, local_cosZ);
			VBO.updatedVertexBuffer[i].add(VBO.localTranslation);
			
	
			//将顶点缓冲中的顶点法量恢复初始值
			vertexNormal.set(VBO.normals[i]);
			
			//将顶点缓冲中的顶点法量按其所在对象本身坐标系进行转换  	
			vertexNormal.rotate_X(local_sinX, local_cosX);
			vertexNormal.rotate_Y(local_sinY, local_cosY);
			vertexNormal.rotate_Z(local_sinZ, local_cosZ);
			
			//在顶点按视角的变换转换之前计算顶点亮度
			if(VBO.lightSource != null) {
				//计算顶点的亮度: 顶点亮度 = 环境的亮度 + 漫反射亮度 + 镜面反射亮度
				
				//漫反射亮度 = 漫反射系数 * （顶点法量 点积 顶点到光源方向）
				lightDirection.set(VBO.lightSource.position);
				lightDirection.subtract(VBO.updatedVertexBuffer[i]);
				lightDirection.unit();
				float ld = VBO.kd * (Math.max(vertexNormal.dot(lightDirection), 0));
				
				//光线的反射方向 = 2 * （顶点法量 点积 顶点到光源方向） * 顶点法量 - 顶点到光源方向
				reflectionDirection.set(vertexNormal).scale(2 * vertexNormal.dot(lightDirection)).subtract(lightDirection);
				
				
				//镜面反射亮度 = 镜面反射系数 * （光线的反射方向 点积 顶点到视角方向） ^ n
				viewDirection.set(Camera.position).subtract(VBO.updatedVertexBuffer[i]).unit();
				float ls = viewDirection.dot(reflectionDirection);
				ls = Math.max(ls,0);
				ls = ls*ls*ls*ls;
				ls = ls*ls*ls*ls;     //n = 16;
				ls = VBO.ks * ls;
				VBO.vertexLightLevelBuffer[i] = VBO.lightSource.la + ld + ls;
			}
			
			//把顶点缓冲中的顶点按视角变换的反方向用来转换
			VBO.updatedVertexBuffer[i].subtract(Camera.position);
			VBO.updatedVertexBuffer[i].rotate_Y(global_sinY, global_cosY);
			VBO.updatedVertexBuffer[i].rotate_X(global_sinX, global_cosX);
			
		}
  	}
  	
	//测试隐藏面
  	public  boolean testHidden() {
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
  		leftMostPosition = screen_w;
		rightMostPosition = -1;
		upperMostPosition = screen_h;
		lowerMostPosition = -1;
  		
  		for(int j = 0; j < 3; j++) {
  			//获得顶点的深度值
			vertexDepth[j] = 1f/updatedVertices[j].z;
  			
  			//用投影公式计算顶点在屏幕上的2D坐标
			vertices2D[j][0] = half_screen_w + updatedVertices[j].x*screenDistance*vertexDepth[j]; 
			vertices2D[j][1] = half_screen_h - updatedVertices[j].y*screenDistance*vertexDepth[j];	
			
			if(updatedVertices[j].z >= nearClipDistance) {
				leftMostPosition = Math.min(leftMostPosition, vertices2D[j][0]);
				rightMostPosition = Math.max(rightMostPosition, vertices2D[j][0]);
				upperMostPosition = Math.min(upperMostPosition, vertices2D[j][1]);
				lowerMostPosition = Math.max(lowerMostPosition, vertices2D[j][1]);
			}else {
				float screenX = half_screen_w + updatedVertices[j].x*screenDistance/nearClipDistance; 
				float screenY = half_screen_h - updatedVertices[j].y*screenDistance/nearClipDistance; 
				leftMostPosition = Math.min(leftMostPosition, screenX);
				rightMostPosition = Math.max(rightMostPosition, screenX);
				upperMostPosition = Math.min(upperMostPosition, screenY);
				lowerMostPosition = Math.max(lowerMostPosition, screenY);
			}
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
  	public  boolean clipZNearPlane() {
  		//一般情况下三角形顶点数为3，裁剪后有可能变为4。
  		verticesCount = 0;
  		 
  		needToBeClipped = false;
  		
  		for(int i = 0; i < 3; i++){
  		    //如果顶点在裁剪平面之前，则不做任何改动
			if(updatedVertices[i].z >= nearClipDistance){
				clippedVertices[verticesCount].set(updatedVertices[i]);
				clippedLight[verticesCount] = vertexLightLevel[i];
				verticesCount++;
			} 
			//如果顶点在裁剪平面之后，则需要对三角形进行裁剪
			else{
				needToBeClipped = true;
				
				//找到前一个顶点（即三角形边上与当前顶点相邻的顶点）
				int index = (i+3 - 1)%3;
				if(updatedVertices[index].z >= nearClipDistance){
					//如果前一个顶点在裁剪平面的前面，  就找出当前顶点和前一个顶点之间的线段在裁剪平面的交点
					approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index], vertexLightLevel[i], vertexLightLevel[index]);
					verticesCount++;
				}
				//找到后一个顶点（即三角形边上与当前顶点相邻的另一个顶点）
				index = (i+1)%3;
				if(updatedVertices[index].z >= nearClipDistance){
					//如果后一个顶点在裁剪平面的前面，  就找出当前顶点和后一个顶点之间的线段在裁剪平面的交点
					approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index],  vertexLightLevel[i], vertexLightLevel[index]);
					verticesCount++;
				}
			}
		}
  		
  		//如果三角形被裁剪平面裁剪则要重新计算一遍和顶点的有关的参数
  		if(needToBeClipped) {
  			leftMostPosition = screen_w;
  			rightMostPosition = -1;
  			upperMostPosition = screen_h;
  			lowerMostPosition = -1;
  			
  			for(int i = 0; i < verticesCount; i++) {
  				//获得顶点的深度值
				vertexDepth[i] = 1f/clippedVertices[i].z;
  				
	  			//用投影公式计算顶点在屏幕上的2D坐标
				vertices2D[i][0] = half_screen_w + clippedVertices[i].x*screenDistance*vertexDepth[i]; 
				vertices2D[i][1] = half_screen_h - clippedVertices[i].y*screenDistance*vertexDepth[i];
				
				
  				leftMostPosition = Math.min(leftMostPosition, vertices2D[i][0]);
  				rightMostPosition = Math.max(rightMostPosition, vertices2D[i][0]);
  				upperMostPosition = Math.min(upperMostPosition, vertices2D[i][1]);
  				lowerMostPosition = Math.max(lowerMostPosition, vertices2D[i][1]);
  				
  			}
  			
  			//重新判断三角形是否在屏幕外	
  	  		if(leftMostPosition == screen_w ||  rightMostPosition == -1 || upperMostPosition == screen_h || lowerMostPosition == -1) {
  	  			return true;
  	  		}
  	  		
  	  		//重新判断三角形是否和屏幕的左边和右边相切
  	  		isClippingRightOrLeft = false;
  	  		if(leftMostPosition < 0 || rightMostPosition >= screen_w)
  	  			isClippingRightOrLeft = true;
  	  		
  	  		
  		}
  		
  		
  		
  		return false;
  	}
  	
  	//找出两点之间的线段在裁剪平面的交点
  	public  void approximatePoint(int index, Vector3D behindPoint, Vector3D frontPoint, float behindLightLevel, float frontLightLevel){
  		
  		//交点在线段间位置的比例
		tempVector1.set(frontPoint.x - behindPoint.x, frontPoint.y - behindPoint.y, frontPoint.z - behindPoint.z);
		float ratio = (frontPoint.z- nearClipDistance)/tempVector1.z;
		
		//线段方向矢量乘以这个比例，就可以得到交点的位置
		tempVector1.scale(ratio);
		clippedVertices[index].set(frontPoint.x, frontPoint.y, frontPoint.z);
		clippedVertices[index].subtract(tempVector1);
		
		//计算交点的位置的亮度
		clippedLight[index] = frontLightLevel - (frontLightLevel - behindLightLevel)*ratio;
		

	}
  	
  	
  	
  	//给三角形的像素着色
  	public  void renderTriangle(int renderType) {
  		//根据三角形的类别选择不同渲染方法
  		if(renderType == 0) {
  		
	  		scanTriangle_zDepth_Shading();	
	  	
  		}
  	
  	}
  	
  	//将三角形转换为扫描线 (额外扫描深度值和亮度值)
  	public  void scanTriangle_zDepth_Shading() {
  		
  		//初始化扫描线最高， 最低， 最左， 最右的位置
		scanUpperPosition = screen_h;
		scanLowerPosition = -1;
		
  		
		//扫描三角形的每一个边
		for(int i = 0; i < verticesCount; i++){  
			
			float[] vertex1 = vertices2D[i];    //获取第一个顶点
			float[] vertex2;
			
			float depth1 = vertexDepth[i];  //获取第一个顶点的深度值
			float depth2;
			
			
			float light1 = clippedLight[i]; //获取第一个顶点的亮度值
			float light2;
			
			if(i == verticesCount -1 ){    //如果已经处理到最后一个顶点
				vertex2 = vertices2D[0];   //则第二个点为第一个顶点
				depth2 = vertexDepth[0];
				light2 = clippedLight[0];
			}else{
				vertex2 = vertices2D[i+1];   //否则第二个顶点为下一个顶点
				depth2 = vertexDepth[i+1];
				light2 = clippedLight[i+1];
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
				
				float tempLight = light1;   //调动两个点所对应的亮度值
				light1 = light2;
				light2 = tempLight; 
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
		
			//用线性插值算出这条边高位的x初始值
			float startX = ((vertex1[0]) +  (startY - vertex1[1]) * gradient);
			
			//如果顶点的x值小于0则认该三角形与屏幕的左侧相切
			if(startX < 0 && !isClippingRightOrLeft) {
				startX = leftMostPosition;
			}
			
			//计算边的深度值变化的梯度
			float z_dy = (depth2-depth1)/dy;	
			
			//计算边初始位置的深度值
			float tempZ= depth1 + (startY - vertex1[1])*z_dy;   
			
			//计算边亮度值变化的梯度
			float light_dy = (light2 - light1)/dy;
			
			//计算边初始位置的亮度值
			float tempLight= light1 + (startY - vertex1[1])*light_dy;   
			
			for (int y=startY; y<=endY; y++, startX+=gradient, tempZ+=z_dy, tempLight+=light_dy) {
			    //把下降边的x值存到扫描线的右边，反之就放到扫描线的左面
				if(downwards){                
					xRight[y] = (int)startX;
					zRight[y] = tempZ;
					lightRight[y] = tempLight;
				}else{
					xLeft[y] = (int)startX;
					zLeft[y] = tempZ;
					lightLeft[y] = tempLight;
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
				
				if(xLeftInView && xRightInView && x_right >= x_left)        //如果都在屏幕内就不做任何处理
					continue;
				
				if(x_left >= screen_w  || x_right <= 0 || x_right < x_left ){   //如果扫描线在屏幕外边，那么把这条扫描线的长度设为0以免被渲染出来
					xLeft[y] = 0;
					xRight[y] = 0;
					continue;
				}
				
				
				
				float dx =  x_right - x_left;          //算出扫描线的长度
				float dz = zRight[y] - zLeft[y];       //算出扫描线左缘和右缘之间的深度差
				float dlight = lightRight[y] - lightLeft[y];       //算出扫描线左缘和右缘之间的亮度差
				
				if(!xLeftInView){           //如果左缘比屏幕的左边还要左
					xLeft[y] = 0;           //就把左缘设在屏幕的最左端
					zLeft[y] = (zLeft[y] + dz /dx * (0 - x_left) ) ;  //用线性插值算出左缘的深度值
					lightLeft[y] = (lightLeft[y] + dlight /dx * (0 - x_left) ) ;  //用线性插值算出左缘的亮度值
				
				}
				
				if(!xRightInView){          //如果右缘比屏幕的右边还要右
					xRight[y] = screen_w;   //就把右缘设在屏幕的最右端
					zRight[y] = (zRight[y] - dz /dx * (x_right - screen_w));   //用线性插值算出右缘的深度值
					lightRight[y] = (lightRight[y] - dlight /dx * (x_right - screen_w));   //用线性插值算出右缘的亮度值
				}
			}
		}
		
		rendersolidTriangle_zDepth_shading();
  	}
  	
  	//画单色的三角形 （填充深度值，使用亮度）
  	public void rendersolidTriangle_zDepth_shading() {
  		//逐行渲染扫描线
  		for(int i = scanUpperPosition; i <= scanLowerPosition; i++){
  			int x_left = xLeft[i] ;
  			int x_right = xRight[i];
  					
  			float z_Left = zLeft[i];
  		    float z_Right = zRight[i];
  		    
  		   
  		    float lightLevel = lightLeft[i];
  		    float light_right = lightRight[i];
   
  		    
  			float dz = (z_Right- z_Left)/(x_right - x_left);  //算出这条扫描线上深度值改变的梯度

  			float dlight = (light_right- lightLevel)/(x_right - x_left);  //算出这条扫描线上亮度值改变的梯度
  			
  			x_left+=i * screen_w;
  			x_right+=i * screen_w;
  			
  			for(int j = x_left;  j < x_right; j++, z_Left+=dz, lightLevel+=dlight){
  				if(zBuffer[j] < z_Left) {         //如果深度浅于深度缓冲上的值
  					zBuffer[j] = z_Left;          //就更新深度缓冲上的值
  					int r = (triangleColor >> 16);
  				    int g = (triangleColor >> 8) & 0xFF;
  				    int b = triangleColor & 0xFF;
  				    
  				    r*=lightLevel;
  				    g*=lightLevel;
  				    b*=lightLevel;
  				    
  				    r = (int) Math.min(r, 255);
				    g = (int) Math.min(g, 255);
				    b = (int) Math.min(b, 255);
  				    
  					screen[j] = (r << 16) | (g << 8) | b;  //并给屏幕上的这个像素上色
  				}
  			}
  		}
  		
  	}

}
