package core;

public class Vector3D {
	//矢量在 x y, z 轴上的分量
	public float x, y, z;
	
	//构造函数，传入矢量的三个分量
    public Vector3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    //把 x, y, z赋值为另一个  Vector3D 的 x, y, z
    public void set(Vector3D v) {
        this.x=v.x;
        this.y=v.y;
        this.z=v.z;
    }
    
    public void set(float x , float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
    }
    
    //与另一个矢量相加
    public void add(Vector3D v) {
        this.x+=v.x;
        this.y+=v.y;
        this.z+=v.z;
    }
    
    public void add(float x, float y, float z) {
        this.x+=x;
        this.y+=y;
        this.z+=z;
    }
    
    public void add(Vector3D v, float scaler){
    	x += v.x * scaler;
    	y += v.y * scaler;
    	z += v.z * scaler;
    }
    
    //矢量减法
    public void subtract(Vector3D v) {
    	 this.x-=v.x;
         this.y-=v.y;
         this.z-=v.z;
    }
    
    public void subtract(Vector3D v, float scaler){
    	x -= v.x * scaler;
    	y -= v.y * scaler;
    	z -= v.z * scaler;
    }
    
    //矢量点积, 结果代表两个矢量之间的相似程度
    public float dot(Vector3D v2){
		return this.x*v2.x + this.y*v2.y + this.z*v2.z;
	}
    
    public float dot(float x, float y, float z){
		return this.x*x + this.y*y + this.z*z;
	}
    
    //矢量叉积，来求一个与这两个矢量都垂直的矢量
    public void cross(Vector3D v1, Vector3D v2){
		x = v1.y*v2.z - v1.z*v2.y;
		y = v1.z*v2.x - v1.x*v2.z;
		z = v1.x*v2.y - v1.y*v2.x;
	}
    
	public  Vector3D cross(Vector3D v){
		return new Vector3D(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);
	}
    
    //返回矢量的长度
    public float getLength() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    
    //将矢量单位化
	public void unit(){
		float length = getLength();
		x = x/length;
		y = y/length;
		z = z/length;
	}
	
	//将矢量乘以一个标量
    public void scale(float scalar) {
    	x*=scalar;
		y*=scalar;
		z*=scalar;
    }
    
    //绕 Y 轴旋转矢量，使其顺时针旋转指定角度
  	public void  rotate_Y(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_X = x;
  		float old_Z = z;
  		x = cos*old_X + sin*old_Z;
  		z = - sin*old_X + cos*old_Z;
  	}
  	
  	public void  rotate_Y(float sin, float cos){
  		float old_X = x;
  		float old_Z = z;
  		x = cos*old_X + sin*old_Z;
  		z = -sin*old_X + cos*old_Z;
  	}

  	//绕 X 轴旋转矢量，使其顺时针旋转指定角度
  	public void rotate_X(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_Y = y;
  		float old_Z = z;
  		y = cos*old_Y + sin*old_Z;
  		z = -sin*old_Y + cos*old_Z;
  	}
  	
	public void rotate_X(float sin, float cos){
  		float old_Y = y;
  		float old_Z = z;
  		y = cos*old_Y + sin*old_Z;
  		z = -sin*old_Y + cos*old_Z;
  	}
  	
  	//绕 Z 轴旋转矢量，使其顺时针旋转指定角度
  	public void rotate_Z(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_X = x;
  		float old_Y = y;
  		x = cos*old_X + sin*old_Y;
  		y = -sin*old_X + cos*old_Y;
  	}
  	
	public void rotate_Z(float sin, float cos){
  		float old_X = x;
  		float old_Y = y;
  		x = cos*old_X + sin*old_Y;
  		y = -sin*old_X + cos*old_Y;
  	}
    
	
	//将Vector3D换成字符串
    public String toString() {
    	return "(" + x + ", " + y + ", " + z + ")";
    }
}
