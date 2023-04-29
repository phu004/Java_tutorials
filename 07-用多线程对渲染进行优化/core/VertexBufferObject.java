package core;

public class VertexBufferObject {
	
	//顶点缓冲
	public Vector3D[] vertexBuffer;
	
	//索引缓冲
	public int[]  indexBuffer;
	
	//用以存储变换后的顶点的容器
	public Vector3D[] updatedVertexBuffer;
	
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
	
	//构造函数，
	public VertexBufferObject(Vector3D[] vertexBuffer, int[]  indexBuffer) {
		this.vertexBuffer = vertexBuffer;
		this.indexBuffer = indexBuffer;
		this.vertexCount = vertexBuffer.length;
		this.triangleCount = indexBuffer.length/3;
		
		//初始化用以存储变换后的顶点的容器
		updatedVertexBuffer = new Vector3D[vertexCount];
		for(int i = 0; i < vertexCount; i++) {
			updatedVertexBuffer[i] = new Vector3D(0,0,0);
		}
			
		
	}

}
