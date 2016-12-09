package org.camera.camera.preview;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;









import jp.co.cyberagent.android.av.RenderHelper;

import org.camera.camera.CameraInterface;

import com.android.grafika.gles.FlatShadedProgram;
import com.android.grafika.gles.FullFrameRect;
import com.android.grafika.gles.GlUtil;
import com.android.grafika.gles.Texture2dProgram;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.AttributeSet;
import android.util.Log;

public class CameraGLSurfaceView extends GLSurfaceView implements SurfaceTexture.OnFrameAvailableListener , GLSurfaceView.Renderer {
	private static final String TAG = "yanzi";
	Context mContext;
	SurfaceTexture mSurface;
	int mTextureID = -1;
	DirectDrawer mDirectDrawer;
	private RenderHelper mRenderHelper;
	private boolean mStartEncoder = false;
	private int mWidth;
	private int mHeight;
	private boolean mRecordingEnabled = false;
	public CameraGLSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		mContext = context;
		setEGLContextClientVersion(2);
		setRenderer(this);
		setRenderMode(RENDERMODE_WHEN_DIRTY);
		
		mRenderHelper = new RenderHelper(null, new File("/sdcard/output.mp4"), 1);
	}

	public void setRenderHelper() {
    	if (!mStartEncoder) {
    		mStartEncoder = true;
    		mRenderHelper.startEncoder(mWidth, mHeight);
    		mRecordingEnabled = true;
    		Log.d("Grafika", "startEncoder");
    	} else {
    		mRecordingEnabled = false;
    		mStartEncoder = false;
    		mRenderHelper.stopEncoder();
    		Log.d("Grafika", "stopEncoder");
    		
    	}
    	
//    	if (mRenderHelper == null) {
//    		mRenderHelper = new RenderHelper(null, new File("/sdcard/output.mp4"), 1);
//    		//renderHelper.startEncoder(mWidth, mHeight);
//    	}
    }
	
	public void closeRenderHelper() {
    	if (mRenderHelper != null) {
    		mRenderHelper.stopEncoder();
    	}
    }
	
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onSurfaceCreated..." + getId());
		mTextureID = createTextureID();
		mSurface = new SurfaceTexture(mTextureID);
		mSurface.setOnFrameAvailableListener(this);
		mDirectDrawer = new DirectDrawer(mTextureID);
		CameraInterface.getInstance().doOpenCamera(null);
		
		mRenderHelper.mRect.setTexture(mTextureID);
		mRenderHelper.mTexProgram = new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT);
		if (mRenderHelper != null) {
        	mRenderHelper.surfaceCreated();
        }
	}
	@Override
	public void onSurfaceChanged(GL gl, int width, int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onSurfaceChanged..." + getId());
		mWidth = width;
		mHeight = height;
		GLES20.glViewport(0, 0, width, height);
		if(!CameraInterface.getInstance().isPreviewing()){
			CameraInterface.getInstance().doStartPreview(mSurface, 1.33f);
		}
		
		if (mRenderHelper != null) {
        	Log.d(TAG, "onDrawFrame surfaceChanged");
        	//mRenderHelper.startEncoder(width, height);
        	mRenderHelper.surfaceChanged(width, height);
        }

	}
	@Override
	public void onDrawFrame(GL gl) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onDrawFrame...");
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mSurface.updateTexImage();
		float[] mtx = new float[16];
		mSurface.getTransformMatrix(mtx);
		mDirectDrawer.draw(mtx);
	}
	
	@Override
	public void onDrawFrame(GL gl, GLSurfaceView view) {
		Log.i(TAG, "onDrawFrame...");
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mSurface.updateTexImage();
		
		if (mRenderHelper != null) {
	        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mRenderHelper.mFramebuffer);
	        //mSurface.getTransformMatrix(mRenderHelper.mDisplayProjectionMatrix);
	        mRenderHelper.draw();
	        GlUtil.checkGlError("glBindFramebuffer");
	        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	        
	       

	        mRenderHelper.mFullScreen.drawFrame(mRenderHelper.mOffscreenTexture, mRenderHelper.mIdentityMatrix);
	        view.eglSwap();
	        
	        // Blit to encoder.
	        if (mRecordingEnabled && mRenderHelper.mVideoEncoder != null) {
	        	Log.d(TAG, "onDrawFrame videoen coder.");
	        	 mRenderHelper.mVideoEncoder.frameAvailableSoon();
	             mRenderHelper.mInputWindowSurface.makeCurrent();
	             //mRenderHelper.initFrameRect();
	             GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);    // again, only really need to
	             GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);     //  clear pixels outside rect
	             Log.d(TAG, "1onDrawFrame videoencoder. left=" + mRenderHelper.mVideoRect.left + "  top=" + mRenderHelper.mVideoRect.top
		        			+ "  width=" + mRenderHelper.mVideoRect.width() + " height=" + mRenderHelper.mVideoRect.height());
//	             GLES20.glViewport(mRenderHelper.mVideoRect.left, mRenderHelper.mVideoRect.top,
//	             		mRenderHelper.mVideoRect.width(), mRenderHelper.mVideoRect.height());
	             
	             GLES20.glViewport(0, 0, 1080, 1920);
	             //mRenderHelper.mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
	             //mFilter.onDraw(mRenderHelper.mOffscreenTexture, mGLCubeBuffer, mGLTextureBuffer);
	             if (mRenderHelper.mFullScreen2 == null) {
	            	 mRenderHelper.mFullScreen2 = new FullFrameRect(
		                     new Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_2D));
	             }
	             if (mRenderHelper.mProgram == null) {
	            	 mRenderHelper.mProgram = new FlatShadedProgram();
	             }
	             GlUtil.checkGlError("ooo");
	             mRenderHelper.mTri.draw(mRenderHelper.mProgram, mRenderHelper.mDisplayProjectionMatrix);
	             //mRenderHelper.mFullScreen.drawFrame(mRenderHelper.mOffscreenTexture, mRenderHelper.mIdentityMatrix);
	             mRenderHelper.mInputWindowSurface.setPresentationTime(System.nanoTime());
	             mRenderHelper.mInputWindowSurface.swapBuffers();

	             // Restore previous values.
	             GLES20.glViewport(0, 0, mWidth, mHeight);
	             GlUtil.checkGlError("ooo1");
	             
	             view.eglMakeCurrent();
	        }
	        
	       
	        
	        //mFilter.onDraw(mRenderHelper.mOffscreenTexture, mGLCubeBuffer, mGLTextureBuffer);
	       
	        
	        Log.d(TAG, "onDrawFrame end mGLTextureId" + mRenderHelper.mOffscreenTexture);
        }
	}
	
	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		CameraInterface.getInstance().doStopCamera();
	}
	private int createTextureID()
	{
		int[] texture = new int[1];

		GLES20.glGenTextures(1, texture, 0);
		GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
		GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);        
		GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
				GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

		return texture[0];
	}
	public SurfaceTexture _getSurfaceTexture(){
		return mSurface;
	}
	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onFrameAvailable...");
		this.requestRender();
	}

	
}
