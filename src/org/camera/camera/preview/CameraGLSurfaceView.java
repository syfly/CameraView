package org.camera.camera.preview;

import java.io.File;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;






import jp.co.cyberagent.android.av.RenderHelper;

import org.camera.camera.CameraInterface;

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
		Log.i(TAG, "onSurfaceCreated...");
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
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		// TODO Auto-generated method stub
		Log.i(TAG, "onSurfaceChanged...");
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
	public void onDrawFrame(GL10 gl) {
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
	public void onDrawFrame(GL10 gl, GLSurfaceView view) {
		Log.i(TAG, "onDrawFrame...");
		GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		mSurface.updateTexImage();
		
		if (mRenderHelper != null) {
	        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mRenderHelper.mFramebuffer);
	        mRenderHelper.draw();
	        GlUtil.checkGlError("glBindFramebuffer");
	        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
	        
	        mRenderHelper.mFullScreen.drawFrame(mRenderHelper.mOffscreenTexture, mRenderHelper.mIdentityMatrix);
	        //mFilter.onDraw(mRenderHelper.mOffscreenTexture, mGLCubeBuffer, mGLTextureBuffer);
	        view.eglSwap();

	        
	        // Blit to encoder.
	        if (mRecordingEnabled && mRenderHelper.mVideoEncoder != null) {
	        	Log.d(TAG, "onDrawFrame videoencoder.");
	        	 mRenderHelper.mVideoEncoder.frameAvailableSoon();
	             mRenderHelper.mInputWindowSurface.makeCurrent();
	             //mRenderHelper.initFrameRect();
	             GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);    // again, only really need to
	             GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);     //  clear pixels outside rect
	             Log.d(TAG, "1onDrawFrame videoencoder. left=" + mRenderHelper.mVideoRect.left + "  top=" + mRenderHelper.mVideoRect.top
		        			+ "  width=" + mRenderHelper.mVideoRect.width() + " height=" + mRenderHelper.mVideoRect.height());
//	             GLES20.glViewport(mRenderHelper.mVideoRect.left, mRenderHelper.mVideoRect.top,
//	             		mRenderHelper.mVideoRect.width(), mRenderHelper.mVideoRect.height());
	             
	             GLES20.glViewport(0, 0, 640, 720);
	             //mRenderHelper.mFullScreen.drawFrame(mOffscreenTexture, mIdentityMatrix);
	             //mFilter.onDraw(mRenderHelper.mOffscreenTexture, mGLCubeBuffer, mGLTextureBuffer);
	             mRenderHelper.mFullScreen.drawFrame(mRenderHelper.mOffscreenTexture, mRenderHelper.mIdentityMatrix);
	             mRenderHelper.mInputWindowSurface.setPresentationTime(System.nanoTime());
	             mRenderHelper.mInputWindowSurface.swapBuffers();

	             // Restore previous values.
	             GLES20.glViewport(0, 0, -1, -1);
	             
	             
	             view.eglMakeCurrent();
	        }
	        
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


//04-06 20:08:38.429: I/yanzi(23477): onSurfaceCreated...
//04-06 20:08:38.429: I/yanzi(23477): Camera open....
//04-06 20:08:38.689: I/yanzi(23477): Camera open over....
//04-06 20:08:38.689: I/yanzi(23477): onSurfaceChanged...
//04-06 20:08:38.689: I/yanzi(23477): doStartPreview...
//04-06 20:08:38.689: I/yanzi(23477): PictureSize : w = 1280h = 960
//04-06 20:08:38.689: I/yanzi(23477): PreviewSize:w = 960h = 720
//04-06 20:08:39.009: I/yanzi(23477): 最终设置:PreviewSize--With = 960Height = 720
//04-06 20:08:39.009: I/yanzi(23477): 最终设置:PictureSize--With = 1280Height = 960
//04-06 20:08:39.009: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.079: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.079: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.089: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.109: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.119: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.119: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.149: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.149: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.149: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.179: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.179: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.179: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.219: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.219: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.219: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.249: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.249: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.249: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.279: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.279: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.289: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.319: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.319: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.319: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.349: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.349: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.349: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.379: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.379: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.379: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.409: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.409: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.419: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.449: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.449: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.449: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.479: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.479: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.479: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.509: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.509: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.519: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.549: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.549: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.549: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.579: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.579: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.589: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.619: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.619: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.619: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.649: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.649: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.649: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.679: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.679: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.689: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.719: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.719: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.719: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.749: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.749: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.759: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.779: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.779: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.789: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.819: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.819: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.819: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.849: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.849: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.849: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.879: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.879: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.889: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.919: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.919: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.919: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.949: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.949: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.949: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:39.979: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:39.979: I/yanzi(23477): onDrawFrame...
//04-06 20:08:39.979: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.019: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.019: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.019: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.049: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.049: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.049: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.079: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.079: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.089: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.119: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.119: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.119: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.149: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.149: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.149: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.179: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.179: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.189: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.219: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.219: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.219: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.249: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.249: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.249: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.279: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.279: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.289: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.319: I/yanzi(23477): onFrameAvailable...
//04-06 20:08:40.319: I/yanzi(23477): onDrawFrame...
//04-06 20:08:40.339: D/yanzi(23477): onPreviewFrame data len=1036800
//04-06 20:08:40.729: I/yanzi(23477): onFrameAvailable...
