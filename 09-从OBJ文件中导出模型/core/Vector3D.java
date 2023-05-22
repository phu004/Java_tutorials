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
    
    public Vector3D(Vector3D v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
    
    //把 x, y, z赋值为另一个  Vector3D 的 x, y, z
    public Vector3D set(Vector3D v) {
        this.x=v.x;
        this.y=v.y;
        this.z=v.z;
        return this;
    }
    
    public Vector3D set(float x , float y, float z) {
        this.x=x;
        this.y=y;
        this.z=z;
        return this;
    }
    
    //与另一个矢量相加
    public Vector3D add(Vector3D v) {
        this.x+=v.x;
        this.y+=v.y;
        this.z+=v.z;
        return this;
    }
    
    public Vector3D add(float x, float y, float z) {
        this.x+=x;
        this.y+=y;
        this.z+=z;
        return this;
    }
    
    public Vector3D add(Vector3D v, float scaler){
    	x += v.x * scaler;
    	y += v.y * scaler;
    	z += v.z * scaler;
    	return this;
    }
    
    //矢量减法
    public Vector3D subtract(Vector3D v) {
    	 this.x-=v.x;
         this.y-=v.y;
         this.z-=v.z;
         return this;
    }
    
    public Vector3D subtract(float x, float y, float z) {
   	 	this.x-=x;
        this.y-=y;
        this.z-=z;
        return this;
    }
    
    public Vector3D subtract(Vector3D v, float scaler){
    	x -= v.x * scaler;
    	y -= v.y * scaler;
    	z -= v.z * scaler;
    	return this;
    }
    
    //矢量点积, 结果代表两个矢量之间的相似程度
    public float dot(Vector3D v2){
		return this.x*v2.x + this.y*v2.y + this.z*v2.z;
	}
    
    public float dot(float x, float y, float z){
		return this.x*x + this.y*y + this.z*z;
	}
    
    //矢量叉积，来求一个与这两个矢量都垂直的矢量
    public Vector3D cross(Vector3D v1, Vector3D v2){
		x = v1.y*v2.z - v1.z*v2.y;
		y = v1.z*v2.x - v1.x*v2.z;
		z = v1.x*v2.y - v1.y*v2.x;
		return this;
	}
    
	public  Vector3D cross(Vector3D v){
		return new Vector3D(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);
	}
    
    //返回矢量的长度
    public float getLength() {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }
    
    //将矢量单位化
	public Vector3D unit(){
		float length = getLength();
		x = x/length;
		y = y/length;
		z = z/length;
		return this;
	}
	
	//将矢量乘以一个标量
    public Vector3D scale(float scalar) {
    	x*=scalar;
		y*=scalar;
		z*=scalar;
		return this;
    }
    
    //绕 Y 轴旋转矢量，使其顺时针旋转指定角度
  	public Vector3D  rotate_Y(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_X = x;
  		float old_Z = z;
  		x = cos*old_X + sin*old_Z;
  		z = - sin*old_X + cos*old_Z;
  		return this;
  	}
  	
  	public Vector3D  rotate_Y(float sin, float cos){
  		float old_X = x;
  		float old_Z = z;
  		x = cos*old_X + sin*old_Z;
  		z = -sin*old_X + cos*old_Z;
  		return this;
  	}

  	//绕 X 轴旋转矢量，使其顺时针旋转指定角度
  	public Vector3D rotate_X(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_Y = y;
  		float old_Z = z;
  		y = cos*old_Y + sin*old_Z;
  		z = -sin*old_Y + cos*old_Z;
  		return this;
  	}
  	
	public Vector3D rotate_X(float sin, float cos){
  		float old_Y = y;
  		float old_Z = z;
  		y = cos*old_Y + sin*old_Z;
  		z = -sin*old_Y + cos*old_Z;
  		return this;
  	}
  	
  	//绕 Z 轴旋转矢量，使其顺时针旋转指定角度
  	public Vector3D rotate_Z(int angle){
  		float sin = LookupTables.sin[angle];
  		float cos = LookupTables.cos[angle];
  		float old_X = x;
  		float old_Y = y;
  		x = cos*old_X + sin*old_Y;
  		y = -sin*old_X + cos*old_Y;
  		return this;
  	}
  	
	public Vector3D rotate_Z(float sin, float cos){
  		float old_X = x;
  		float old_Y = y;
  		x = cos*old_X + sin*old_Y;
  		y = -sin*old_X + cos*old_Y;
  		return this;
  	}
    
	
	//将Vector3D换成字符串
    public String toString() {
    	return "(" + x + ", " + y + ", " + z + ")";
    }
}
