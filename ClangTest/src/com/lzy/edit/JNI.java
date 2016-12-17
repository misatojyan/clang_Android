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

import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.List;

public final class JNI  {
	
	// The object of JNI for native code to using multi thread
	private static JNI jni;
	// The object of Handler for send message
	private static Handler handler;
	// JNI init
    private JNI(Handler handler){
		JNI.handler = handler;
	}

	// Get the instance of JNI
	public static JNI getInstance(Handler mHandler){
		if(jni == null){
			jni = new JNI(mHandler);
		}
		return jni;
	}

	// For native code to calling java function
	public void astyleFormatError(int errno ,String info){
		if(handler!=null){
			Message msg = handler.obtainMessage();
			msg.obj = "Error: " + info;
			handler.sendMessage(msg);
		}
	}

	// Return the result of clang complete
	public void autoCompleteResult(String result){
		if(handler!=null){
			Message msg = handler.obtainMessage();
			msg.obj = result;
			handler.sendMessage(msg);
		}
	}

    static {
        System.loadLibrary("main");
		System.loadLibrary("clang");
    }

	// Get the clang's version
    public native static String getClangVersion();

	// Clang auto complete
	public native static ArrayList<String> clangAutoComplete(String filename, String content, String[] cmdOptions,
												 String expression ,int currLine, int currColumn);

    // Clang syntax diagnosis
	public native static ArrayList<String> clangSyntaxDiagnosis(String filename, String content, String[] cmdOptions);
	

}

