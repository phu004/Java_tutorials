#include "shader.h"
#include <iostream>

#include "Rasterizer.h"
#include <cmath>
#include "Camera.h"


Shader::Shader(unsigned int* screen, float* zBuffer) : isWorking(false) {
    this->screen = screen;
    this->zBuffer = zBuffer;
}

void Shader::run() {
    while (true) {
        {
            std::unique_lock<std::mutex> lock(mutex);
            isWorking = false;
            cv.notify_one();
        }

        std::unique_lock<std::mutex> lock(mutex);
        cv.wait(lock, [this] { return isWorking; });

        //锁被打开后, 下面运行渲染任务

        //把三角形数归零
		triangleCount = 0;
        
        //对渲染器分配给自己的顶点缓冲对象进行渲染
		for(int j = VBOsStart; j < VBOsEnd; j++) {
            VBO = Rasterizer::VBOs[j];
            triangleColor = VBO->triangleColor;


            //从预先算好的三角函数表中获取变换角度所需的函数值
            local_sinX = sin(VBO->localRotationX * M_PI / 180.0);
            local_cosX = cos(VBO->localRotationX * M_PI / 180.0);
            local_sinY = sin(VBO->localRotationY * M_PI / 180.0);
            local_cosY = cos(VBO->localRotationY * M_PI / 180.0);
            local_sinZ = sin(VBO->localRotationZ * M_PI / 180.0);
            local_cosZ = cos(VBO->localRotationZ * M_PI / 180.0);

            global_sinY = sin((360-Camera::Y_angle) * M_PI / 180.0);
            global_cosY = cos((360-Camera::Y_angle) * M_PI / 180.0);
            global_sinX = sin((360-Camera::X_angle) * M_PI / 180.0);
            global_cosX = cos((360-Camera::X_angle) * M_PI / 180.0);

           
            //变换三角形的顶点
	  	  	transformVertices();

           

            //对顶点缓冲对象中的三角形进行渲染
		  	for(int i = 0; i < VBO->triangleCount; i++) {
                int firstVertex = VBO->indexBuffer[i*3];
		  		int secondVertex = VBO->indexBuffer[i*3+1];
		  		int thirdVertex = VBO->indexBuffer[i*3+2];

                //用索引缓冲来构建三角形
                updatedVertices[0] = VBO->updatedVertexBuffer[firstVertex];
                updatedVertices[1] = VBO->updatedVertexBuffer[secondVertex];
                updatedVertices[2] = VBO->updatedVertexBuffer[thirdVertex];
                
                vertexLightLevel[0] = VBO->vertexLightLevelBuffer[firstVertex];
                vertexLightLevel[1] = VBO->vertexLightLevelBuffer[secondVertex];
                vertexLightLevel[2] = VBO->vertexLightLevelBuffer[thirdVertex]; 

                //测试三角形是否该被渲染出来
                bool isHidden = testHidden();
                if(isHidden)
                    continue;


                //将在三角形在z裁键平面的部分裁剪掉
                isHidden = clipZNearPlane();		
                if(isHidden)
                    continue;

                

                if(VBO->renderType == VertexBufferObject::textured) {
			  			
                    origin.set(VBO->updatedVertexBuffer[firstVertex]);
                    originU = VBO->uvCoordinates[i][0];
                    originV = VBO->uvCoordinates[i][1];
                
                    uDirection.set(VBO->UVDirections[i][0]).scale(VBO->textureScale[i][0] / VBO->scale);
                    vDirection.set(VBO->UVDirections[i][1]).scale(VBO->textureScale[i][1] / VBO->scale);
                    
                    //将纹理方向按其所在模型变换的方向进行转换  	
                    uDirection.rotateX(local_sinX, local_cosX);
                    uDirection.rotateY(local_sinY, local_cosY);
                    uDirection.rotateZ(local_sinZ, local_cosZ);
                    vDirection.rotateX(local_sinX, local_cosX);
                    vDirection.rotateY(local_sinY, local_cosY);
                    vDirection.rotateZ(local_sinZ, local_cosZ);
                    
                    //把纹理方向按视角变换的反方向用来转换
                    uDirection.rotateY(global_sinY, global_cosY);
                    uDirection.rotateX(global_sinX, global_cosX);
                    vDirection.rotateY(global_sinY, global_cosY);
                    vDirection.rotateX(global_sinX, global_cosX);
			  	}else if(VBO->renderType == VertexBufferObject::barycentric_textured) {
                  
                    u1 = VBO->uvCoordinates[i*3][0];
                    v1 = VBO->uvCoordinates[i*3][1];
                    u2 = VBO->uvCoordinates[i*3+1][0];
                    v2 = VBO->uvCoordinates[i*3+1][1];
                    u3 = VBO->uvCoordinates[i*3+2][0];
                    v3 = VBO->uvCoordinates[i*3+2][1];
			  	}
                

                triangleCount++;
			  		
                //给三角形的像素着色
                scanTriangle();	    
                
            }

        }

    }
}

//裁剪三角形在z裁剪平面后的部分
bool Shader::clipZNearPlane(){
    //一般情况下三角形顶点数为3，裁剪后有可能变为4。
  	verticesCount = 0;
  		 
  	needToBeClipped = false;

    for(int i = 0; i < 3; i++){
        //如果顶点在裁剪平面之前，则不做任何改动
        if(updatedVertices[i].z >= nearClipDistance){
            clippedVertices[verticesCount].set(updatedVertices[i]);
            clippedLight[verticesCount] = vertexLightLevel[i];
            verticesCount++;
        } 
        //如果顶点在裁剪平面之后，则需要对三角形进行裁剪
        else{
            needToBeClipped = true;
            
            //找到前一个顶点（即三角形边上与当前顶点相邻的顶点）
            int index = (i+3 - 1)%3;
            if(updatedVertices[index].z >= nearClipDistance){
                //如果前一个顶点在裁剪平面的前面，  就找出当前顶点和前一个顶点之间的线段在裁剪平面的交点
                approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index], vertexLightLevel[i], vertexLightLevel[index]);
                verticesCount++;
            }
            //找到后一个顶点（即三角形边上与当前顶点相邻的另一个顶点）
            index = (i+1)%3;
            if(updatedVertices[index].z >= nearClipDistance){
                //如果后一个顶点在裁剪平面的前面，  就找出当前顶点和后一个顶点之间的线段在裁剪平面的交点
                approximatePoint(verticesCount, updatedVertices[i], updatedVertices[index], vertexLightLevel[i], vertexLightLevel[index]);
                verticesCount++;
            }
        }
    }

    //如果三角形被裁剪平面裁剪则要重新计算一遍和顶点的有关的参数
    if(needToBeClipped) {
        leftMostPosition = SCREEN_W;
        rightMostPosition = -1;
        upperMostPosition = SCREEN_H;
        lowerMostPosition = -1;
        
        for(int i = 0; i < verticesCount; i++) {
            //获得顶点的深度值
            vertexDepth[i] = 1.0f/clippedVertices[i].z;
            
            //用投影公式计算顶点在屏幕上的2D坐标
            vertices2D[i][0] = HALF_SCREEN_W + clippedVertices[i].x*screenDistance*vertexDepth[i]; 
            vertices2D[i][1] = HALF_SCREEN_H - clippedVertices[i].y*screenDistance*vertexDepth[i];
            
            leftMostPosition = std::min(leftMostPosition, vertices2D[i][0]);
            rightMostPosition = std::max(rightMostPosition, vertices2D[i][0]);
            upperMostPosition = std::min(upperMostPosition, vertices2D[i][1]);
            lowerMostPosition = std::max(lowerMostPosition, vertices2D[i][1]);
            
        }
        
        //重新判断三角形是否在屏幕外	
        if(leftMostPosition == SCREEN_W ||  rightMostPosition == -1 || upperMostPosition == SCREEN_H || lowerMostPosition == -1) {
            return true;
        }
        
        //重新判断三角形是否和屏幕的左边和右边相切
        isClippingRightOrLeft = leftMostPosition < 0 || rightMostPosition >= SCREEN_W;
    }

    return false;
}

//找出两点之间的线段在裁剪平面的交点
void Shader::approximatePoint(int i, Vector3D& behindPoint, Vector3D& frontPoint, float behindLightLevel, float frontLightLevel){
    //交点在线段间位置的比例
    tempVector1.set(frontPoint.x - behindPoint.x, frontPoint.y - behindPoint.y, frontPoint.z - behindPoint.z);
    float ratio = (frontPoint.z- nearClipDistance)/tempVector1.z;
    
    //线段方向矢量乘以这个比例，就可以得到交点的位置
    tempVector1.scale(ratio);
    clippedVertices[i].set(frontPoint.x, frontPoint.y, frontPoint.z);
    clippedVertices[i].subtract(tempVector1);
    
    //计算交点的位置的亮度
    clippedLight[i] = frontLightLevel - (frontLightLevel - behindLightLevel)*ratio;
}

//测试隐藏面
bool Shader::testHidden() {
   

    //测试 1: 如果三角形的顶点全部在Z裁剪平面后面，则这个三角形可视为隐藏面
    bool allBehindClippingPlane = true;
    for(int i = 0; i < 3; i++) {
        if(updatedVertices[i].z >= nearClipDistance) {
            allBehindClippingPlane = false;
            break;
        }
    }
    if(allBehindClippingPlane)
        return true;

    

    //测试 2: 计算三角形表面法线向量并检查其是否朝向视角
    edge1.set(updatedVertices[1]);
    edge1.subtract(updatedVertices[0]);
    edge2.set(updatedVertices[2]);
    edge2.subtract(updatedVertices[0]);
    surfaceNormal.cross(edge1, edge2);
    float dotProduct  = surfaceNormal.dot(updatedVertices[0]);
    //如果不朝向视角， 则这个三角形可视为隐藏面
    if(dotProduct >= 0)
        return true;

    //测试 3: 判断三角形是否在屏幕外
	testBoundary();

    //如果这个三角形的最左边或最右或最上或最下都没有被重新赋值，那么这个三角形肯定在屏幕范围之外，所以不对其进行渲染。
    if(leftMostPosition == SCREEN_W ||  rightMostPosition == -1 || upperMostPosition == SCREEN_H || lowerMostPosition == -1) {
        return true;
    }

    //判断三角形是否和屏幕的左边和右边相切
	isClippingRightOrLeft = leftMostPosition < 0 || rightMostPosition >= SCREEN_W;

    return false;
}

void Shader::testBoundary(){
    leftMostPosition = SCREEN_W;
    rightMostPosition = -1;
    upperMostPosition = SCREEN_H;
    lowerMostPosition = -1;

    for(int j = 0; j < 3; j++) {
        //获得顶点的深度值
        vertexDepth[j] = 1.0f/updatedVertices[j].z;

        //用投影公式计算顶点在屏幕上的2D坐标
        vertices2D[j][0] = HALF_SCREEN_W + updatedVertices[j].x*screenDistance*vertexDepth[j];
        vertices2D[j][1] = HALF_SCREEN_H - updatedVertices[j].y*screenDistance*vertexDepth[j];

        if(updatedVertices[j].z >= nearClipDistance) {
            leftMostPosition = std::min(leftMostPosition, vertices2D[j][0]);
            rightMostPosition = std::max(rightMostPosition, vertices2D[j][0]);
            upperMostPosition = std::min(upperMostPosition, vertices2D[j][1]);
            lowerMostPosition = std::max(lowerMostPosition, vertices2D[j][1]);
        }else {
            float screenX = HALF_SCREEN_W + updatedVertices[j].x*screenDistance/nearClipDistance;
            float screenY = HALF_SCREEN_H - updatedVertices[j].y*screenDistance/nearClipDistance;
            leftMostPosition = std::min(leftMostPosition, screenX);
            rightMostPosition = std::max(rightMostPosition, screenX);
            upperMostPosition = std::min(upperMostPosition, screenY);
            lowerMostPosition = std::max(lowerMostPosition, screenY);
        }
    }

}

//变换三角形的顶点
void Shader::transformVertices(){


    for(int i = 0; i < VBO->vertexCount; i++) {
        //将顶点缓冲中的顶点恢复初始值
		VBO->updatedVertexBuffer[i].set(VBO->vertexBuffer[i]);

        //对顶点进行大小变换
		VBO->updatedVertexBuffer[i].scale(VBO->scale);

        //将顶点缓冲中的顶点按其所在对象本身坐标系进行转换  	
        VBO->updatedVertexBuffer[i].rotateX(local_sinX, local_cosX);
        VBO->updatedVertexBuffer[i].rotateY(local_sinY, local_cosY);
        VBO->updatedVertexBuffer[i].rotateZ(local_sinZ, local_cosZ);
        VBO->updatedVertexBuffer[i].add(VBO->localTranslation);

        

        //将顶点缓冲中的顶点法量恢复初始值
		vertexNormal.set(VBO->normals[i]);


        //将顶点缓冲中的顶点法量按其所在对象本身坐标系进行转换  	
        vertexNormal.rotateX(local_sinX, local_cosX);
        vertexNormal.rotateY(local_sinY, local_cosY);
        vertexNormal.rotateZ(local_sinZ, local_cosZ);

       

        //如果顶点的法线和视角方向相反，就不计算其亮度。
        tempVector1.set(vertexNormal);
        tempVector1.rotateY(global_sinY, global_cosY);
        tempVector1.rotateX(global_sinX, global_cosX);

        if(tempVector1.z >0.6) {
            VBO->vertexLightLevelBuffer[i] = 0;
        }else {
            //在顶点按视角的变换转换之前计算顶点亮度
            if(VBO->lightSource != nullptr) {
                calculateLight(i);
            }
        }

        //把顶点缓冲中的顶点按视角变换的反方向用来转换
        VBO->updatedVertexBuffer[i].subtract(Camera::position);
        VBO->updatedVertexBuffer[i].rotateY(global_sinY, global_cosY);
        VBO->updatedVertexBuffer[i].rotateX(global_sinX, global_cosX);
    }
}

void Shader::calculateLight(int i){
    //计算顶点受到光照亮度: 顶点亮度 = 环境的亮度 + 漫反射亮度 + 镜面反射亮度
    //由于环境的亮度要在给像素着色的步骤中完成，我们这里只计算 漫反射亮度 + 镜面反射亮度

    //漫反射亮度 = 漫反射系数 * （顶点法量 点积 顶点到光源方向）
    lightDirection.set(VBO->lightSource->position);
    lightDirection.subtract(VBO->updatedVertexBuffer[i]);
	lightDirection.unit();
    float ld = VBO->kd * (std::max(vertexNormal.dot(lightDirection), 0.0f));

    //光线的反射方向 = 2 * （顶点法量 点积 顶点到光源方向） * 顶点法量 - 顶点到光源方向
	reflectionDirection.set(vertexNormal).scale(2 * vertexNormal.dot(lightDirection)).subtract(lightDirection);

    //镜面反射亮度 = 镜面反射系数 * （光线的反射方向 点积 顶点到视角方向） ^ n
	viewDirection.set(Camera::position).subtract(VBO->updatedVertexBuffer[i]).unit();
    float ls = viewDirection.dot(reflectionDirection);
    ls = std::max(ls,0.0f);
    ls = ls*ls*ls*ls*ls*ls;
    ls = ls*ls*ls*ls*ls*ls;
    ls = VBO->ks * ls;
    VBO->vertexLightLevelBuffer[i] = ld + ls;



   
}

//将三角形转换为扫描线
void Shader::scanTriangle() {

    //初始化扫描线最高， 最低， 最左， 最右的位置
    scanUpperPosition = SCREEN_H;
    scanLowerPosition = -1;

    //扫描三角形的每一个边
    for(int i = 0; i < verticesCount; i++){
        scanSide(i);
    }

    //当三角形与屏幕的两边相切的时候，我们需要对扫描线数组的两个边缘进行修改，确保它们的值都在0和screen_w之间
    if(isClippingRightOrLeft) {
        handleClipping();
    }
    
    //根据三角形种类调用不同的渲染模式
    if(VBO->renderType == VertexBufferObject::soildColor)
        rendersolidTriangle_zDepth_shading();	
    else if(VBO->renderType == VertexBufferObject::textured)
        renderTriangle_zDepth_shading_textured();
    else if(VBO->renderType == VertexBufferObject::barycentric_textured)
        renderTriangle_zDepth_shading_barycentric_textured();
}

void Shader::scanSide(int i){
    float* vertex1 = vertices2D[i];    //获取第一个顶点
    float* vertex2;

    float depth1 = vertexDepth[i];  //获取第一个顶点的深度值
    float depth2;

    float light1 = clippedLight[i]; //获取第一个顶点的亮度值
    float light2;

    if(i == verticesCount -1 ){    //如果已经处理到最后一个顶点
        vertex2 = vertices2D[0];   //则第二个点为第一个顶点
        depth2 = vertexDepth[0];
        light2 = clippedLight[0];
    }else{
        vertex2 = vertices2D[i+1];   //否则第二个顶点为下一个顶点
        depth2 = vertexDepth[i+1];
        light2 = clippedLight[i+1];
    }

    bool downwards = true;   //默认是下降的边

    if (vertex1[1]> vertex2[1]) {    //如果第一个顶点低于第二个顶点
        downwards = false;           //则为上升的边
        float* temp = vertex1;      //互换两个点，这样可以保证扫描始终从上往下扫
        vertex1 = vertex2;
        vertex2 = temp;

        float tempDepth = depth1;   //调动两个点所对应的深度值
        depth1 = depth2;
        depth2 = tempDepth;

        float tempLight = light1;   //调动两个点所对应的亮度值
        light1 = light2;
        light2 = tempLight;
    }

    // 忽略水平边
    float dy = vertex2[1] - vertex1[1];
    if(dy ==  0) {
        return;
    }

    //得到这条边的最高和最低位
    int startY = std::max((int)(vertex1[1]) + 1, 0);
    int endY = std::min((int)(vertex2[1]), SCREEN_H-1);

    //更新扫描线整体的最高和最低的位置
    if(startY < scanUpperPosition )
        scanUpperPosition = startY;

    if(endY > scanLowerPosition)
        scanLowerPosition = endY;

    //计算边的x值变化梯度
    float gradient = (vertex2[0] - vertex1[0]) /dy;

    //用线性插值算出这条边高位的x初始值
    float startX = ((vertex1[0]) +  (startY - vertex1[1]) * gradient);

    //如果顶点的x值小于0则认该三角形与屏幕的左侧相切
    if(startX < 0 && !isClippingRightOrLeft) {
        startX = leftMostPosition;
    }

    //计算边的深度值变化的梯度
    float z_dy = (depth2-depth1)/dy;

    //计算边初始位置的深度值
    float tempZ= depth1 + (startY - vertex1[1])*z_dy;

    //计算边亮度值变化的梯度
    float light_dy = (light2 - light1)/dy;


    //计算边初始位置的亮度值
    float tempLight= light1 + (startY - vertex1[1])*light_dy;

    for (int y=startY; y<=endY; y++, startX+=gradient, tempZ+=z_dy, tempLight+=light_dy) {
        //把下降边的x值存到扫描线的右边，反之就放到扫描线的左面
        if(downwards){
            xRight[y] = (int)startX;
            zRight[y] = tempZ;
            lightRight[y] = tempLight;
        }else{
            xLeft[y] = (int)startX;
            zLeft[y] = tempZ;
            lightLeft[y] = tempLight;
        }
    }

}

void Shader::handleClipping(){
    int x_left, x_right;
    bool xLeftInView, xRightInView;
    for(int y = scanUpperPosition; y <= scanLowerPosition; y++){   //对扫描线进行逐行处理
        x_left = xLeft[y];
        x_right = xRight[y];
        xLeftInView = x_left >=0 && x_left < SCREEN_W;       //左缘是否在屏幕内
        xRightInView = x_right >0 && x_right < SCREEN_W;     //右缘是否在屏幕内

        if(xLeftInView && xRightInView && x_right >= x_left)        //如果都在屏幕内就不做任何处理
            continue;

        if(x_left >= SCREEN_W  || x_right <= 0 || x_right < x_left ){   //如果扫描线在屏幕外边，那么把这条扫描线的长度设为0以免被渲染出来
            xLeft[y] = 0;
            xRight[y] = 0;
            continue;
        }
        float dx =  x_right - x_left;          //算出扫描线的长度
        float dz = zRight[y] - zLeft[y];       //算出扫描线左缘和右缘之间的深度差
        float dlight = lightRight[y] - lightLeft[y];       //算出扫描线左缘和右缘之间的亮度差

        if(!xLeftInView){           //如果左缘比屏幕的左边还要左
            xLeft[y] = 0;           //就把左缘设在屏幕的最左端
            zLeft[y] = (zLeft[y] + dz /dx * (-x_left) ) ;  //用线性插值算出左缘的深度值
            lightLeft[y] = (lightLeft[y] + dlight /dx * (-x_left) ) ;  //用线性插值算出左缘的亮度值
        }
        if(!xRightInView){          //如果右缘比屏幕的右边还要右
            xRight[y] = SCREEN_W;   //就把右缘设在屏幕的最右端
            zRight[y] = (zRight[y] - dz /dx * (x_right - SCREEN_W));   //用线性插值算出右缘的深度值
            lightRight[y] = (lightRight[y] - dlight /dx * (x_right - SCREEN_W));   //用线性插值算出右缘的亮度值
        }
	}
}

//画单色的三角形 （填充深度值，使用亮度）
void Shader::rendersolidTriangle_zDepth_shading(){
    //逐行渲染扫描线
    for(int i = scanUpperPosition; i <= scanLowerPosition; i++){
        int x_left = xLeft[i] ;
        int x_right = xRight[i];
                
        float z_Left = zLeft[i];
        float z_Right = zRight[i];
        
        float lightLevel = lightLeft[i];
        float light_right = lightRight[i];

        float dz = (z_Right- z_Left)/(x_right - x_left);  //算出这条扫描线上深度值改变的梯度

        float dlight = (light_right- lightLevel)/(x_right - x_left);  //算出这条扫描线上亮度值改变的梯度
        
        x_left+=i * SCREEN_W;
        x_right+=i * SCREEN_W;

        for(int j = x_left;  j < x_right; j++, z_Left+=dz, lightLevel+=dlight){
            if(zBuffer[j] < z_Left) {         //如果深度浅于深度缓冲上的值
                zBuffer[j] = z_Left;          //就更新深度缓冲上的值
                int r = (triangleColor >> 16)&0xFF;
                int g = (triangleColor >> 8) & 0xFF;
                int b = triangleColor & 0xFF;
            
                //像素最终的颜色 = 像素在环境光下的颜色 + 光源的颜色 * 该像素收到的光照亮度 
                r= (int)(r*VBO->lightSource->la + 255* lightLevel);
                g= (int)(g*VBO->lightSource->la + 255* lightLevel);
                b= (int)(b*VBO->lightSource->la + 255* lightLevel);
                
                
                r = std::min(r, 255);
                g = std::min(g, 255);
                b = std::min(b, 255);
                
                screen[j] = (r << 16) | (g << 8) | b;  //并给屏幕上的这个像素上色
                
                
            }
        }
  	}
}

//画有纹理的三角形 （填充深度值，使用亮度）
void Shader::renderTriangle_zDepth_shading_textured() {
    int* texture = Rasterizer::textures[VBO->textureIndex]->texture;
    int width = Rasterizer::textures[VBO->textureIndex]->width;
    int height = Rasterizer::textures[VBO->textureIndex]->height;
    int widthMask = Rasterizer::textures[VBO->textureIndex]->widthMask;
    int heightMask = Rasterizer::textures[VBO->textureIndex]->heightMask;
    int widthBits = Rasterizer::textures[VBO->textureIndex]->widthBits;
    
    
    float WorldX = 0;
    float WorldY = 0;
    float WorldZ = 0;
    
    float X1 = 0;
    float X2 = 0;
    float Y1 = 0;
    float Y2 = 0;
    
    //逐行渲染扫描线
    for(int i = scanUpperPosition; i <= scanLowerPosition; i++){

        int x_left = xLeft[i] ;
        int x_right = xRight[i];
                
        float z_Left = zLeft[i];
        float z_Right = zRight[i];
        
        float lightLevel = lightLeft[i];
        float light_right = lightRight[i];

        float dz = (z_Right- z_Left)/(x_right - x_left);  //算出这条扫描线上深度值改变的梯度

        float dlight = (light_right- lightLevel)/(x_right - x_left);  //算出这条扫描线上亮度值改变的梯度
        
        x_left+=i * SCREEN_W;
        x_right+=i * SCREEN_W;
        
        float WorldYTemp = (float)(HALF_SCREEN_H - i) / screenDistance;
        
 

        
        for(int j = x_left; j < x_right; j+=16) {
            //每隔16个像素
            //用三维投影公式推算出像素再三维空间的位置
            //用纹理的UV反向分别与这个P点与场景原点的差值经行点积运算, 得出的值再分别乘以纹理的长和宽
            
            int delta = std::min(16, x_right - j);
            
            if(j == x_left) {
                WorldZ = 1.0f/z_Left;
                WorldX = ((float)(j%SCREEN_W) - HALF_SCREEN_W) / screenDistance  * WorldZ;
                WorldY =  WorldYTemp * WorldZ;

                point.set(WorldX, WorldY, WorldZ);
                point.subtract(origin);
                
                X1 = (uDirection.dot(point)+originU)*width;
                Y1 = (vDirection.dot(point)+originV)*height;

                


            }else {
                X1 = X2;
                Y1 = Y2;
            }

          
            WorldZ = 1.0f/(z_Left+dz*delta);
            WorldX = ((float)(j%SCREEN_W+delta) - HALF_SCREEN_W) / screenDistance  * WorldZ;
            WorldY = WorldYTemp * WorldZ;

            point.set(WorldX, WorldY, WorldZ);
            point.subtract(origin);
            
            X2 = (uDirection.dot(point)+originU)*width;
            Y2 = (vDirection.dot(point)+originV)*height;
            
            float dX = (X2 - X1)/delta;
            float dY = (Y2 - Y1)/delta;
            
            for(int k = 0; k < delta; k++, z_Left+=dz, lightLevel+=dlight, X1+=dX, Y1+=dY) {
                
                if(zBuffer[j+k] < z_Left) {         //如果深度浅于深度缓冲上的值
                    zBuffer[j+k] = z_Left;          //就更新深度缓冲上的值
                
                    int textureIndex = ((int)X1&widthMask) + (((int)Y1&heightMask)<<widthBits);
                    
                    int color = texture[textureIndex];
                    
                    int r = (color >> 16)&0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                
                    //像素最终的颜色 = 像素在环境光下的颜色 + 光源的颜色 * 该像素收到的光照亮度 
                    
                    r= (int)(r*VBO->lightSource->la + 255* lightLevel);
                    g= (int)(g*VBO->lightSource->la + 255* lightLevel);
                    b= (int)(b*VBO->lightSource->la + 255* lightLevel);
                    
                    r = std::min(r, 255);
                    g = std::min(g, 255);
                    b = std::min(b, 255);
                    
                    screen[j+k] = (r << 16) | (g << 8) | b;  //并给屏幕上的这个像素上色
                }
            }
        }
    }
}

//画有纹理的三角形 （填充深度值，使用亮度, 使用重心坐标）
void Shader::renderTriangle_zDepth_shading_barycentric_textured() {
    int* texture = Rasterizer::textures[VBO->textureIndex]->texture;
    int width = Rasterizer::textures[VBO->textureIndex]->width;
    int height = Rasterizer::textures[VBO->textureIndex]->height;
    int widthMask = Rasterizer::textures[VBO->textureIndex]->widthMask;
    int heightMask = Rasterizer::textures[VBO->textureIndex]->heightMask;
    int widthBits = Rasterizer::textures[VBO->textureIndex]->widthBits;

    float WorldX = 0;
    float WorldY = 0;
    float WorldZ = 0;
    
    float X1 = 0;
    float X2 = 0;
    float Y1 = 0;
    float Y2 = 0;

    pointA.set(updatedVertices[0]);
  	pointB.set(updatedVertices[1]);
  	pointC.set(updatedVertices[2]);

    V0.set(pointB).subtract(pointA);
  	V1.set(pointC).subtract(pointA);

    float d00 = V0.dot(V0);
    float d01 = V0.dot(V1); 
    float d11 = V1.dot(V1);
    float denom = d00 * d11 - d01 * d01;

    float d20 = 0; 
    float d21 = 0; 

    float v = 0;
    float w = 0;
    float u = 0;

    float Pu = 0;
    float Pv = 0;

    //逐行渲染扫描线
    for(int i = scanUpperPosition; i <= scanLowerPosition; i++){

        int x_left = xLeft[i] ;
        int x_right = xRight[i];
                
        float z_Left = zLeft[i];
        float z_Right = zRight[i];
        
        float lightLevel = lightLeft[i];
        float light_right = lightRight[i];

        float dz = (z_Right- z_Left)/(x_right - x_left);  //算出这条扫描线上深度值改变的梯度

        float dlight = (light_right- lightLevel)/(x_right - x_left);  //算出这条扫描线上亮度值改变的梯度
        
        x_left+=i * SCREEN_W;
        x_right+=i * SCREEN_W;
        
        float WorldYTemp = (float)(HALF_SCREEN_H - i) / screenDistance;
        
        for(int j = x_left; j < x_right; j+=16) {
            //每隔16个像素
            //用三维投影公式推算出像素在三维空间的位置
            //然后用他的位置算出其在三角形中的重心坐标
            //最后利用重心坐标算出纹理坐标
            
            int delta = std::min(16, x_right - j);
            
            if(j == x_left) {
                WorldZ = 1.0f/z_Left;
                WorldX = ((float)(j%SCREEN_W) - HALF_SCREEN_W) / screenDistance  * WorldZ;
                WorldY =  WorldYTemp * WorldZ;

                V2.set(WorldX, WorldY, WorldZ).subtract(pointA);
  				d20 = V2.dot(V0); 
  				d21 = V2.dot(V1); 

                v = (d11 * d20 - d01 * d21) / denom;
  				w = (d00 * d21 - d01 * d20) / denom;
  				u = 1.0f - v - w;

  				Pu = u*u1 + v*u2 + w*u3;
  				Pv = u*v1 + v*v2 + w*v3;

  				X1 = Pu*width;
  				Y1 = Pv*height;
            }else {
                X1 = X2;
                Y1 = Y2;
            }

          
            WorldZ = 1.0f/(z_Left+dz*delta);
            WorldX = ((float)(j%SCREEN_W+delta) - HALF_SCREEN_W) / screenDistance  * WorldZ;
            WorldY = WorldYTemp * WorldZ;

            V2.set(WorldX, WorldY, WorldZ).subtract(pointA);
            d20 = V2.dot(V0); 
            d21 = V2.dot(V1); 

            v = (d11 * d20 - d01 * d21) / denom;
            w = (d00 * d21 - d01 * d20) / denom;
            u = 1.0f - v - w;

            Pu = u*u1 + v*u2 + w*u3;
            Pv = u*v1 + v*v2 + w*v3;

            X2 = Pu*width;
            Y2 = Pv*height;
            
            float dX = (X2 - X1)/delta;
            float dY = (Y2 - Y1)/delta;
            
            for(int k = 0; k < delta; k++, z_Left+=dz, lightLevel+=dlight, X1+=dX, Y1+=dY) {
                
                if(zBuffer[j+k] < z_Left) {         //如果深度浅于深度缓冲上的值
                    zBuffer[j+k] = z_Left;          //就更新深度缓冲上的值
                
                    int textureIndex = ((int)X1&widthMask) + (((int)Y1&heightMask)<<widthBits);
                    
                    int color = texture[textureIndex];
                    
                    int r = (color >> 16)&0xFF;
                    int g = (color >> 8) & 0xFF;
                    int b = color & 0xFF;
                
                    //像素最终的颜色 = 像素在环境光下的颜色 + 光源的颜色 * 该像素收到的光照亮度 
                    
                    r= (int)(r*VBO->lightSource->la + 255* lightLevel);
                    g= (int)(g*VBO->lightSource->la + 255* lightLevel);
                    b= (int)(b*VBO->lightSource->la + 255* lightLevel);
                    
                    r = std::min(r, 255);
                    g = std::min(g, 255);
                    b = std::min(b, 255);
                    
                    screen[j+k] = (r << 16) | (g << 8) | b;  //并给屏幕上的这个像素上色
                }
            }
        }
    }
}

void Shader::start() {
    thread = std::thread(&Shader::run, this);
}

void Shader::stop() {
    thread.join();
}

void Shader::notifyShader() {
    std::unique_lock<std::mutex> lock(mutex);
    isWorking = true;
    cv.notify_one();
}
