#include <stdio.h>
#include <SDL2/SDL.h>
#include <SDL2/SDL_keyboard.h>
#include <windows.h>
#include <cmath>
#include <iostream>

#include "Vector3D.h"
#include "Mesh.h"
#include "Camera.h"
#include "Light.h"
#include "Texture.h"
#include "Rasterizer.h"
#include "globals.h"
#include "constants.h"

//屏幕的分辨率
int screen_w = SCREEN_W;
int screen_h = SCREEN_H;
int screenSize = SCREEN_SIZE;

//使用一个float数组来存储屏幕的深度缓冲值
float*  zBuffer;

//记载目前已渲染的 帧数
int frameIndex;

//希望达到的每频之间的间隔时间 (毫秒)
int frameInterval = 32;

//cpu睡眠时间，数字越小说明运算效率越高
int sleepTime, averageSleepTime;

//刷新率，及计算刷新率所用到一些辅助参数
int framePerSecond;
Uint32 lastDraw;
double thisTime, lastTime;

//总共渲染的三角形数
int totalTriangleCount;

//渲染用到的贴图
Texture** textures;

void handleKeyPress(SDL_Keycode key);
void handleKeyRelease(SDL_Keycode key);

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {


    //弹出一个宽 为screen_w高为screen_h的窗口，并把它放置屏幕中间。
    HWND hWnd = GetConsoleWindow();
    ShowWindow(hWnd, SW_HIDE);

    SDL_Init(SDL_INIT_VIDEO);

    SDL_Window * window = SDL_CreateWindow("",
        SDL_WINDOWPOS_CENTERED, SDL_WINDOWPOS_CENTERED,
        1024, 682,
        SDL_WINDOW_RESIZABLE);
    SDL_SetWindowResizable(window, SDL_FALSE);

    SDL_Surface * window_surface = SDL_GetWindowSurface(window);

    unsigned int* screen = static_cast<unsigned int*>(window_surface->pixels);

    zBuffer = new float[screenSize];

    

    //初始化视角
	Camera::init(0,1,0);

    //读取纹理贴图
    textures = new Texture*[5];
    textures[0] = new Texture("background.jpg", 9, 9);
    textures[1] = new Texture("ground.jpg", 9, 9);
    textures[2] = new Texture("bunny.jpg", 11, 11);
    textures[3] = new Texture("capsule0.jpg", 11, 10);
    textures[4] = new Texture("bunny2.jpg", 11, 11);

    //初始化光栅渲染器
	Rasterizer::init(screen, zBuffer, textures);
   
    //定义光源
    Light lightSource1(0, 300.0f, -100.0f, 0.6f);
	Light lightSource2(0, 300.0f, -100.0f, 1.0f);

    float kd = 0.2f; float ks=0.6f;

    //由obj文件构建兔子模型
	Mesh* bunnyMesh = new Mesh("objs/bunny.obj", "clockwise");
    VertexBufferObject* bunnies[4];
    for (int i = 0; i < 4; i++) {
        VertexBufferObject* bunny = new VertexBufferObject(bunnyMesh, &lightSource1, kd, ks);
        bunny->scale = 7.0f;
        bunny->renderType = VertexBufferObject::barycentric_textured;
        if(i == 0)
            bunny->textureIndex =3;
        else if(i == 2)
            bunny->textureIndex =4;
        else
            bunny->textureIndex =2;
        bunnies[i] = bunny;
    }

    //构建背景贴图的模型
    Vector3D* backgroundVertices = new Vector3D[4]{Vector3D(-4, 5.9f, 12), Vector3D(-4, -2.1f, 12), Vector3D(4, -2.1f, 12), Vector3D(4, 5.9f, 12)};
    Vector3D* backgroundNormals = new Vector3D[4]{Vector3D(0, 0, -1), Vector3D(0, 0, -1), Vector3D(0, 0, -1), Vector3D(0, 0, -1)};
    Vector3D** backgroundUVDirections = new Vector3D*[2];
    backgroundUVDirections[0] = new Vector3D[2]{Vector3D(1.0f, 0, 0), Vector3D(0, -1.0f, 0)};
    backgroundUVDirections[1] = new Vector3D[2]{Vector3D(1.0f, 0, 0), Vector3D(0, -1.0f, 0)};
    int* backgroundIndices = new int[6]{0, 2, 1, 0, 3, 2};
    VertexBufferObject background(backgroundVertices, 4, 2, backgroundUVDirections, backgroundNormals, backgroundIndices, &lightSource2, 0, 0);
    background.textureIndex = 0;
    background.renderType = VertexBufferObject::textured;
    background.textureScale = new float*[2]{new float[2]{0.125f, 0.125f}, new float[2]{0.125f, 0.125f}};
    background.uvCoordinates = new float*[2]{new float[2]{0.0f, 0.0f}, new float[2]{0.0f, 0.0f}};

    //构建地面贴图的模型
    Vector3D* groundVertices = new Vector3D[4] {Vector3D(-20,-2.1f,30), Vector3D(-20,-2.1f,-10), Vector3D(20,-2.1f,-10), Vector3D(20,-2.1f,30)};
    Vector3D* groundNormals = new Vector3D[4] {Vector3D(0,1,0), Vector3D(0,1,0),Vector3D(0,1,0),Vector3D(0,1,0)};
    Vector3D** groundUVDirections = new Vector3D*[2];
    groundUVDirections[0] = new Vector3D[2]{Vector3D(1,0,0), Vector3D(0, 0,-1)};
	groundUVDirections[1] = new Vector3D[2]{Vector3D(1,0,0), Vector3D(0, 0,-1)};
    int* groundIndices = new int[6] {0,2,1,0,3,2};
    VertexBufferObject ground(groundVertices, 4, 2, groundUVDirections, groundNormals, groundIndices, &lightSource2, 0, 0);
    ground.textureIndex = 1;
	ground.renderType = VertexBufferObject::textured;
    ground.textureScale = new float*[2] {new float[2] {0.25f, 0.25f},  new float[2] {0.25f, 0.25f}};
    ground.uvCoordinates = new float*[2]{new float[2]{0.0f, 0.0f}, new float[2]{0.0f, 0.0f}};


    //主循环
    while (true)
    {
        SDL_Event event;
        while (SDL_PollEvent(&event))
        {
            if (event.type == SDL_QUIT) exit(0);

            if(event.type == SDL_KEYDOWN) handleKeyPress(event.key.keysym.sym);

            if(event.type == SDL_KEYUP) handleKeyRelease(event.key.keysym.sym);
        }

        //三角形数归零
        totalTriangleCount = 0;

        //更新视角
		Camera::update();

        //清零深度缓冲
        std::fill_n(zBuffer, screenSize, 0);

        //把屏幕渲染成天蓝色
        std::fill_n(screen, screenSize, (163 << 16) | (216 << 8) | 239);

        Rasterizer::prepare();

        //渲染兔子模型
		
        bunnies[0]->localRotationY = (frameIndex)%360;
        bunnies[0]->localTranslation.set(1.2f, -2.1f, 7.7f);
        bunnies[0]->triangleColor = 0xffffff;
    
        bunnies[1]->localTranslation.set(0.3f, -2.1f, 8.9f);
        bunnies[1]->localRotationY = 40;
        bunnies[1]->triangleColor = 0x006B6B;
        
        bunnies[2]->localRotationY = 360 -(frameIndex*3%360);
        bunnies[2]->localTranslation.set(-1.1f, -2.1f, 8.4f);
        bunnies[2]->triangleColor = 0xE56B3C;
        
        bunnies[3]->localRotationY = (frameIndex*2+ 145)%360;
        bunnies[3]->localTranslation.set(0.0f, -2.1f, 7.7f);
        bunnies[3]->triangleColor = 0xB361B9;

        int size = sizeof(bunnies)/sizeof(bunnies[0]);
        for(int i = 0; i < size; i++) {
            Rasterizer::addVBO(bunnies[i]);
        }

        //渲染背景
		Rasterizer::addVBO(&background);

        //渲染地面
		Rasterizer::addVBO(&ground);

        Rasterizer::renderScene();

        SDL_UpdateWindowSurface(window);

         //loop每运行一边，帧数就+1
		frameIndex++;

        int mySleepTime = 0;
        int processTime = (int)(SDL_GetTicks()-lastDraw);
        if(processTime < frameInterval && lastDraw!= 0) {
            mySleepTime = frameInterval-processTime;
            Sleep(0);
        }
        sleepTime+=mySleepTime;
        lastDraw=SDL_GetTicks();
        //计算当前的刷新率 
        if(frameIndex%30==0){
            thisTime = SDL_GetTicks();
            framePerSecond = (int)(1000/((thisTime - lastTime)/30));
            lastTime = thisTime;
            averageSleepTime=sleepTime/30;
            sleepTime = 0;
        }

        //显示当前刷新率
        int cpuUsage = (frameInterval-averageSleepTime)*100/frameInterval;
        std::string title = std::string("c++软光栅渲染      频数: ") + std::to_string(framePerSecond) + "      "  
                                     +  "三角形总数: " + std::to_string(totalTriangleCount);
        SDL_SetWindowTitle(window, title.c_str());
    }
}

void handleKeyPress(SDL_Keycode key) {
    switch (key) {
        case 119:
            Camera::MOVE_FORWARD = true;
            break;
        case 115:
            Camera::MOVE_BACKWARD = true;
            break;
        case 97:
            Camera::SLIDE_LEFT = true;
            break;
        case 100:
            Camera::SLIDE_RIGHT = true;
            break;
        case 1073741906:
            Camera::LOOK_UP = true;
            break;
        case 1073741905:
            Camera::LOOK_DOWN = true;
            break;
        case 1073741904:
            Camera::LOOK_LEFT = true;
            break;
        case 1073741903:
            Camera::LOOK_RIGHT = true;
            break;
        default:
            break;
    }
}

void handleKeyRelease(SDL_Keycode key) {
    switch (key) {
        case 119:
            Camera::MOVE_FORWARD = false;
            break;
        case 115:
            Camera::MOVE_BACKWARD = false;
            break;
        case 97:
            Camera::SLIDE_LEFT = false;
            break;
        case 100:
            Camera::SLIDE_RIGHT = false;
            break;
        case 1073741906:
            Camera::LOOK_UP = false;
            break;
        case 1073741905:
            Camera::LOOK_DOWN = false;
            break;
        case 1073741904:
            Camera::LOOK_LEFT = false;
            break;
        case 1073741903:
            Camera::LOOK_RIGHT = false;
            break;
        default:
            break;
    }
}