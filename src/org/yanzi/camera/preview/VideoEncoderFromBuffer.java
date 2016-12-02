package org.yanzi.camera.preview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;






import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

public class VideoEncoderFromBuffer {
	private static final String TAG = "VideoEncoderFromBuffer";
	private static final boolean VERBOSE = true; // lots of logging
	private static final String DEBUG_FILE_NAME_BASE = "/sdcard/Movies/h264";
	// parameters for the encoder
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
	private static final int FRAME_RATE = 25; // 15fps
	private static final int IFRAME_INTERVAL = FRAME_RATE; // 10 between
															// I-frames
	private static final int TIMEOUT_USEC = 10000;
	private static final int COMPRESS_RATIO = 256;
	private static final int IMAGE_HEIGHT = 480;
	private static final int IMAGE_WIDTH = 640;
	private static final int BIT_RATE = IMAGE_HEIGHT * IMAGE_WIDTH * 3 * 8 * FRAME_RATE / COMPRESS_RATIO; // bit rate CameraWrapper.
	private int mWidth;
	private int mHeight;
	private MediaCodec mMediaCodec;
	private MediaMuxer mMuxer;
	private BufferInfo mBufferInfo;
	private int mTrackIndex = -1;
	private boolean mMuxerStarted;
	byte[] mFrameData;
	FileOutputStream mFileOutputStream = null;
	private int mColorFormat;
	private long mStartTime = 0;
	private MediaFormat mActualOutputFormat;
	private ByteBuffer[] mDecoderInputBuffers;
    private ByteBuffer[] mEncoderOutputBuffers;
    private Surface mSurface;

	@SuppressLint("NewApi")
	public VideoEncoderFromBuffer(int width, int height) {
		Log.i(TAG, "VideoEncoder() MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface=" + MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		this.mWidth = width;
		this.mHeight = height;
		mFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
		
		mBufferInfo = new MediaCodec.BufferInfo();
		MediaCodecInfo codecInfo = selectCodec(MIME_TYPE);
		if (codecInfo == null) {
			// Don't fail CTS if they don't have an AVC codec (not here,
			// anyway).
			Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
			return;
		}
		if (VERBOSE)
			Log.d(TAG, "found codec: " + codecInfo.getName());
		mColorFormat = selectColorFormat(codecInfo, MIME_TYPE);
		if (VERBOSE)
			Log.d(TAG, "found colorFormat: " + mColorFormat);
		MediaFormat format = MediaFormat.createVideoFormat("video/avc", this.mWidth, this.mHeight);
        // From Nexus 4 Camera in 720p
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 10);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,1);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        
//		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,
//				this.mWidth, this.mHeight);
//		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
//		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
//		//mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, mColorFormat);
//		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
//				1);
//		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//		if (VERBOSE)
//			Log.d(TAG, "format: " + mediaFormat);
//		
//		String type = mediaFormat.getString(MediaFormat.KEY_MIME);
//		mMediaCodec = MediaCodec.createEncoderByType(type);
        
		mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
		
		mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mSurface = mMediaCodec.createInputSurface();
		mMediaCodec.start();
//	Bundle bitrate = new Bundle();
//	bitrate.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, 6000000);       
//	mMediaCodec.setParameters(bitrate);
   	  
		mEncoderOutputBuffers = mMediaCodec.getOutputBuffers();
		
		String fileName = DEBUG_FILE_NAME_BASE + this.mWidth + "x"
				+ this.mHeight + "2.mp4";
		Log.i(TAG, "videofile: " + fileName);
		// try {
		// mFileOutputStream = new FileOutputStream(fileName);
		// } catch (IOException e) {
		// System.out.println(e);
		// } catch (Exception e) {
		// System.out.println(e);
		// }

		mStartTime = System.nanoTime();

		// Create a MediaMuxer.  We can't add the video track and start() the muxer here,
        // because our MediaFormat doesn't have the Magic Goodies.  These can only be
        // obtained from the encoder after it has started processing data.
        //
        // We're not actually interested in multiplexing audio.  We just want to convert
        // the raw H.264 elementary stream we get from MediaCodec into a .mp4 file.
		try {
			mMuxer = new MediaMuxer("/sdcard/transcode_priv.mp4",
					MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
		} catch (IOException ioe) {
			throw new RuntimeException("MediaMuxer creation failed", ioe);
		}
		mTrackIndex = -1;
	    mMuxerStarted = false;
	}
	
	public Surface getSurface() {
		return mSurface;
	}
	
	@SuppressLint("NewApi")
	public int drainEncoder(long timeoutUs) {
        int result = mMediaCodec.dequeueOutputBuffer(mBufferInfo, timeoutUs);
        
        Log.d(TAG, "drainEncoder result=" + result + " mBufferInfo=" + mBufferInfo);
        if (mBufferInfo != null) {
        	Log.d(TAG, "drainEncoder=" + mBufferInfo.toString());
        }
        switch (result) {
            case MediaCodec.INFO_TRY_AGAIN_LATER:
            	Log.d(TAG, "drainEncoder INFO_TRY_AGAIN_LATER");
                return MediaCodec.INFO_TRY_AGAIN_LATER;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                if (mActualOutputFormat != null)
                    throw new RuntimeException("Video output format changed twice.");
                mActualOutputFormat = mMediaCodec.getOutputFormat();
                mTrackIndex = mMuxer.addTrack(mActualOutputFormat);
                
                mMuxer.start();
                mMuxerStarted = true;
                
                Log.d(TAG, "drainEncoder INFO_OUTPUT_FORMAT_CHANGED");
                return MediaCodec.INFO_OUTPUT_FORMAT_CHANGED;
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                //mEncoderOutputBuffers = mEncoder.getOutputBuffers();
            	mEncoderOutputBuffers = mMediaCodec.getOutputBuffers();
            	
            	Log.d(TAG, "drainEncoder INFO_OUTPUT_BUFFERS_CHANGED");
                return MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED;
        }

        if (mActualOutputFormat == null) {
            throw new RuntimeException("Could not determine actual output format.");
        }

        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
            mBufferInfo.set(0, 0, 0, mBufferInfo.flags);
        }
        if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            // SPS or PPS, which should be passed by MediaFormat.
        	mMediaCodec.releaseOutputBuffer(result, false);
        	
        	Log.d(TAG, "drainEncoder BUFFER_FLAG_CODEC_CONFIG");
            return MediaCodec.BUFFER_FLAG_CODEC_CONFIG;
        }
        
        if (!mMuxerStarted) {
//			throw new RuntimeException("muxer hasn't started");
			MediaFormat newFormat = mMediaCodec.getOutputFormat();
			mTrackIndex = mMuxer.addTrack(newFormat);
            mMuxer.start();
            mMuxerStarted = true;
		}
        
        ByteBuffer outputBuffer = mEncoderOutputBuffers[result];
     // adjust the ByteBuffer values to match BufferInfo (not needed?)
        outputBuffer.position(mBufferInfo.offset);
        outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
		
//		write raw data
//		byte[] outData = new byte[bufferInfo.size];
//		outputBuffer.get(outData);
//		outputBuffer.position(bufferInfo.offset);

//		try {
//			mFileOutputStream.write(outData);
//			Log.i(TAG, "output data size -- > " + outData.length);
//		} catch (IOException ioe) {
//			Log.w(TAG, "failed writing debug data to file");
//			throw new RuntimeException(ioe);
//		}
		mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
		
        //mMuxer.writeSampleData(QueuedMuxer.SampleType.VIDEO, mEncoderOutputBuffers[result], mBufferInfo);
        //mWrittenPresentationTimeUs = mBufferInfo.presentationTimeUs;
        mMediaCodec.releaseOutputBuffer(result, false);
        
        Log.d(TAG, "drainEncoder consume data");
        return 0;
    }
	
	@SuppressLint("NewApi")
	public void encodeFrame(byte[] input/* , byte[] output */) {
		Log.i(TAG, "encodeFrame()");
		long encodedSize = 0;
		NV21toI420SemiPlanar(input, mFrameData, this.mWidth, this.mHeight);

		ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
		ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
		int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
		if (VERBOSE)
			Log.i(TAG, "inputBufferIndex-->" + inputBufferIndex);
		if (inputBufferIndex >= 0) {
			long endTime = System.nanoTime();
			long ptsUsec = (endTime - mStartTime) / 1000;
			Log.i(TAG, "resentationTime: " + ptsUsec);
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(mFrameData);
			mMediaCodec.queueInputBuffer(inputBufferIndex, 0,
					mFrameData.length, System.nanoTime() / 1000, 0);
		} else {
			// either all in use, or we timed out during initial setup
			if (VERBOSE)
				Log.d(TAG, "input buffer not available");
		}

		int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
		Log.i(TAG, "outputBufferIndex-->" + outputBufferIndex);
		do {
			if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (VERBOSE)
					Log.d(TAG, "no output from encoder available");
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				outputBuffers = mMediaCodec.getOutputBuffers();
				if (VERBOSE)
					Log.d(TAG, "encoder output buffers changed");
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// not expected for an encoder
				
				MediaFormat newFormat = mMediaCodec.getOutputFormat();
                Log.d(TAG, "encoder output format changed: " + newFormat);

                // now that we have the Magic Goodies, start the muxer
                mTrackIndex = mMuxer.addTrack(newFormat);
                mMuxer.start();
                mMuxerStarted = true;
			} else if (outputBufferIndex < 0) {
				Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " +
						outputBufferIndex);
                // let's ignore it
			} else {
				if (VERBOSE)
					Log.d(TAG, "perform encoding");
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				if (outputBuffer == null) {
                    throw new RuntimeException("encoderOutputBuffer " + outputBufferIndex +
                            " was null");
                }
				
				if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    if (VERBOSE) Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

				if (mBufferInfo.size != 0) {
					if (!mMuxerStarted) {
//						throw new RuntimeException("muxer hasn't started");
						MediaFormat newFormat = mMediaCodec.getOutputFormat();
						mTrackIndex = mMuxer.addTrack(newFormat);
			            mMuxer.start();
			            mMuxerStarted = true;
					}

					// adjust the ByteBuffer values to match BufferInfo (not needed?)
					outputBuffer.position(mBufferInfo.offset);
					outputBuffer.limit(mBufferInfo.offset + mBufferInfo.size);
					
//					write raw data
//					byte[] outData = new byte[bufferInfo.size];
//					outputBuffer.get(outData);
//					outputBuffer.position(bufferInfo.offset);

//					try {
//						mFileOutputStream.write(outData);
//						Log.i(TAG, "output data size -- > " + outData.length);
//					} catch (IOException ioe) {
//						Log.w(TAG, "failed writing debug data to file");
//						throw new RuntimeException(ioe);
//					}
					mMuxer.writeSampleData(mTrackIndex, outputBuffer, mBufferInfo);
					if (VERBOSE) {
						Log.d(TAG, "sent " + mBufferInfo.size + " bytes to muxer");
					}
				}

				mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			}
			outputBufferIndex = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
		} while (outputBufferIndex >= 0);
	}

	@SuppressLint("NewApi")
	public void close() {
		// try {
		// mFileOutputStream.close();
		// } catch (IOException e) {
		// System.out.println(e);
		// } catch (Exception e) {
		// System.out.println(e);
		// }
		Log.i(TAG, "close()");
		try {
			mMediaCodec.stop();
			mMediaCodec.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (mMuxer != null) {
            // TODO: stop() throws an exception if you haven't fed it any data.  Keep track
            //       of frames submitted, and don't call stop() if we haven't written anything.
            mMuxer.stop();
            mMuxer.release();
            mMuxer = null;
        }
	}

	/**
	 * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU I420YUVSemiPlanar
	 * is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV Apply NV21 to
	 * I420YUVSemiPlanar(NV12) Refer to https://wiki.videolan.org/YUV/
	 */
	private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes,
			int width, int height) {
		System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
		for (int i = width * height; i < nv21bytes.length; i += 2) {
			i420bytes[i] = nv21bytes[i + 1];
			i420bytes[i + 1] = nv21bytes[i];
		}
	}

	/**
	 * Returns a color format that is supported by the codec and by this test
	 * code. If no match is found, this throws a test failure -- the set of
	 * formats known to the test should be expanded for new platforms.
	 */
	private static int selectColorFormat(MediaCodecInfo codecInfo,
			String mimeType) {
		MediaCodecInfo.CodecCapabilities capabilities = codecInfo
				.getCapabilitiesForType(mimeType);
		for (int i = 0; i < capabilities.colorFormats.length; i++) {
			int colorFormat = capabilities.colorFormats[i];
			if (isRecognizedFormat(colorFormat)) {
				return colorFormat;
			}
		}
		Log.e(TAG,
				"couldn't find a good color format for " + codecInfo.getName()
						+ " / " + mimeType);
		return 0; // not reached
	}

	/**
	 * Returns true if this is a color format that this test code understands
	 * (i.e. we know how to read and generate frames in this format).
	 */
	private static boolean isRecognizedFormat(int colorFormat) {
		switch (colorFormat) {
		// these are the formats we know how to handle for this test
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the first codec capable of encoding the specified MIME type, or
	 * null if no match was found.
	 */
	private static MediaCodecInfo selectCodec(String mimeType) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {
				continue;
			}
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					return codecInfo;
				}
			}
		}
		return null;
	}


}
