package com.lzy.edit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;



/**
 * UncaughtException处理类,当程序发生Uncaught异常的时候,有该类来接管程序,并记录发送错误报告.
 * 
 * @author user
 * 
 */
public class CrashHandler implements UncaughtExceptionHandler {

    public static final String TAG = "System.out";

    //系统默认的UncaughtException处理类 
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    //CrashHandler实例
    private static CrashHandler INSTANCE = new CrashHandler();
    //程序的Context对象
    private Context mContext;
    //用来存储设备信息和异常信息
    private Map<String, String> infos = new HashMap<String, String>();

    //用于格式化日期,作为日志文件名的一部分
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");

    /** 保证只有一个CrashHandler实例 */
    private CrashHandler() {}

    /** 获取CrashHandler实例 ,单例模式 */
    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    /**
     * 初始化
     * 
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时会转入该函数来处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                Thread.sleep(3000 * 60);
            } catch (InterruptedException e) {

            }
            //退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(0);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     * 
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //使用对话框来显示异常信息
        new Thread(new ExceptionThread(ex)).start();
        //收集设备参数信息 
        collectDeviceInfo(mContext);
        //保存日志文件 
        //saveCrashInfo2File(ex);
        return true;
    }

    /**
     * 收集设备参数信息
     * @param context
     */
    public void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                Log.d(TAG, field.getName() + " : " + field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    /**
     * 保存错误信息到文件中
     * 
     * @param ex
     * @return      返回文件名称,便于将文件传送到服务器
     */
    private String saveCrashInfo2File(Throwable ex) {

        StringBuffer buf = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            buf.append(key + "=" + value + "\n");
        }

        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        buf.append(result);
        try {
            long timestamp = System.currentTimeMillis();
            String time = formatter.format(new Date());
            String fileName = "crash-" + time + "-" + timestamp + ".log";
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                String path = "/sdcard/crash/";
                File dir = new File(path);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName);
                fos.write(buf.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
        }
        return null;
    }


    /**
     * 检查当前网络是否可用
     * 
     * @param context
     * @return
     */

    public boolean isNetworkAvailable(Context context) {
        // 获取手机所有连接管理对象（包括对wi-fi,net等连接的管理）
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        } else {
            // 获取NetworkInfo对象
            NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();

            if (networkInfo != null && networkInfo.length > 0) {
                for (int i = 0; i < networkInfo.length; i++) {
                    // 判断当前网络状态是否为连接状态
                    if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /*
     *启动线程弹出对话框
     */
    class ExceptionThread implements Runnable {

        private Throwable ex;
        private String execption ;

        public ExceptionThread(Throwable ex) {
            this.ex = ex;
            this.getException();
        }

        //获取程序堆栈错误信息
        public void getException() {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            ex.printStackTrace(printWriter);
            Throwable cause = ex.getCause();
            while (cause != null) {
                cause.printStackTrace(printWriter);
                cause = cause.getCause();
            }
            printWriter.close();
            execption = writer.toString();
        }

        @Override
        public void run() {
            // TODO: Implement this method
            Looper.prepare();
            showErrorDialog(mContext.getResources().getString(R.string.error) + execption);
            Looper.loop();
        }


        //打开错误对话框
        public void showErrorDialog(String msg) {
            final View view = LayoutInflater.from(mContext).inflate(R.layout.error_dialog, null);
            final TextView errorTextView = (TextView) view.findViewById(R.id.errorTextView);
            errorTextView.setText(msg);
            //创建对话框
            final AlertDialog.Builder builder = new AlertDialog.Builder(mContext, R.style.Error_dialog);
			builder.setIcon(R.drawable.svg_error);
			builder.setTitle(mContext.getResources().getString(R.string.error_dialog_title));
			builder.setView(view);
			builder.setCancelable(false);
			builder.setPositiveButton(mContext.getResources().getString(R.string.positive_button_text), 
				new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Implement this method
                        //判断网络是否可用，发送错误信息至服务器
                        if (isNetworkAvailable(mContext)) {
                            //saveCrashInfo2File(ex);
                            new SendCrashLog().execute(execption);
                        }              
                        dialog.dismiss();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                });
			builder.setNegativeButton(mContext.getResources().getString(R.string.negative_button_text),
				new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO: Implement this method
                        dialog.dismiss();
                        android.os.Process.killProcess(android.os.Process.myPid());
                        System.exit(0);
                    }
                });
			final AlertDialog dialog = builder.create();
            Window window = dialog.getWindow();
            WindowManager.LayoutParams attr = window.getAttributes();
            attr.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;       
            window.setAttributes(attr);
            dialog.show();
		}
    }


    class SendCrashLog extends AsyncTask<String, String, Boolean> {

        public SendCrashLog() {}

        @Override
        protected Boolean doInBackground(String... params) {
            if (params[0].length() == 0) {
                return false;
            }
            HttpClient httpClient = new DefaultHttpClient();
            //你的服务器，这里只是举个例子。把异常信息当作http请求发送到服务器
            HttpPost httpPost = new HttpPost("http://www.liuzhiyong.com/");
            //这里把相关的异常信息转为http post请求的数据参数
            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
                nameValuePairs.add(new BasicNameValuePair("model", params[0]));
                nameValuePairs.add(new BasicNameValuePair("device", params[1]));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                //发送相关请求信息
                httpClient.execute(httpPost);
            } catch (Exception e) {
                return false;
            } 
            Log.d(TAG, "Device model sent.");
            return true;
        }

		@Override
		protected void onPostExecute(Boolean result) {
			// TODO: Implement this method
			super.onPostExecute(result);
		}
    }
}
