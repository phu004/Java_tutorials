#include "Camera.h"
#include <cmath>

Vector3D Camera::position;
Vector3D Camera::viewDirection;
Vector3D Camera::left;
Vector3D Camera::right;
Vector3D Camera::tempDirection;
bool Camera::MOVE_FORWARD = false, 
     Camera::MOVE_BACKWARD = false, 
    Camera::SLIDE_LEFT = false, 
    Camera::SLIDE_RIGHT = false, 
    Camera::LOOK_UP = false, 
    Camera::LOOK_DOWN = false, 
    Camera::LOOK_RIGHT = false, 
    Camera::LOOK_LEFT = false;
int Camera::Y_angle = 0;
int Camera::X_angle = 0;
int Camera::turnRate = 2;
float Camera::moveSpeed = 0.03f;

void Camera::init(float x, float y, float z) {
    position.set(x, y, z);
  
}

void Camera::update() {
    

    // 处理向上看，并保证仰角不大于等于90度
    if (LOOK_UP) {
        X_angle += turnRate;
        if (X_angle > 89 && X_angle < 180)
            X_angle = 89;
    }

    // 处理向下看，并保证俯角不大于等于90度
    if (LOOK_DOWN) {
        X_angle -= turnRate;
        if (X_angle < 271 && X_angle > 180)
            X_angle = -89;
    }

    // 处理向右看
    if (LOOK_RIGHT) {
        Y_angle += turnRate;
    }

    // 处理向左看
    if (LOOK_LEFT) {
        Y_angle -= turnRate;
    }

    // 将 Y_angle 和 X_angle 的值限制在 0-359 的范围内
    Y_angle = (Y_angle + 360) % 360;
    X_angle = (X_angle + 360) % 360;

    // 更新视角的方向
    viewDirection.set(0, 0, 1);
    viewDirection.rotateX(sin(X_angle * M_PI / 180.0),  cos(X_angle * M_PI / 180.0));
    viewDirection.rotateY(sin(Y_angle * M_PI / 180.0),  cos(Y_angle * M_PI / 180.0));
    viewDirection.unit();

    // 处理向前移动
    if (MOVE_FORWARD) {
        
        position.add(viewDirection, moveSpeed);
       
    }

    // 处理后前移动
    if (MOVE_BACKWARD) {
       
        position.subtract(viewDirection, moveSpeed);
    }

    // 视角方向与一个向下的矢量的叉积结果为视角需要向左移动的方向
    if (SLIDE_LEFT) {
        tempDirection.set(0, -1, 0);
        left.cross(viewDirection, tempDirection);
        left.unit();
        position.subtract(left, moveSpeed);
    }

    // 视角方向与一个向上的矢量的叉积结果为视角需要向右移动的方向
    if (SLIDE_RIGHT) {
        tempDirection.set(0, 1, 0);
        right.cross(viewDirection, tempDirection);
        right.unit();
        position.subtract(right, moveSpeed);
    }
    
}
