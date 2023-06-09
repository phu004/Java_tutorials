#ifndef SHADER_H
#define SHADER_H

#include <thread>
#include <condition_variable>

#include "VertexBufferObject.h"
#include "Vector3D.h"
#include "constants.h"

class Shader {
    
public:

    //控制线程运行的一些变量
    std::thread thread;
    bool isWorking;
    std::mutex mutex;
    std::condition_variable cv;

    //渲染器分配给着色器的顶点缓冲对象在其数组中的的开始和结束的位置
    int VBOsStart, VBOsEnd;

    //当前正在被处理的顶点缓冲对象
    VertexBufferObject* VBO;

    //变换后的三角形顶点
    Vector3D updatedVertices[3];

    //辅助渲染计算的变量
  	Vector3D surfaceNormal;
	Vector3D edge1;
	Vector3D edge2;
	Vector3D tempVector1;
	Vector3D clippedVertices[4];

    //辅助渲染计算顶点亮度的变量
  	Vector3D vertexNormal, lightDirection, reflectionDirection, viewDirection;
  	float clippedLight[4];
  	float local_sinX, local_cosX, local_sinY, local_cosY, local_sinZ, local_cosZ, global_sinY, global_cosY, global_sinX, global_cosX;

    //辅助渲染计算纹理的变量
  	Vector3D uDirection, vDirection, origin, point, V0, V1, V2, pointA, pointB, pointC;
  	float originU, originV,  u1, v1, u2, v2, u3, v3;

    //三角形变换后的顶点的亮度
  	float vertexLightLevel[4];

    //三角形变换后的顶点投影在屏幕上的2D坐标
  	float vertices2D[4][2];

    //视角的原点到屏幕的距离 （以像素为单位）， 这个值越大视角就越狭窄。常用的值为屏宽的2/3
    int screenDistance = 815;

    //Z裁剪平面离视角原点的距离
  	float nearClipDistance = 0.01f;

    //用于记录三角形顶点的深度值
  	float vertexDepth[4];

    //三角形扫描线最高和最低的位置
  	int scanUpperPosition, scanLowerPosition;

     //三角形的最高和最低, 最左和最右的位置
  	float leftMostPosition, rightMostPosition, upperMostPosition, lowerMostPosition;

    //判断三角形是否与屏幕的左边和右边相切
  	bool isClippingRightOrLeft = false;

    //三角形的顶点数, 一般为3。 但当三角形与视角的z平面相切的时候有可能会变成4个 。
  	int verticesCount = 3;

    //判断三角形是否被Z裁剪平面裁剪
  	bool needToBeClipped = false;

    //用于扫描三角形填充区域的两个数组，每行有两个值，分别表示描线的起点和终点的 x 坐标
  	int xLeft[SCREEN_H];
  	int xRight[SCREEN_H];

    //用于扫描三角形深度的两个数组，每行有两个值，分别表示描线的起点和终点的z值
  	float zLeft[SCREEN_H];
  	float zRight[SCREEN_H];

    //用于扫描三角形亮度的两个数组，每行有两个值，分别表示描线的起点和终点的亮度值
  	float lightLeft[SCREEN_H];
  	float lightRight[SCREEN_H];

    //屏幕的深度缓冲
  	float* zBuffer;

    //屏幕的像素组
  	unsigned int* screen;

    //三角形的颜色
  	int triangleColor;
  	
  	//渲染的三角形数
  	int triangleCount = 0;

    Shader(unsigned int* screen, float* zbuffer);

    void run();

    void start();

    void stop();

    void notifyShader();

    //变换三角形的顶点
    void transformVertices();

    void calculateLight(int i);

    bool testHidden();

    void testBoundary();

    bool clipZNearPlane();

    void approximatePoint(int index, Vector3D& behindPoint, Vector3D& frontPoint, float behindLightLevel, float frontLightLevel);

    void scanTriangle();

    void scanSide(int i);

    void handleClipping();

    void rendersolidTriangle_zDepth_shading();

    void renderTriangle_zDepth_shading_textured();

    void renderTriangle_zDepth_shading_barycentric_textured();

   
};

#endif  // SHADER_H