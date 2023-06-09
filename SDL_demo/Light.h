#ifndef LIGHT_H
#define LIGHT_H

#include "Vector3D.h"

class Light {
public:
    Vector3D position;
    float la;

    Light(float x, float y, float z, float la);
};

#endif  // LIGHT_H
