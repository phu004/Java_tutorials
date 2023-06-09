#include "Texture.h"
#include <iostream>
#include <cmath>
#include <stdexcept>
#include <fstream>

#define STB_IMAGE_IMPLEMENTATION
#include "stb_image.h"

Texture::Texture(const std::string& file, int widthBits, int heightBits) {
    std::string imageFolder = "image/";
    std::string filePath = imageFolder + file;

    int channels;
    unsigned char* image = stbi_load(filePath.c_str(), &width, &height, &channels, 0);
    if (!image) {
        throw std::runtime_error("Failed to load image: " + filePath);
    }

    this->widthBits = widthBits;
    this->heightBits = heightBits;

    height = static_cast<int>(std::pow(2, heightBits));
    width = static_cast<int>(std::pow(2, widthBits));

    heightMask = height - 1;
    widthMask = width - 1;

    texture = new int[width * height];

    // Convert image to texture pixels
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            int index = y * width + x;
            int imageIndex = (y * width + x) * channels;
            texture[index] = (image[imageIndex] << 16) | (image[imageIndex + 1] << 8) | image[imageIndex + 2];
        }
    }

    stbi_image_free(image);
}

Texture::~Texture() {
    delete[] texture;
    texture = nullptr;
}
