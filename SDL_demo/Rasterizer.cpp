#include "Rasterizer.h"
#include "globals.h"

//顶点缓冲对象数组，设定最多可以处理1000个顶点缓冲对象
VertexBufferObject* Rasterizer::VBOs[1000];

//记载每一频加载的VBO个数
int Rasterizer::numberOfVBOs;

//用来处理着色的线程
Shader* Rasterizer::shaders[5];

Texture** Rasterizer::textures;

//初始化光栅渲染器
void Rasterizer::init(unsigned int* screen, float* zbuffer, Texture** textures) {
    Rasterizer::textures = textures;

    int size = sizeof(shaders)/sizeof(shaders[0]);

    //初始化着色器线程并让它们运行起来
    for (int i = 0; i < size; i++) {
        shaders[i] = new Shader(screen, zbuffer);
        shaders[i]->start();
    }
}

void Rasterizer::prepare() {
    //在每一频的开始，把渲染器上一频的信息清除
    numberOfVBOs = 0;    

    //把场景平均分配给着色器进行渲染
    shaders[0]->VBOsStart = 0;
    shaders[0]->VBOsEnd = 1;

    shaders[1]->VBOsStart = 1;
    shaders[1]->VBOsEnd = 2;

    shaders[2]->VBOsStart = 2;
    shaders[2]->VBOsEnd = 3;

    shaders[3]->VBOsStart = 3;
    shaders[3]->VBOsEnd = 4;

    shaders[4]->VBOsStart = 4;
    shaders[4]->VBOsEnd = 6;
}

//加载一个VBO
void Rasterizer::addVBO(VertexBufferObject* VBO) {
    VBOs[numberOfVBOs] = VBO;
    numberOfVBOs++;
    
}

//渲染器的入口
void Rasterizer::renderScene() {
    int size = sizeof(shaders)/sizeof(shaders[0]);

    //让着色器开始工作
    for (int i = 0; i < size; i++) {
         shaders[i]->notifyShader();
    }

    //等着色器线程完成工作
    for (int i = 0; i < size; i++) {
        std::unique_lock<std::mutex> lock(shaders[i]->mutex);
        shaders[i]->cv.wait(lock, [i] { return !shaders[i]->isWorking; });
    }

    //计算渲染的三角形总数
    for(int i = 0; i < size; i++)
        totalTriangleCount+=shaders[i]->triangleCount;
    
   
}
