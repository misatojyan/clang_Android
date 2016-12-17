/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzy.edit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import java.util.ArrayList;
import android.widget.TextView;
import android.view.View.OnClickListener;
import android.widget.HorizontalScrollView;
import com.lzy.edit.BaseRecycleAdapter.ViewHolder;
import android.widget.Toast;


/*
 * The clang test demo
 * Author: Liuzhiyong
 + Email: 975434530@qq.com
 */


public class MainActivity extends Activity {


	private EditText mScrollEditText;
	private String[] cmdOptions;
	private String defaultFileName;
	private String sourceContent;
	private Thread updateThread;
	private int cursorIndex;
	private int actionBarHeight;
	private int statusBarHeight;
	private int screenWidth,screenHeight;

	private View popupView;
	private Rect rect ;
	private String expression = "";
	private String defaultComplete ;
	private PopupWindow mPopupWindow;
	private ScrollView mScrollView;
	private HorizontalScrollView mHorizontalScrollView;

	private RecyclerView autoCompleteRecyclerView;
	private RecyclerView diagnosisRecyclerView;
	private BaseRecycleAdapter autoCompleteAdapter;
	private BaseRecycleAdapter diagnosisAdapter;
	private ArrayList<String> autoCompleteList;
	private ArrayList<String> diagnosisList;

	private boolean isTextChanged = false;
	private boolean isThreadPause = false;
	private boolean isItemClick = false;

	public final static int AUTO_COMPLETE_RESULT = 0;
	public final static int SYNTAX_DIAGNOSIS_RESULT = 1;
	public final static int CLOSE_POPUP_WINDOW = 2;

	private Handler handler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			// TODO: Implement this method
			super.handleMessage(msg);
			switch (msg.what) {
				case AUTO_COMPLETE_RESULT:
					autoCompleteAdapter.notifyDataSetChanged();
					showPopupWindow(0, (int)msg.obj);
					break;
				case SYNTAX_DIAGNOSIS_RESULT:
					diagnosisAdapter.notifyDataSetChanged();
					break;
				case CLOSE_POPUP_WINDOW:
					mPopupWindow.dismiss();
					break;
			}
		}
	};


	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		/* Create a TextView and set its content.
		 * the text is retrieved by calling a native
		 * function.
		 */

        setContentView(R.layout.main_activity);

		// Init EditText
		mScrollEditText = (EditText) findViewById(R.id.mScrollEditText);
		mScrollEditText.setGravity(Gravity.LEFT | Gravity.TOP);

		mScrollEditText.setOnClickListener(clickListener);
		mScrollEditText.addTextChangedListener(watcher);

		mScrollView = (ScrollView) findViewById(R.id.mScrollView);
		mHorizontalScrollView = (HorizontalScrollView) findViewById(R.id.mHorizontalScrollView);
		rect = new Rect();

		autoCompleteList = new ArrayList<String>();
		diagnosisList = new ArrayList<String>();
		diagnosisList.add(JNI.getClangVersion());

		// Init diagnosis RecycleView
		diagnosisRecyclerView = (RecyclerView) findViewById(R.id.mRecyclerView);

		diagnosisRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		diagnosisAdapter = new BaseRecycleAdapter<String>(this, diagnosisList, R.layout.recycler_view_item) {

			@Override
			public void convertView(BaseRecycleAdapter.ViewHolder holder, String data, int position) {
				// TODO: Implement this method
				holder.setText(R.id.itemTextView, data, Color.MAGENTA);
			}

		};

		// Set Adapter
		diagnosisRecyclerView.setAdapter(diagnosisAdapter);

		// Init auto complete RecyclerView
		popupView = LayoutInflater.from(this).inflate(R.layout.popup_window, null);
		autoCompleteRecyclerView = (RecyclerView) popupView.findViewById(R.id.mRecyclerView);
		autoCompleteRecyclerView.setLayoutManager(new LinearLayoutManager(this));

		autoCompleteAdapter = new BaseRecycleAdapter<String>(this, autoCompleteList, R.layout.recycler_view_item){

			@Override
			public void convertView(BaseRecycleAdapter.ViewHolder holder, String data, int position) {
				// TODO: Implement this method
				int len = expression.length();

				int index = data.toLowerCase().indexOf(expression.toLowerCase());

				SpannableString spanString = new SpannableString(data);
				ForegroundColorSpan span = new ForegroundColorSpan(0xff009688);

				if (index >= 0 && !data.equals(defaultComplete)) {
					spanString.setSpan(span, index, index + len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

					((TextView)holder.getView(R.id.itemTextView)).setText(spanString);
				} else {
					holder.setText(R.id.itemTextView, data);
				}
			}
		};


		autoCompleteAdapter.setOnItemClickListener(new BaseRecycleAdapter.OnItemClickListener(){

				@Override
				public void onItemClick(BaseRecycleAdapter.ViewHolder holder, View v, int position) {
					// TODO: Implement this method
					if (!autoCompleteList.get(0).equals(defaultComplete)) {
						isItemClick = true;
						int start = cursorIndex - 1;
						while (start > 0) {

							char ch = sourceContent.charAt(start);
							if (String.valueOf(ch).matches("[^\\w|\\d|_]")) break;
							--start;
						}
						if (start != 0) start = start + 1;
						mScrollEditText.getEditableText().replace(start, cursorIndex, autoCompleteList.get(position));
					}

					mPopupWindow.dismiss();
				}

				@Override
				public void OnItemLongClick(BaseRecycleAdapter.ViewHolder holder, View v, int position) {
					// TODO: Implement this method
				}


			});

		autoCompleteRecyclerView.setAdapter(autoCompleteAdapter);


		WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
		screenWidth = wm.getDefaultDisplay().getWidth();
		screenHeight = wm.getDefaultDisplay().getHeight();

		defaultComplete = "No matches";
		defaultFileName = "untitled.cpp";
		cmdOptions = new String[]{"-I/sdcard/system/include","-fsyntax-only","-std=c++11"};

		updateThread = new Thread(autoCompleteThread);
		updateThread.start();

    }



	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		// TODO: Implement this method
		super.onWindowFocusChanged(hasFocus);

		if (actionBarHeight == 0)
			actionBarHeight = getActionBar().getHeight();

		if (statusBarHeight == 0) {
			getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
			statusBarHeight = rect.top;
		}
	}



    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO: Implement this method
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// TODO: Implement this method
		switch (item.getItemId()) {
			case android.R.id.home:
				break;
			case R.id.action_settings:
				break;
		}
		return super.onOptionsItemSelected(item);
	}



	private TextWatcher watcher = new TextWatcher(){

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int before) {
			// TODO: Implement this method
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int count, int after) {
			// TODO: Implement this method

			sourceContent = s.toString();
			isTextChanged = true;
			updateThread.interrupt();

		}

		@Override
		public void afterTextChanged(Editable edit) {
			// TODO: Implement this method
		}
	};

	private Runnable autoCompleteThread = new Runnable(){

		@Override
		public void run() {
			// TODO: Implement this method

			while (!isThreadPause) {

				// Thread sleep
				if (!isTextChanged) {
					threadSleep(Long.MAX_VALUE);
				}
				// Syntax diagnosis
				syntaxDiagnosis();
				// Auto complete
				autoComplete();

				isTextChanged = false;
			}
		}
	};


	public void threadSleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


	public void syntaxDiagnosis() {
		if (sourceContent.equals("")) return;
		diagnosisList.clear();
		diagnosisList.addAll(JNI.clangSyntaxDiagnosis(defaultFileName, sourceContent,
												   cmdOptions));
		handler.sendEmptyMessage(SYNTAX_DIAGNOSIS_RESULT);
	}


	public void autoComplete() {
		if (sourceContent.equals("")) return;
		cursorIndex = mScrollEditText.getSelectionStart();

		// Check the char of current inputing
		char ch = sourceContent.charAt(cursorIndex - 1);
		if (String.valueOf(ch).matches("[^\\d|\\w|.|_]") || isItemClick) {

			if (mPopupWindow != null) {
				isItemClick = false;
				handler.sendEmptyMessage(CLOSE_POPUP_WINDOW);
			}
			return;
		} 

		// Get expression for regex match
		int index = cursorIndex - 1;
		while (index >= 0) {
			ch = sourceContent.charAt(index);
			if (index == 0 || String.valueOf(ch).matches("[^\\d|\\w|_]")) {
				if (index != 0) index = index + 1;
				expression = sourceContent.substring(index, cursorIndex);
				break;
			}
			--index;
		}

		// Get some params for auto complete
		Layout layout = mScrollEditText.getLayout();

		if (layout != null && isTextChanged) {

			autoCompleteList.clear();
			int currLine = layout.getLineForOffset(cursorIndex) + 1;
			int currLineStart = layout.getLineStart(currLine - 1);
			int currColumn = cursorIndex - currLineStart;

			mScrollEditText.getLineBounds(currLine - 1, rect);


			int y = rect.bottom - mScrollView.getScrollY() + actionBarHeight + statusBarHeight + 5;

			autoCompleteList.addAll(JNI.clangAutoComplete(defaultFileName, sourceContent,
														   cmdOptions, expression, currLine, currColumn));

			if (autoCompleteList.isEmpty())
				autoCompleteList.add(defaultComplete);
			Message msg = handler.obtainMessage(AUTO_COMPLETE_RESULT);
			msg.obj = y;
			handler.sendMessage(msg);

		}
	}


	public void showPopupWindow(int x, int y) {

		int width = screenWidth - 10;
		int height = 300;
		int size = autoCompleteList.size();
		if (size > 0 && size <= 3)
			height = size * 80;

		if (mPopupWindow == null) {
			mPopupWindow = new PopupWindow(popupView, width, height, true);
			mPopupWindow.setElevation(10);

			mPopupWindow.setFocusable(false);
			mPopupWindow.setTouchable(true);

			mPopupWindow.setBackgroundDrawable(getDrawable(R.drawable.popup_window_style));
		}

		mPopupWindow.update(x, y, width, height, true);
		mPopupWindow.showAtLocation(mScrollEditText, Gravity.TOP, x, y);

	}


	private OnClickListener clickListener = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO: Implement this method
			switch (v.getId()) {
				case R.id.mScrollEditText:
					if(mPopupWindow != null)
						mPopupWindow.dismiss();
					break;
			}
		}
	};


	@Override
	protected void onDestroy() {
		// TODO: Implement this method
		super.onDestroy();
		isThreadPause = true;
	}
}

