#include "Vector3D.h"


Vector3D::Vector3D(float x, float y, float z) : x(x), y(y), z(z) {}

Vector3D::Vector3D(const Vector3D& v) : x(v.x), y(v.y), z(v.z) {}

Vector3D& Vector3D::set(const Vector3D& v) {
    x = v.x;
    y = v.y;
    z = v.z;
    return *this;
}

Vector3D& Vector3D::set(float x, float y, float z) {
    this->x = x;
    this->y = y;
    this->z = z;
    return *this;
}

Vector3D& Vector3D::add(const Vector3D& v) {
    x += v.x;
    y += v.y;
    z += v.z;
    return *this;
}

Vector3D& Vector3D::add(float x, float y, float z) {
    this->x += x;
    this->y += y;
    this->z += z;
    return *this;
}

Vector3D& Vector3D::add(const Vector3D& v, float scaler) {
    x += v.x * scaler;
    y += v.y * scaler;
    z += v.z * scaler;
    return *this;
}

Vector3D& Vector3D::subtract(const Vector3D& v) {
    x -= v.x;
    y -= v.y;
    z -= v.z;
    return *this;
}

Vector3D& Vector3D::subtract(float x, float y, float z) {
    this->x -= x;
    this->y -= y;
    this->z -= z;
    return *this;
}

Vector3D& Vector3D::subtract(const Vector3D& v, float scaler) {
    x -= v.x * scaler;
    y -= v.y * scaler;
    z -= v.z * scaler;
    return *this;
}

float Vector3D::dot(const Vector3D& v2) const {
    return x * v2.x + y * v2.y + z * v2.z;
}

float Vector3D::dot(float x, float y, float z) const {
    return this->x * x + this->y * y + this->z * z;
}

Vector3D& Vector3D::cross(const Vector3D& v1, const Vector3D& v2) {
    x = v1.y * v2.z - v1.z * v2.y;
    y = v1.z * v2.x - v1.x * v2.z;
    z = v1.x * v2.y - v1.y * v2.x;
    return *this;
}

Vector3D Vector3D::cross(const Vector3D& v) const {
    return Vector3D(y * v.z - z * v.y, z * v.x - x * v.z, x * v.y - y * v.x);
}

float Vector3D::getLength() const {
    return std::sqrt(x * x + y * y + z * z);
}

Vector3D& Vector3D::unit() {
    float length = getLength();
    x /= length;
    y /= length;
    z /= length;
    return *this;
}

Vector3D& Vector3D::scale(float scalar) {
    x *= scalar;
    y *= scalar;
    z *= scalar;
    return *this;
}

Vector3D& Vector3D::rotateY(float sin, float cos) {
    float oldX = x;
    float oldZ = z;
    x = cos * oldX + sin * oldZ;
    z = -sin * oldX + cos * oldZ;
    return *this;
}

Vector3D& Vector3D::rotateX(float sin, float cos) {
    float oldY = y;
    float oldZ = z;
    y = cos * oldY + sin * oldZ;
    z = -sin * oldY + cos * oldZ;
    return *this;
}

Vector3D& Vector3D::rotateZ(float sin, float cos) {
    float oldX = x;
    float oldY = y;
    x = cos * oldX + sin * oldY;
    y = -sin * oldX + cos * oldY;
    return *this;
}

std::string Vector3D::toString() const {
    return "(" + std::to_string(x) + ", " + std::to_string(y) + ", " + std::to_string(z) + ")";
}
