package core;

public class VertexBufferObject {
	
	//顶点缓冲
	public Vector3D[] vertexBuffer;
	
	//索引缓冲
	public int[]  indexBuffer;
	
	//顶点数
	public int vertexCount;
	
	//三角形数
	public int triangleCount;
	
	//构造函数，
	public VertexBufferObject(Vector3D[] vertexBuffer, int[]  indexBuffer) {
		this.vertexBuffer = vertexBuffer;
		this.indexBuffer = indexBuffer;
		this.vertexCount = vertexBuffer.length;
		this.triangleCount = indexBuffer.length/3;
	}

}
