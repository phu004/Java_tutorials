#ifndef CAMERA_H
#define CAMERA_H

#include "Vector3D.h"

class Camera {
public:
    // 视角的位置矢量
    static Vector3D position;

    // 视角的方向矢量
    static Vector3D viewDirection, left, right, tempDirection;

    // 判断视角走位，观察方向的变量
    static bool MOVE_FORWARD, MOVE_BACKWARD, SLIDE_LEFT, SLIDE_RIGHT, LOOK_UP, LOOK_DOWN, LOOK_RIGHT, LOOK_LEFT;

    // 视角在Y轴上的旋转，用来控制向左或向右看
    static int Y_angle;

    // 视角在X轴上的旋转，用来控制向上或向下看
    static int X_angle;

    // 视角改变观察方向的速率，每频可旋转2度
    static int turnRate;

    // 视角改变位置的速度，每频可移动0.03f个单位长度
    static float moveSpeed;

    // 初始化方法
    static void init(float x, float y, float z);

    // 更新视角状态
    static void update();
};

#endif // CAMERA_H
