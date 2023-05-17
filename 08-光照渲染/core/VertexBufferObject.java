package core;

public class VertexBufferObject {
	
	//顶点缓冲
	public Vector3D[] vertexBuffer;
	
	//顶点法量
	public Vector3D[] normals;
	
	//索引缓冲
	public int[]  indexBuffer;
	
	//用以存储变换后的顶点的容器
	public Vector3D[] updatedVertexBuffer;
	
	//用以存储变换后的顶点亮度的容器
	public float[] vertexLightLevelBuffer;
	
	//顶点数
	public int vertexCount;
	
	//三角形数
	public int triangleCount;
	
	//局部坐标系变换的角度
	public int localRotationX, localRotationY, localRotationZ;
	
	//局部的平移变换
	public Vector3D localTranslation = new Vector3D(0,0,0);
	
	//三角形的颜色(渲染单色三角形时才会用到)
	public int triangleColor;
	
  	//三角形的类型
  	public int renderType;
  	
  	//漫反射系数
  	public float kd;
  	
  	//镜面反射系数
  	public float ks;
  	
  	//光源
  	public Light lightSource;
	
	//构造函数，
	public VertexBufferObject(Vector3D[] vertexBuffer, int[]  indexBuffer, Vector3D[] normals, Light lightSource, float kd, float ks) {
		this.vertexBuffer = vertexBuffer;
		this.normals = normals;
		this.indexBuffer = indexBuffer;
		this.vertexCount = vertexBuffer.length;
		this.triangleCount = indexBuffer.length/3;
		this.lightSource = lightSource;
		this.kd = kd;
		this.ks=ks;
		
		//初始化用以存储变换后的顶点的容器
		updatedVertexBuffer = new Vector3D[vertexCount];
		for(int i = 0; i < vertexCount; i++) {
			updatedVertexBuffer[i] = new Vector3D(0,0,0);
		}
		
		//初始化用以存储变换后顶点亮度的容器
		vertexLightLevelBuffer = new float[vertexCount];
			
	}

}
