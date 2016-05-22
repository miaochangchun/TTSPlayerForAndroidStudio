package com.example.ttsplayer;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.init.HciCloudSysHelper;
import com.sinovoice.hcicloudsdk.android.tts.player.TTSPlayer;
import com.sinovoice.hcicloudsdk.common.tts.TtsConfig;
import com.sinovoice.hcicloudsdk.common.tts.TtsInitParam;
import com.sinovoice.hcicloudsdk.player.TTSCommonPlayer;
import com.sinovoice.hcicloudsdk.player.TTSPlayerListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final int INIT_SUCCESS = 0;
    private EditText inputText;
    private String XIAOKUN = "tts.cloud.xiaokun";
    private String WANGJING = "tts.cloud.wangjing";
    private String CAP_KEY;
    private static final String TAG = MainActivity.class.getSimpleName();
    private Button playButton;
    private Button pauseButton;
    private Button resumeButton;
    private Button stopButton;
    private HciCloudSysHelper mHciCloudSysHelper;
    private TTSPlayer mTtsPlayer;
    private TtsConfig ttsConfig;
    private Spinner selectVoice;
    List<String> list = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case INIT_SUCCESS:
                    Bundle bundle = msg.getData();
                    boolean b = bundle.getBoolean("errCode");
                    if(true == b){
                        Toast.makeText(MainActivity.this, "初始化成功", Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mTtsPlayer != null) {
            mTtsPlayer.release();
        }
        mHciCloudSysHelper.release();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputText = (EditText) findViewById(R.id.input_text);
        playButton = (Button) findViewById(R.id.play_button);
        pauseButton = (Button) findViewById(R.id.pause_button);
        resumeButton = (Button) findViewById(R.id.resume_button);
        stopButton = (Button) findViewById(R.id.stop_button);

        list.add("王静");
        list.add("小坤");
        selectVoice = (Spinner) findViewById(R.id.select_voice);
        //第二步：为下拉列表定义一个适配器，这里就用到里前面定义的list。
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item, list);
        //第三步：为适配器设置下拉列表下拉时的菜单样式。
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //第四步：将适配器添加到下拉列表上
        selectVoice.setAdapter(adapter);

        mHciCloudSysHelper = HciCloudSysHelper.getInstance();
        mTtsPlayer = new TTSPlayer();

        new Thread(new Runnable() {
            @Override
            public void run() {
                //灵云系统和TTS播放器初始化放到子线程中，初始化成功之后通过Handler通知主线程
                boolean b = init();
                //获取Message信息
                Message message = Message.obtain();
                //设置成功时的返回状态
                message.what = INIT_SUCCESS;
                //初始化的返回值通过Bundle传送给主线程
                Bundle bundle = message.getData();
                //设置key=errCode，value=b
                bundle.putBoolean("errCode", b);
                message.setData(bundle);
                handler.sendMessage(message);
            }
        }).start();
        /**
         * 选择发音人
         */
        selectVoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch(position){
                    case 0:
                        CAP_KEY = WANGJING;
                        break;
                    case 1:
                        CAP_KEY = XIAOKUN;
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //默认设置为王静
                CAP_KEY = WANGJING;
            }
        });

        playButton.setOnClickListener(this);
        pauseButton.setOnClickListener(this);
        resumeButton.setOnClickListener(this);
        stopButton.setOnClickListener(this);
    }

    private boolean init() {
        boolean isSuccess = mHciCloudSysHelper.init(this);
        if (!isSuccess) {
            Log.e(TAG, "hci init error.");
            return false;
        }
        //播放器初始化
        isSuccess = initPlayer();
        if (!isSuccess) {
            Log.e(TAG, "hci ttsplayer error");
            return false;
        }
        return isSuccess;
    }

    /**
     * 播放器初始化
     * @return
     */
    private boolean initPlayer() {
        // 构造Tts初始化的帮助类的实例
        TtsInitParam ttsInitParam = new TtsInitParam();
        // 获取App应用中的lib的路径
        String dataPath = getBaseContext().getFilesDir().getAbsolutePath().replace("files", "lib");
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_DATA_PATH, dataPath);
        // 此处演示初始化的能力为tts.cloud.xiaokun, 用户可以根据自己可用的能力进行设置, 另外,此处可以传入多个能力值,并用;隔开
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_INIT_CAP_KEYS, WANGJING + ";" + XIAOKUN);
        ttsInitParam.addParam(TtsInitParam.PARAM_KEY_FILE_FLAG, "android_so");

        mTtsPlayer.init(ttsInitParam.getStringConfig(), new TTSEventProcess());

        if (mTtsPlayer.getPlayerState() == TTSPlayer.PLAYER_STATE_IDLE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 播放器合成函数
     * @param text
     */
    private void synth(String text, String voiceName) {
        // 配置播放器的属性。包括：音频格式，音库文件，语音风格，语速等等。详情见文档。
        ttsConfig = new TtsConfig();
        // 指定语音合成的能力(云端合成,发言人是XiaoKun)
        ttsConfig.addParam(TtsConfig.SessionConfig.PARAM_KEY_CAP_KEY, voiceName);
        // 音频格式
        ttsConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_AUDIO_FORMAT, "pcm16k16bit");
        // 设置合成语速
        ttsConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_SPEED, "5");
        // 设置合成音量   取值范围为0-9.99
        ttsConfig.addParam(TtsConfig.BasicConfig.PARAM_KEY_VOLUME, "5");

        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PLAYING
                || mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PAUSE) {
            mTtsPlayer.stop();
        }

        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_IDLE) {
            mTtsPlayer.play(text, ttsConfig.getStringConfig());
        } else {
            Toast.makeText(MainActivity.this, "播放器内部状态错误", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View v) {
        if (mTtsPlayer != null) {
            try {
                switch (v.getId()) {
                    case R.id.play_button:
                        //播放相关设置
                        synth(inputText.getText().toString(), CAP_KEY);
                        break;

                    case R.id.pause_button:
                        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PLAYING) {
                            mTtsPlayer.pause();
                        }
                        break;

                    case R.id.resume_button:
                        if (mTtsPlayer.getPlayerState() == TTSCommonPlayer.PLAYER_STATE_PAUSE) {
                            mTtsPlayer.resume();
                        }
                        break;
                    case R.id.stop_button:
                        if(mTtsPlayer.canStop()){
                            mTtsPlayer.stop();
                        }
                        break;
                    default:
                        break;
                }
            } catch (IllegalStateException ex) {
                Toast.makeText(getBaseContext(), "状态错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 播放器状态回调
     */
    private class TTSEventProcess implements TTSPlayerListener {

        @Override
        public void onPlayerEventPlayerError(TTSCommonPlayer.PlayerEvent playerEvent, int errorCode) {
            Log.i(TAG, "onError " + playerEvent.name() + " code: " + errorCode);
        }

        @Override
        public void onPlayerEventProgressChange(TTSCommonPlayer.PlayerEvent playerEvent, int start, int end) {
            Log.i(TAG, "onProcessChange " + playerEvent.name() + " from "
                    + start + " to " + end);
        }

        @Override
        public void onPlayerEventStateChange(TTSCommonPlayer.PlayerEvent playerEvent) {
            Log.i(TAG, "onStateChange " + playerEvent.name());
        }

    }
}