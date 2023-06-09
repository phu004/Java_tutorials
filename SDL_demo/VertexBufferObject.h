#ifndef VERTEXBUFFEROBJECT_H
#define VERTEXBUFFEROBJECT_H

#include "Vector3D.h"
#include "Light.h"
#include "Mesh.h"

class VertexBufferObject {
public:
    Vector3D* vertexBuffer;        // 顶点缓冲
    Vector3D* normals;             // 顶点法量
    Vector3D** UVDirections;       // 顶点的纹理方向
    float** uvCoordinates;         // 顶点的纹理坐标
    int* indexBuffer;              // 索引缓冲
    Vector3D* updatedVertexBuffer; // 用以存储变换后的顶点的容器
    float* vertexLightLevelBuffer; // 用以存储变换后的顶点亮度的容器
    int vertexCount;               // 顶点数
    int triangleCount;             // 三角形数
    float scale = 1;                   // 大小的变换
    int localRotationX = 0, localRotationY = 0, localRotationZ = 0; // 局部坐标系变换的角度
    Vector3D localTranslation;      // 局部的平移变换
    int triangleColor;              // 三角形的颜色(渲染单色三角形时才会用到)
    int renderType = 0;                 // 三角形的类型
    static const int soildColor = 0;
    static const int textured = 1;
    static const int barycentric_textured = 2;
    float kd;                       // 漫反射系数
    float ks;                       // 镜面反射系数
    Light* lightSource;             // 光源
    int textureIndex;               // 模型用到的贴图的索引
    float** textureScale;           // 贴图纹理坐标方向的重复数

    void prepareResource();         // 初始化资源的方法
    VertexBufferObject(Vector3D* vertexBuffer, int vertexCount, int triangleCount, Vector3D** UVDirections, Vector3D* normals, int* indexBuffer, Light* lightSource, float kd, float ks);
    VertexBufferObject(Mesh* mesh, Light* lightSource, float kd, float ks);
    ~VertexBufferObject();

    // Add other member functions as needed
};

#endif /* VERTEXBUFFEROBJECT_H */
