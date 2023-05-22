package core;

public class Light {
	//光源的位置
	public Vector3D position;
	
	//在该光源照射下场景的环境光亮度
	public float la;
	
	public Light(float x, float y, float z, float la) {
		position = new Vector3D(x,y,z);
		this.la = la;
	}

}
