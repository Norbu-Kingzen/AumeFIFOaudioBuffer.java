
/**
 * 
 * AumeFIFOaudioBuffer
 * Miles Thorogood.
 * Contact: mthorogo@sfu.ca
 * 
 * 
 * AumeFIFOaudioBuffer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */

import java.util.concurrent.ArrayBlockingQueue;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioBufferRecord {

	
	private static int SAMPLE_RATE = 16000;

	public static final int FRAME_SIZE = 1024 * 2 ; // number of (16bit) samples in a frame
	public static float WINDOW_SIZE = 4.f ; // in seconds
	
	private AudioRecord mRecorder; // the main recording object
	private byte[] tBuffer; // record data into this buff
	private byte[] mBuffer; // log to this buff for correct FRAME_SIZE size
	private int mCounter ; // keep track of where we are up to in mBuffer
    private static ArrayBlockingQueue<byte[]> windowQ ; // this is the FIFO buffer
	private boolean mIsRecording = false;
	
	
	
	AudioBufferRecord(float analysisLength) {
		WINDOW_SIZE = analysisLength ; // analysis window in seconds
		this.initRecorder() ;
	     
	}
	
	
	public int getSampleRate() {
		return SAMPLE_RATE;
	}


	public static void setSAMPLE_RATE(int sAMPLE_RATE) {
		SAMPLE_RATE = sAMPLE_RATE;
	}


	public void beginRecording() {
		if (!mIsRecording) {
			mIsRecording = true;
			mRecorder.startRecording();
	        mIsRecording = true;
			startRecording();
			//mRecording = getFile("raw");
			//startBufferedWrite(mRecording);
		}
	}
	
	public void endRecording() {
		mIsRecording = false;
		mRecorder.stop();
	}
	
	public void killRecording() {
		mIsRecording = false ;
		mRecorder.release();
		mRecorder = null ;
	}
	
	public byte[] getBuffer() {
		// get the audio data from the FIFO buffer and flaten the array
		byte[][] test = windowQ.toArray(new byte[0][0]) ;
		byte[] rawData = new byte[test.length * test[0].length]; // because 16bit is two bytes
		int count = 0 ;
		
		for(int i=0; i<test.length; i++) {
			for(int j=0; j< test[0].length; j++) {
				//short x = test[i][j] ;
				//rawData[count++] = (byte)(x & 0xff) ; // high byte first
				//rawData[count++] = (byte)((x >> 8) & 0xff) ; // low byte
				rawData[count++] = test[i][j] ;
			}
		}
		return rawData;		
	}
	

	/*
	** Audio Recording Management
	*/
	
	private void initRecorder() {
		System.out.println("Initializing Audio Recorder");
		int bufferSize = AudioRecord.getMinBufferSize(getSampleRate(), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
		tBuffer = new byte[bufferSize];
		
		mBuffer = new byte[FRAME_SIZE]; // will be half this two bytes to a 16bit sample
		
		mCounter = 0 ;
		mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, getSampleRate(), AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		
		int num_blocks = (int)(WINDOW_SIZE/((float)(FRAME_SIZE/2)/(float)getSampleRate())) ;
		
		windowQ = new ArrayBlockingQueue<byte[]>(num_blocks) ;
	}
	

	
    private void startRecording(){

        System.out.println("Starting Recorder");
        new Thread(new Runnable() {
        	
                @Override
                public void run() {
                	
                    while (mIsRecording) {
                        int result = mRecorder.read(tBuffer, 0, tBuffer.length);
                        //System.out.println(result) ;
                        // if we have a results then copy it over to our window length buffer
                        // and put that into our FIFO Queue
                        if (result > 0) {
                        	//double sum = 0;

                        	for(int i=0; i<tBuffer.length; i++) {
                        		mBuffer[mCounter] = tBuffer[i] ;
                        		mCounter ++ ;
                        		if(mCounter >= mBuffer.length ) {
                        			mCounter = 0;
                        			//System.out.println("MBuff length is: "+ mBuffer.length);
                        			try{
                                		if(windowQ.remainingCapacity() < 1) {
                                			//System.out.println("Now we take it from the top");
                                			windowQ.take() ; // remove the head
                                		}
                                		byte[] temp = new byte[mBuffer.length] ;
                                
                						System.arraycopy( mBuffer, 0, temp, 0, mBuffer.length );
                                		windowQ.put(temp) ; // insert onto the tail
                                	}catch(Exception e){
                                		e.printStackTrace();
                                	}
                        		}
                        	}
                        	
                            //window[index_counter] = sarray ;
                    		//index_counter = (index_counter++) % frames_in_window ;
                           // --your stuffs--
                    		//mBuffer = new short[BUFFERSIZE];
                        } else if (result == AudioRecord.ERROR_INVALID_OPERATION) {
                            Log.e("Recording", "Invalid operation error");
                            break;
                        } else if (result == AudioRecord.ERROR_BAD_VALUE) {
                            Log.e("Recording", "Bad value error");
                            break;
                        } else if (result == AudioRecord.ERROR) {
                            Log.e("Recording", "Unknown error");
                            break;
                        }
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        }).start();
    }
    
	
	
}
