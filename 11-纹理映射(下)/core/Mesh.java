package core;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Mesh {
	
	public Vector3D[] vertices;
	public Vector3D[] normals;
	public int[] indices;
	public float[][] uvCoordinates;
	public Vector3D tempVector = new Vector3D(0,0,0);
	
	public Mesh(String objFilePath, String order) {
		
		//构建文件路径
		String filePath = MainThread.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		objFilePath = filePath + objFilePath;

		
		//从文件中读取顶点，法线的个数
        int numVertices = 0;
        int numNormals = 0;
        int numIndices = 0;
        int numUVs = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(objFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("v ")) {
                    numVertices++;
                } else if (line.startsWith("vn ")) {
                    numNormals++;
                } else if (line.startsWith("f ")) {
                    numIndices += 3;
                } else if(line.startsWith("vt ")) {
                	numUVs++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
        //创建用来存储顶点位置， 法线的数组
        vertices = new Vector3D[numVertices];
        normals = new Vector3D[numVertices];
        indices = new int[numIndices];
        uvCoordinates = new float[numIndices][];
        
        //创建一个单独数组用来帮助调整法线数和UV数组的组索引， 这样顶点位置和法线和UV数组的索引就能对应上来
        Vector3D[] normalsTemp = new Vector3D[numNormals];
        float[][] uvCoordinatesTemp = new float[numUVs][];
        
        //再重新读一边这个 OBj文件
        try (BufferedReader br = new BufferedReader(new FileReader(objFilePath))) {
        	String line;
        	int vertexIndex = 0;
        	int normalIndex = 0;
        	int indexIndex = 0;
        	int uvIndex = 0;

        	while ((line = br.readLine()) != null) {
        		if (line.startsWith("v ")) {
        			String[] values = line.split("\\s+");        //读取顶点位置
        			float x = Float.parseFloat(values[1]);
        			float y = Float.parseFloat(values[2]);
        			float z = Float.parseFloat(values[3]);
        			vertices[vertexIndex++] = new Vector3D(x, y, z);
        		} else if (line.startsWith("vn ")) {
        			String[] values = line.split("\\s+");        //读取法线位置，存放在临时的法线数组中
        			float x = Float.parseFloat(values[1]);
        			float y = Float.parseFloat(values[2]);
        			float z = Float.parseFloat(values[3]);
        			normalsTemp[normalIndex++] = new Vector3D(x, y, z);
        		}else if(line.startsWith("vt ")) {
        			String[] values = line.split("\\s+"); 
        			float u = Float.parseFloat(values[1]);
        			float v = Float.parseFloat(values[2]);
        			uvCoordinatesTemp[uvIndex++] = new float[]{u,v};
        		}else if (line.startsWith("f ")) {
        			String[] values = line.split("\\s+");
        			
        			if(order != "clockwise") {
	        			for (int i = 3; i >= 1; i--) {
	        				String[] indicesValues = values[i].split("/");
	        				int vertexIndexValue = Integer.parseInt(indicesValues[0]) - 1;
	        				int uvIndexValue = Integer.parseInt(indicesValues[1]) - 1;
	        				int normalIndexValue = Integer.parseInt(indicesValues[2])-1;
	        				normals[vertexIndexValue] = normalsTemp[normalIndexValue];
	        				uvCoordinates[indexIndex] = uvCoordinatesTemp[uvIndexValue];
	        				indices[indexIndex++] = vertexIndexValue;
	        			}
        			}else {
        				 for (int i = 1; i <= 3; i++) {
  	                        String[] indicesValues = values[i].split("/");
  	                        int vertexIndexValue = Integer.parseInt(indicesValues[0]) - 1;
  	                        int uvIndexValue = Integer.parseInt(indicesValues[1]) - 1;
  	                        int normalIndexValue = Integer.parseInt(indicesValues[2])-1;
  	                        normals[vertexIndexValue] = normalsTemp[normalIndexValue];
  	                        uvCoordinates[indexIndex] = uvCoordinatesTemp[uvIndexValue];
  	                        indices[indexIndex++] = vertexIndexValue;
  	                    }
        			}
        		}
        	}
        } catch (IOException e) {
        	e.printStackTrace();
        }
        
        for(int i = 0; i < normals.length; i++) {
        	if(normals[i] == null)
        		normals[i] = new Vector3D(0,0,1);
        }
	}
	
	
	
}
