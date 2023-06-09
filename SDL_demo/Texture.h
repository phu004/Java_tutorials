#ifndef TEXTURE_H
#define TEXTURE_H

#include <string>

class Texture {
public:
    int* texture;
    int height, width, heightMask, widthMask, widthBits, heightBits;

    Texture(const std::string& file, int widthBits, int heightBits);
    ~Texture();

};

#endif /* TEXTURE_H */
