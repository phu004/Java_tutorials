#include <iostream>
#include <fstream>
#include <Windows.h>

#include "Mesh.h"

Mesh::Mesh(const std::string& objFilePath, const std::string& order) {
    // 构造文件路径
    char buffer[MAX_PATH];
    DWORD length = GetCurrentDirectoryA(MAX_PATH, buffer);

    if (length == 0) {
        std::cout << "获取当前目录失败。" << std::endl;
    }

    std::string currentPath(buffer);
    std::string filePath = currentPath + "/" + objFilePath;

    // 读取文件以计算顶点、法线、索引和 UV 的数量
    numVertices = 0;
    numNormals = 0;
    numIndices = 0;
    numUVs = 0;
        
    std::ifstream file(filePath);
    std::string line;
        
    while (std::getline(file, line)) {
        if (line.rfind("v ", 0) == 0) {
            numVertices++;
        } else if (line.rfind("vn ", 0) == 0) {
            numNormals++;
        } else if (line.rfind("f ", 0) == 0) {
            numIndices += 3;
        } else if (line.rfind("vt ", 0) == 0) {
            numUVs++;
        }
    }

    
    // 创建顶点、法线、索引和 UV 的数组
    vertices = new Vector3D[numVertices];
    normals = new Vector3D[numVertices];
    indices = new int[numIndices];
    uvCoordinates = new float*[numIndices];

        
    // 创建临时数组以辅助调整法线和 UV 索引
    Vector3D* normalsTemp = new Vector3D[numNormals];
    float** uvCoordinatesTemp = new float*[numUVs];
        
    
    // 再次读取 OBJ 文件以填充数组
    file.clear();
    file.seekg(0);

        
    int vertexIndex = 0;
    int normalIndex = 0;
    int indexIndex = 0;
    int uvIndex = 0;
        
    while (std::getline(file, line)) {
        if (line.rfind("v ", 0) == 0) {
            float x, y, z;
            sscanf(line.c_str(), "v %f %f %f", &x, &y, &z);
            vertices[vertexIndex++].set(x, y, z);
        } else if (line.rfind("vn ", 0) == 0) {
            float x, y, z;
            sscanf(line.c_str(), "vn %f %f %f", &x, &y, &z);
            normalsTemp[normalIndex++].set(x, y, z);
        } else if (line.rfind("vt ", 0) == 0) {
            float u, v;
            sscanf(line.c_str(), "vt %f %f", &u, &v);
            uvCoordinatesTemp[uvIndex++] = new float[2]{ u, v };
        } else if (line.rfind("f ", 0) == 0) {
            int indicesValues[9];
            sscanf(line.c_str(), "f %d/%d/%d %d/%d/%d %d/%d/%d",
                   &indicesValues[0], &indicesValues[1],
                   &indicesValues[2], &indicesValues[3],
                   &indicesValues[4], &indicesValues[5],
                   &indicesValues[6], &indicesValues[7],
                   &indicesValues[8]);

           
          
            if (order != "clockwise") {
                for (int i = 2; i >= 0; i--) {
                    int vertexIndexValue = indicesValues[i * 3] - 1;
                    int uvIndexValue = indicesValues[i * 3 + 1] - 1;
                    int normalIndexValue = indicesValues[i * 3 + 2] - 1;
                    normals[vertexIndexValue].set(normalsTemp[normalIndexValue]);
                    uvCoordinates[indexIndex] = uvCoordinatesTemp[uvIndexValue];
                    indices[indexIndex++] = vertexIndexValue;
                }
            } else {
                for (int i = 0; i < 3; i++) {
                    int vertexIndexValue = indicesValues[i * 3] - 1;
                    int uvIndexValue = indicesValues[i * 3 + 1] - 1;
                    int normalIndexValue = indicesValues[i * 3 + 2] - 1;
                    normals[vertexIndexValue].set(normalsTemp[normalIndexValue]);
                    uvCoordinates[indexIndex] = new float[2];
                    uvCoordinates[indexIndex][0] = uvCoordinatesTemp[uvIndexValue][0];
                    uvCoordinates[indexIndex][1] = uvCoordinatesTemp[uvIndexValue][1];
                    indices[indexIndex++] = vertexIndexValue;
                    
                }
            }
        }
    }

 

    // 清理临时数组
    delete[] normalsTemp;
    for (int i = 0; i < numUVs; i++) {
        delete[] uvCoordinatesTemp[i];
    }
    delete[] uvCoordinatesTemp;
    
        
    // 关闭文件
    file.close();

    
}

Mesh::~Mesh() {
    delete[] vertices;
    delete[] normals;
    delete[] indices;
    for (int i = 0; i < sizeof(uvCoordinates); i++) {
        delete[] uvCoordinates[i];
    }
    delete[] uvCoordinates;
}
