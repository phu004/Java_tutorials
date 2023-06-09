#ifndef RASTERIZER_H
#define RASTERIZER_H

#include "VertexBufferObject.h"
#include "Shader.h"
#include "Texture.h"

class Rasterizer {
public:
    static VertexBufferObject* VBOs[1000];   //顶点缓冲对象数组，设定最多可以处理1000个顶点缓冲对象
    static int numberOfVBOs;     	//记载每一频加载的VBO个数
    static Shader* shaders[5];     //用来处理着色的线程
    static Texture** textures;

    static void init(unsigned int* screen, float* zbuffer, Texture** textures);     //初始化光栅渲染器
    static void prepare();   //在每一频的开始，把渲染器上一频的信息清除
    static void addVBO(VertexBufferObject* VBO);    //加载一个VBO
    static void renderScene();   //渲染器的入口
};

#endif // RASTERIZER_H