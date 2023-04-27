package core;

public class Camera {
	//视角的位置矢量
	public static Vector3D position;
	
	//视角的方向矢量
	public static Vector3D viewDirection;
	
	//判断视角走位，观察方向的变量
	public static boolean MOVE_FORWARD, MOVE_BACKWARD, SLIDE_LEFT, SLIDE_RIGHT, LOOK_UP, LOOK_DOWN, LOOK_RIGHT, LOOK_LEFT;
	
	//视角在Y轴上的旋转， 用来控制向左或向右看
	public static int Y_angle;
	
	//视角在X轴上的旋转, 用来控制向上或向下看
	public static int X_angle;
	
	//视角改变观察方向的速率,每频可旋转2度
	public static int turnRate= 2;
	
	//视角改变位置的速度，每频可移动0.03f个单位长度
	public static float moveSpeed = 0.03f;
	
	//初始化方法
	public static void init(float x, float y, float z){
		position = new Vector3D(x,y,z);
		viewDirection = new Vector3D(0, 0, 1);
	}
	
	//更新视角状态
	public static void update(){
		//处理向上看， 并保证仰角不大于等于90度
		if(LOOK_UP){
			X_angle+=turnRate;
			if(X_angle > 89 && X_angle < 180)
				X_angle = 89;
		}
		
		//处理向下看， 并保证俯角不大于等于90度
		if(LOOK_DOWN){
			X_angle-=turnRate;
			if(X_angle < 271 && X_angle > 180)
				X_angle = -89;
		}
		
		// 处理向右看
		if(LOOK_RIGHT){
			Y_angle+=turnRate;
		}
		
		// 处理向左看
		if(LOOK_LEFT){
			Y_angle-=turnRate;
		}
		
		//将 Y_angle 和 X_angle 的值限制在 0-359 的范围内
		Y_angle = (Y_angle + 360) % 360;
		X_angle = (X_angle + 360) % 360;
		
		//更新视角的方向
		viewDirection.set(0,0,1);
		viewDirection.rotate_X(X_angle);
		viewDirection.rotate_Y(Y_angle);
		viewDirection.unit();
		
		//处理向前移动
		if(MOVE_FORWARD){
			position.add(viewDirection, moveSpeed);
		
		}

		//处理后前移动
		if(MOVE_BACKWARD){
			position.subtract(viewDirection, moveSpeed);
		}
		
		//视角方向与一个向下的矢量的叉积结果为视角需要向左移动的方向
		if(SLIDE_LEFT){
			Vector3D left = viewDirection.cross(new Vector3D(0, -1, 0));
			left.unit();
			position.subtract(left, moveSpeed);
		
		}
		
		//视角方向与一个向上的矢量的叉积结果为视角需要向右移动的方向
		if(SLIDE_RIGHT){
			Vector3D right = viewDirection.cross(new Vector3D(0, 1, 0));    
			right.unit();
			position.subtract(right, moveSpeed);
		}
	}
}
