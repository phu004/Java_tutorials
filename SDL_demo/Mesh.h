#ifndef MESH_H
#define MESH_H

#include <string>
#include "Vector3D.h"

class Mesh {
public:
    Vector3D* vertices;
    Vector3D* normals;
    int* indices;
    float** uvCoordinates;
    int numVertices;
    int numNormals;
    int numIndices;
    int numUVs;
    
    Mesh(const std::string& objFilePath, const std::string& order);
    ~Mesh();
};

#endif // MESH_H
