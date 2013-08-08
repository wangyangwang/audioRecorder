package com.ppmeet;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Timer;
import java.util.TimerTask;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

public class TestAudioRecord extends Activity {
	// 音频获取源
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 44100;	
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	private static int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// 缓冲区字节大小
	private int bufferSizeInBytes = 0;
	private Button Start;
	private Button Stop;
	private Button Playback;
	private Button btn_set_timer;
	private AudioRecord audioRecord;
	static int numberPerG = 4;
	private boolean isRecord = false;// 设置正在录制的状态
	// AudioName裸音频数据文件
	private static final String AudioName = "/mnt/sdcard/love.raw";
	private static final String AudioName2 = "/mnt/sdcard/love2.raw";
	// private static final String AudioName = "R.raw.love";
	// NewAudioName可播放的音频文件
	private static final String NewAudioName = "/mnt/sdcard/new.wav";
	// create a player
	private MediaPlayer mPlayer;
	boolean mStartPlaying = true;
	// timer argus
	private Timer mTimer = null;
	private TimerTask mTimerTask = null;
	private TextView tv_timer;
	private final int PLAY_COMPLETION = 1;
	private final int PLAY_STOP = 2;
	private final int RECORD_TIMER = 3;
	private final int TIMES_UP = 4;
	private Calendar calendar;
	private InnerReceiver receiver;

	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			super.handleMessage(msg);

			switch (msg.what) {
			case PLAY_COMPLETION:
				mStartPlaying = !mStartPlaying;
				Playback.setText("Start playing");
				break;

			case PLAY_STOP:
				if (mStartPlaying) {
					Playback.setText("Stop playing");
				} else {
					Playback.setText("Start playing");
				}
				mStartPlaying = !mStartPlaying;
				break;

			case RECORD_TIMER:

				tv_timer.setText(String.valueOf(msg.arg1));

				break;
				
			case TIMES_UP:
				
				if(isRecord){
					stopRecord();
					Start.setEnabled(true);
					Stop.setEnabled(false);
					play_stop();
				}
				
				break;

			default:
				break;
			}

		}

	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		init();
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.ppmeet.time");
		receiver = new InnerReceiver();
		registerReceiver(receiver, filter);
	}

	private void init() {
		tv_timer = (TextView) findViewById(R.id.tv_timer);
		Start = (Button) this.findViewById(R.id.start);
		Stop = (Button) this.findViewById(R.id.stop);
		Stop.setEnabled(false);
		btn_set_timer = (Button) this.findViewById(R.id.btn_setalarm);
		Playback = (Button) this.findViewById(R.id.playback);
		Start.setOnClickListener(new TestAudioListener());
		Stop.setOnClickListener(new TestAudioListener());
		Playback.setOnClickListener(new TestAudioListener());
		btn_set_timer.setOnClickListener(new TestAudioListener());
		// creatAudioRecord();
		calendar = Calendar.getInstance();
	}

	private void creatAudioRecord() {
		// 获得缓冲区字节大小
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// 创建AudioRecord对象
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes);
	}

	private void onPlay(boolean start) {
		if (start) {
			startPlaying();
		} else {
			stopPlaying();
		}
	}

	class TestAudioListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (v == Start) {
				startRecord();
				Start.setEnabled(false);
				Stop.setEnabled(true);
				// startTimer();
			}
			if (v == Stop) {
				stopRecord();
				Start.setEnabled(true);
				Stop.setEnabled(false);
				// stopTimer();
			}
			if (v == Playback) {
				play_stop();
			}
			
			if(v == btn_set_timer){
				calendar.setTimeInMillis(System.currentTimeMillis());
				new TimePickerDialog(TestAudioRecord.this,new TimePickerDialog.OnTimeSetListener(){

					@Override
					public void onTimeSet(TimePicker arg0, int h, int m) {
						// TODO Auto-generated method stub
						//更新按钮上的时间
						tv_timer.setText(h+":"+m);
						//设置日历的时间，主要是让日历的年月日和当前同步
						calendar.setTimeInMillis(System.currentTimeMillis());
						//设置日历的小时和分钟
						calendar.set(Calendar.HOUR_OF_DAY, h);
						calendar.set(Calendar.MINUTE, m);
						//将秒和毫秒设置为0
						calendar.set(Calendar.SECOND, 0);
						calendar.set(Calendar.MILLISECOND, 0);
						//建立Intent和PendingIntent来调用闹钟管理器
						Intent intent = new Intent(TestAudioRecord.this,AlarmReceiver.class);
						intent.setAction("com.ppmeet.alarm");
						PendingIntent pendingIntent = PendingIntent.getBroadcast(TestAudioRecord.this, 0, intent, 0);
						//获取闹钟管理器
						AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
						//设置闹钟
						alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
						//alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), 10*1000, pendingIntent);
						Toast.makeText(TestAudioRecord.this, "设置成功！", Toast.LENGTH_SHORT).show();
					}
					
				},calendar.get(Calendar.HOUR_OF_DAY),calendar.get(Calendar.MINUTE),true).show();
					
			}

		}

	}

	private void play_stop() {

		onPlay(mStartPlaying);

		Message msg = mHandler.obtainMessage();
		msg.what = PLAY_STOP;
		mHandler.sendMessage(msg);

	}

	private void startRecord() {
		creatAudioRecord();
		audioRecord.startRecording();
		// 让录制状态为true
		isRecord = true;
		// 开启音频文件写入线程
		new Thread(new AudioRecordThread()).start();
	}

	private void stopRecord() {

		isRecord = false;// 停止文件写入
		close();
	}

	private void close() {

		if (audioRecord != null) {
			System.out.println("stopRecord");
			audioRecord.stop();
			audioRecord.release();// 释放资源
			audioRecord = null;
		}
	}

	private void reverse() {
		Log.v("tag", "reverse() start");
		File file = new File(AudioName);
		File file2 = new File(AudioName2);
		int size = (int) file.length();
		byte[] rawAudioData = new byte[size];
		byte[] newOrderAudioData = new byte[size];
		try {
			BufferedInputStream buf = new BufferedInputStream(
					new FileInputStream(file));
			buf.read(rawAudioData, 0, rawAudioData.length);
			buf.close();
			Log.v("tag", "read rawaudiodata successfully!");
			Log.v("tag", Integer.toString(buf.read()));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		byte[][] twod = new byte[rawAudioData.length / numberPerG][numberPerG];
		byte[][] reversedTwod = new byte[rawAudioData.length / numberPerG][numberPerG];

		for (int i = 0; i < rawAudioData.length / numberPerG; i++) {
			for (int j = 0; j < numberPerG; j++) {
				twod[i][j] = rawAudioData[(i * numberPerG) + j];
			}
		}

		for (int i = 0; i < rawAudioData.length / numberPerG; i++) {
			for (int j = 0; j < numberPerG; j++) {
				reversedTwod[i][j] = twod[(rawAudioData.length / numberPerG)
						- i - 1][j];
			}
		}
		for (int i = 0; i < rawAudioData.length / numberPerG; i++) {
			for (int j = 0; j < numberPerG; j++) {
				newOrderAudioData[i * (numberPerG) + j] = reversedTwod[i][j];
			}
		}

		FileOutputStream readinto = null;
		try {
			readinto = new FileOutputStream(AudioName2);

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		

		try {
			readinto.write(newOrderAudioData, 0, size);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			readinto.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();// 往文件中写入裸数据
			copyWaveFile(AudioName2, NewAudioName);// 给裸数据加上头文件
		}
	}


	/**
	 * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
	 * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
	 * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
	 */
	private void writeDateTOFile() {
		// new一个byte数组用来存一些字节数据，大小为缓冲区大小
		byte[] audiodata = new byte[bufferSizeInBytes];

		FileOutputStream fos = null;
		int readsize = 0;
		try {
			File file = new File(AudioName);
			if (file.exists()) {
				file.delete();
			}
			fos = new FileOutputStream(file);// 建立一个可存取字节的文件
		} catch (Exception e) {
			e.printStackTrace();
		}
		while (isRecord == true) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
				try {

					// write file
					fos.write(audiodata);

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			fos.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		reverse();

	}

	// 这里得到可播放的音频文件
	private void copyWaveFile(String inFilename, String outFilename) {
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = sampleRateInHz;
		int channels = 2;
		long byteRate = 16 * sampleRateInHz * channels / 8;
		byte[] data = new byte[bufferSizeInBytes];
		try {

			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);

			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;
			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);
			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {
		byte[] header = new byte[44];
		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = 16; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
		out.write(header, 0, 44);
	}

	private void startPlaying() {
		mPlayer = new MediaPlayer();
		try {
			mPlayer.setDataSource(NewAudioName);
			mPlayer.prepare();
			mPlayer.start();
			mPlayer.setOnCompletionListener(new OnCompletionListener() {

				@Override
				public void onCompletion(MediaPlayer arg0) {
					// TODO Auto-generated method stub
					Message msg = mHandler.obtainMessage();
					msg.what = PLAY_COMPLETION;
					mHandler.sendMessage(msg);

				}
			});
		} catch (IOException e) {

		}
	}

	private void stopPlaying() {
		mPlayer.release();
		mPlayer = null;
	}

	@Override
	protected void onDestroy() {
		close();
		unregisterReceiver(receiver);
		super.onDestroy();
	}

	private void startTimer() {

		if (mTimer == null) {
			mTimer = new Timer();
		}

		if (mTimerTask == null) {
			mTimerTask = new TimerTask() {
				@Override
				public void run() {
					startRecord();
				}
			};
		}

		if (mTimer != null && mTimerTask != null)
			mTimer.schedule(mTimerTask, 0, 10000);

	}

	private void stopTimer() {

		stopRecord();

		if (mTimer != null) {
			mTimer.cancel();
			mTimer = null;
		}

		if (mTimerTask != null) {
			mTimerTask.cancel();
			mTimerTask = null;
		}

	}
	
	class InnerReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			if(intent.getAction().equals("com.ppmeet.time")){
				
				mHandler.sendEmptyMessage(TIMES_UP);
			}
		}
		
	}
  

}
