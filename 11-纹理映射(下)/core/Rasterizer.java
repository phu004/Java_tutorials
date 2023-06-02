package core;

//只用于三角形的渲染
public class Rasterizer {

  	//顶点缓冲对象数组，设定最多可以处理1000个顶点缓冲对象
  	public static VertexBufferObject[] VBOs = new VertexBufferObject[1000];
  	
  	//记载每一频加载的VBO个数
  	public static int numberOfVBOs;
  	
  	//用来处理着色的线程
  	public static Shader[] shaders = new Shader[5];
  	
  
    //初始化光栅渲染器
  	public static void init(){
  		//初始化着色器线程并让它们运行起来
  		for(int i = 0; i < shaders.length; i++) {
  			shaders[i] = new Shader("shader"+i);
  			shaders[i].start();
  		}
  		
  	}
  	
  	//在每一频的开始，把渲染器上一频的信息清除
  	public static void prepare() {
  		numberOfVBOs = 0;
  		
  		//把场景平均分配给着色器进行渲染
  		shaders[0].VBOsStart = 0;
  		shaders[0].VBOsEnd = 1;
  		
  		shaders[1].VBOsStart = 1;
  		shaders[1].VBOsEnd = 2;
  		
  		shaders[2].VBOsStart = 2;
  		shaders[2].VBOsEnd = 3;
  		
  		shaders[3].VBOsStart = 3;
  		shaders[3].VBOsEnd = 4;
  		
  		shaders[4].VBOsStart = 4;
  		shaders[4].VBOsEnd = 6;
  		
  	}
  	
  	//加载一个VBO
  	public static void addVBO(VertexBufferObject VBO) {
  		VBOs[numberOfVBOs] = VBO;
  		numberOfVBOs++;
  	}
  	
    //渲染器的入口
  	public static void renderScene(){
  			
  		//让着色器开始工作
		for(int i = 0; i < shaders.length; i++) {
			synchronized(shaders[i]) { 
				shaders[i].notify();
				shaders[i].isWorking = true;
			}
		}
		
		//等着色器线程完成工作
		for(int i = 0; i < shaders.length; i++) {
			synchronized(shaders[i].myLock) {
				while(shaders[i].isWorking){
					try {
						shaders[i].myLock.wait();
					} catch (InterruptedException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}
		
		//计算渲染的三角形总数
		for(int i = 0; i < shaders.length; i++)
			MainThread.triangleCount+=shaders[i].triangleCount;
		
  	}
}