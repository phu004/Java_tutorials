#ifndef VECTOR3D_H
#define VECTOR3D_H

#include <cmath>
#include <iostream>

class Vector3D {
public:
    // Vector components
    float x, y, z;

    // Constructors
    Vector3D() : x(0.0f), y(0.0f), z(0.0f) {}

    Vector3D(float x, float y, float z);

    Vector3D(const Vector3D& v);

    // Setters
    Vector3D& set(const Vector3D& v);

    Vector3D& set(float x, float y, float z);

    // Addition
    Vector3D& add(const Vector3D& v);

    Vector3D& add(float x, float y, float z);

    Vector3D& add(const Vector3D& v, float scaler);

    // Subtraction
    Vector3D& subtract(const Vector3D& v);

    Vector3D& subtract(float x, float y, float z);

    Vector3D& subtract(const Vector3D& v, float scaler);

    // Dot product
    float dot(const Vector3D& v2) const;

    float dot(float x, float y, float z) const;

    // Cross product
    Vector3D& cross(const Vector3D& v1, const Vector3D& v2);

    Vector3D cross(const Vector3D& v) const;

    // Length
    float getLength() const;

    // Unit vector
    Vector3D& unit();

    // Scaling
    Vector3D& scale(float scalar);

    // Y-axis rotation
    Vector3D& rotateY(float sin, float cos);

    // X-axis rotation 
    Vector3D& rotateX(float sin, float cos);

    // Z-axis rotation
    Vector3D& rotateZ(float sin, float cos);

    // String representation
    std::string toString() const;
};

#endif
