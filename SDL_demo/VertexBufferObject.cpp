#include "VertexBufferObject.h"

VertexBufferObject::VertexBufferObject(Vector3D* vertexBuffer, int vertexCount, int triangleCount,  Vector3D** UVDirections, Vector3D* normals, int* indexBuffer, Light* lightSource, float kd, float ks)
    : vertexBuffer(vertexBuffer), vertexCount(vertexCount), triangleCount(triangleCount), UVDirections(UVDirections), normals(normals), indexBuffer(indexBuffer), lightSource(lightSource), kd(kd), ks(ks)
{

    prepareResource();
}

VertexBufferObject::VertexBufferObject(Mesh* mesh, Light* lightSource, float kd, float ks)
    : vertexBuffer(mesh->vertices), normals(mesh->normals), indexBuffer(mesh->indices), uvCoordinates(mesh->uvCoordinates),
      lightSource(lightSource), kd(kd), ks(ks)
{
    vertexCount = mesh->numVertices; 
    triangleCount = mesh->numIndices/3;

    prepareResource();
}

VertexBufferObject::~VertexBufferObject()
{
    // Destructor implementation
}

void VertexBufferObject::prepareResource()
{
    // 初始化用以存储变换后的顶点的容器
    updatedVertexBuffer = new Vector3D[vertexCount];
    for (int i = 0; i < vertexCount; i++) {
        updatedVertexBuffer[i] = Vector3D(0, 0, 0);
    }

    // 初始化用以存储变换后顶点亮度的容器
    vertexLightLevelBuffer = new float[vertexCount];
    
}

// Implement other member functions as needed
