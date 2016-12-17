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
 *
 */

// C header files
#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <assert.h>
#include <errno.h>

// JNI header files
#include <jni.h>
#include <android/log.h>

// C++ header files
#include <iostream>
#include <cstdio>
#include <cstdlib>
#include <sstream>
#include <string>
#include <regex>
#include <vector>

// Clang header files
#include "clang-c/Index.h"

#define LOG_TAG "System.out"

#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

#define LOGE(...) {\
	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s:Line:%d\n",__FILE__,__LINE__);\
	__android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__);\
	exit(EXIT_FAILURE);\
}

#define printf(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

//获取数组的长度
#define GET_ARRAY_LEN(arr) ((sizeof(arr) / sizeof(arr[0])))

using namespace std;


jclass list_cls;	// ArrayList
jmethodID list_init;// Constructor
jmethodID add_mid ;	// add function
JNIEnv* m_env;		// Env object
jobject list_obj_auto;	// auto complete list
jobject list_obj_diag;	// diagnosis list

/* 
 * This is a trivial JNI example where we use a native method
 * to return a new VM String. See the corresponding Java source
 * file located at:
 *
 *   apps/samples/hello-jni/project/src/com/example/hellojni/HelloJni.java
 */
 
 
 /******************* Clang Auto Complete **********************/
 
 const char* getCompleteChunkKindSpelling(CXCompletionChunkKind chunkKind) {
 	switch (chunkKind) {
    	case CXCompletionChunk_Optional:         return "Optional"; break;
    	case CXCompletionChunk_TypedText:        return "TypedText"; break;
    	case CXCompletionChunk_Text:             return "Text"; break;
    	case CXCompletionChunk_Placeholder:      return "Placeholder"; break;
    	case CXCompletionChunk_Informative:      return "Informative"; break;
    	case CXCompletionChunk_CurrentParameter: return "CurrentParameter"; break;
    	case CXCompletionChunk_LeftParen:        return "LeftParen"; break;
    	case CXCompletionChunk_RightParen:       return "RightParen"; break;
    	case CXCompletionChunk_LeftBracket:      return "LeftBracket"; break;
    	case CXCompletionChunk_RightBracket:     return "RightBracket"; break;
    	case CXCompletionChunk_LeftBrace:        return "LeftBrace"; break;
    	case CXCompletionChunk_RightBrace:       return "RightBrace"; break;
    	case CXCompletionChunk_LeftAngle:        return "LeftAngle"; break;
    	case CXCompletionChunk_RightAngle:       return "RightAngle"; break;
    	case CXCompletionChunk_Comma:            return "Comma"; break;
    	case CXCompletionChunk_ResultType:       return "ResultType"; break;
    	case CXCompletionChunk_Colon:            return "Colon"; break;
    	case CXCompletionChunk_SemiColon:        return "SemiColon"; break;
    	case CXCompletionChunk_Equal:            return "Equal"; break;
    	case CXCompletionChunk_HorizontalSpace:  return "HorizontalSpace"; break;
    	case CXCompletionChunk_VerticalSpace:    return "VerticalSpace"; break;
    	default:                                 return "Unknown"; break;
  	}
}




const char* getCompletionAvailabilitySpelling(CXAvailabilityKind availavility) {
	switch (availavility) {
    	case CXAvailability_Available:     return "Available"; break;
    	case CXAvailability_Deprecated:    return "Deprecated"; break;
    	case CXAvailability_NotAvailable:  return "NotAvailable"; break;
    	case CXAvailability_NotAccessible: return "NotAccessible"; break;
    	default:                           return "Unknown"; break;
  	}
}



const char* getKindTypeName(CXCursor cursor) {
  	CXCursorKind curKind  = clang_getCursorKind(cursor);
  	const char *type;
  	if (clang_isAttribute(curKind)) {
    	type = "Attribute";
  	} 
	else if (clang_isDeclaration(curKind)) {
    	type = "Declaration";
  	} 
	else if (clang_isExpression(curKind)) {
    	type = "Expression";
  	} 
	else if (clang_isInvalid(curKind)) {
    	type = "Invalid";
  	} 
	else if (clang_isPreprocessing(curKind)) {
    	type = "Preprocessing";
  	} 
	else if (clang_isReference(curKind)) {
    	type = "Reference";
  	} 
	else if (clang_isStatement(curKind)) {
    	type = "Statement";
  	} 
	else if (clang_isTranslationUnit(curKind)) {
    	type = "TranslationUnit";
  	} 
	else if (clang_isUnexposed(curKind)) {
    	type = "Unexposed";
  	} 
	else {
    	type = "Unknown";
  	}
  	return type;
}


void show_completion_results(CXCodeCompleteResults *compResults,std::string& expr) {
  	//printf("********** show results\n");
  	unsigned isIncomplete = 0;
  	CXCursorKind kind = clang_codeCompleteGetContainerKind(compResults, &isIncomplete);
  	//printf("Complete: %d\n", !isIncomplete);
  	CXString kindName = clang_getCursorKindSpelling(kind);
  	//printf("Kind: %s\n", clang_getCString(kindName));
  	clang_disposeString(kindName);

  	CXString usr = clang_codeCompleteGetContainerUSR(compResults);
  	//printf("USR: %s\n", clang_getCString(usr));
  	clang_disposeString(usr);

  	//unsigned long long context = clang_codeCompleteGetContexts(compResults);
  	//printf("Context: %llu\n\n", context);

	
	const std::regex pattern("(\\S*)"+expr,std::regex_constants::icase);
  
	std::string autoCompleteResult;
	std::string first ;
	
  	// show completion results
  	//printf("********** show completion results\n");
  	//printf("CodeCompleationResultsNum: %d\n", compResults->NumResults);
  	for (auto i = 0U; i < compResults->NumResults; i++) {
    	//printf("Results: %d\n", i);
    	const CXCompletionResult &result = compResults->Results[i];
    	const CXCompletionString &compString = result.CompletionString;
    	const CXCursorKind kind = result.CursorKind;
		
    	CXString kindName  = clang_getCursorKindSpelling(kind);
    	//printf(" Kind: %s\n", clang_getCString(kindName));
		
		// Clear ArrayList
		autoCompleteResult = "";
		
		
		//autoCompleteResult += clang_getCString(kindName);
    	clang_disposeString(kindName);

    	CXAvailabilityKind availavility = clang_getCompletionAvailability(compString);
    	const char* availavilityText = getCompletionAvailabilitySpelling(availavility);
    	//printf(" Availavility: %s\n", availavilityText);

    	unsigned priority = clang_getCompletionPriority(compString);
    	//printf(" Priority: %d\n", priority);

    	CXString comment = clang_getCompletionBriefComment(compString);
    	//printf(" Comment: %s\n", clang_getCString(comment));
    	clang_disposeString(comment);

    	unsigned numChunks = clang_getNumCompletionChunks(compString);
    	//printf(" NumChunks: %d\n", numChunks);
    	for (auto j = 0U; j < numChunks; j++) {
      		CXString chunkText = clang_getCompletionChunkText(compString, j);
      		CXCompletionChunkKind chunkKind = clang_getCompletionChunkKind(compString, j);
      		//printf("   Kind: %s Text: %s\n",
            //getCompleteChunkKindSpelling(chunkKind),
            //clang_getCString(chunkText));
			
			if(j != 0) 
				autoCompleteResult += clang_getCString(chunkText);
			else
				first = clang_getCString(chunkText);
				
			
			
      		// TODO: check child chunks when CXCompletionChunk_Optional
      		// CXCompletionString child = clang_getCompletionChunkCompletionString(compString);
      		clang_disposeString(chunkText);
    	}

		if(first.compare("") != 0 && numChunks >= 2)
			autoCompleteResult += ":\t";
		autoCompleteResult += first;
		
    	unsigned numAnnotations = clang_getCompletionNumAnnotations(compString);
    	//printf(" NumAnnotation: %d\n", numAnnotations);
    	for (auto j = 0U; j < numAnnotations; ++j) {
      		CXString annoText = clang_getCompletionAnnotation(compString, j);
      		//printf("   Annotation: %s\n", clang_getCString(annoText));
      		clang_disposeString(annoText);
    	}
		
		
		// Regex match
		if(regex_search(autoCompleteResult,pattern)){
			jstring ret = m_env->NewStringUTF(autoCompleteResult.c_str());
			m_env->CallBooleanMethod(list_obj_auto,add_mid,ret);   
			m_env->DeleteLocalRef(ret); 
		}
		//auto_complete_result(autoCompleteResult.c_str());
    	//printf("\r\n");
  	}
}



/************************ Clang Diagnosis **********************/

void show_diagnosis_format(const CXDiagnostic &diag) {
  	unsigned formatOption = CXDiagnostic_DisplaySourceLocation |
                          CXDiagnostic_DisplayColumn |
                          CXDiagnostic_DisplayOption ;
  	CXString format = clang_formatDiagnostic(diag, formatOption);
  	jstring jstr = m_env->NewStringUTF(clang_getCString(format));
	m_env->CallBooleanMethod(list_obj_diag,add_mid,jstr);   
	m_env->DeleteLocalRef(jstr); 
  	//printf("  Format: %s\n", clang_getCString(format));
  	clang_disposeString(format);
}



void show_diagnosis_fixit(const CXDiagnostic &diag) {
  	unsigned numFixit = clang_getDiagnosticNumFixIts(diag);
  	for (auto j = 0U; j < numFixit; j++) {
  		CXString fixit = clang_getDiagnosticFixIt(diag, j, NULL);
    	//printf("Fixit: %s\n", clang_getCString(fixit));
		jstring jstr = m_env->NewStringUTF(clang_getCString(fixit));
		m_env->CallBooleanMethod(list_obj_diag,add_mid,jstr);   
		m_env->DeleteLocalRef(jstr); 
    	clang_disposeString(fixit);
  	}
}



void show_diagnosis(const CXTranslationUnit &unit) {
  CXDiagnosticSet diagSet = clang_getDiagnosticSetFromTU(unit);
  unsigned numDiag = clang_getNumDiagnosticsInSet(diagSet);

  for (auto i = 0U; i < numDiag; i++) {
    CXDiagnostic diag = clang_getDiagnosticInSet(diagSet, i);

    // show diagnosis spell
    CXString diagText = clang_getDiagnosticSpelling(diag);
    clang_disposeString(diagText);
    show_diagnosis_format(diag);   // format
    show_diagnosis_fixit(diag);    // fixit

    clang_disposeDiagnostic(diag);
  }
  clang_disposeDiagnosticSet(diagSet);
}


// Get Clang's version
const char* show_clang_version() {
  	CXString ver = clang_getClangVersion();
	std::string version = clang_getCString(ver);
  	//printf("%s\n", clang_getCString(version));
  	clang_disposeString(ver);
  	return version.c_str();
}




/******************** JNI Functions *******************/

#ifdef __cplusplus
extern "C" {
#endif

void init_list(JNIEnv* env,jobject thiz) {
	m_env = env;
	// Find class of ArrayList
	list_cls = env->FindClass("java/util/ArrayList");   
	// Init constructor
	list_init = env->GetMethodID(list_cls,"<init>","()V");   
	// Create List object
	list_obj_auto = env->NewObject(list_cls,list_init);
	list_obj_diag = env->NewObject(list_cls,list_init);
	// Add function
	add_mid = env->GetMethodID(list_cls,"add","(Ljava/lang/Object;)Z");
	
}


void complete_and_diagnosis(JNIEnv* env, jobject thiz,
  jstring fileName,jstring content,jobjectArray cmdOptions,jstring expression,jint currLine,jint currColumn){
		
	init_list(env,thiz);
	
	// Unsaved source file
	CXUnsavedFile sourceFile;
	sourceFile.Filename = env->GetStringUTFChars(fileName,NULL);
  	sourceFile.Contents = env->GetStringUTFChars(content,NULL);
	sourceFile.Length = env->GetStringLength(content);
	
	// Get the file name
	const auto filename = env->GetStringUTFChars(fileName,NULL);
	
	// Get the command line options
	jstring jstr = NULL;
	const char* cmd = NULL;
	int arrLen = env->GetArrayLength(cmdOptions);
	char** cmdArgs = (char**)malloc(sizeof(char*) * arrLen);

	for(int i=0;i<arrLen;++i){
		jstr = (jstring)env->GetObjectArrayElement(cmdOptions,i);
		cmd = env->GetStringUTFChars(jstr,NULL);
		int len = strlen(cmd);
		cmdArgs[i] = (char*)malloc(sizeof(char) * len);
		strcpy(cmdArgs[i],cmd);
	}
	
	// Get the expr for regex match
	const auto exp = env->GetStringUTFChars(expression,NULL);
	std::string expr = exp;
	
	// Get line and column
  	unsigned line = currLine;
  	unsigned column = currColumn;
  	auto numArgs = arrLen;
	
  	// Create index w/ excludeDeclsFromPCH = 1, displayDiagnostics=1.
  	CXIndex index = clang_createIndex(1, 0);

  	// Create Translation Unit
  	CXTranslationUnit unit = clang_parseTranslationUnit(index, filename, cmdArgs, numArgs, &sourceFile, 1, 
  		CXTranslationUnit_Incomplete|CXTranslationUnit_PrecompiledPreamble|CXTranslationUnit_CacheCompletionResults);
  
  	if (unit == NULL) {
    	LOGE("Cannot parse translation unit %s\n",strerror(errno));
    	exit(EXIT_FAILURE);
  	}

  	// Code Completion
  	CXCodeCompleteResults *compResults;
  	compResults = clang_codeCompleteAt(unit, filename, line, column,
                                     &sourceFile, 1, clang_defaultCodeCompleteOptions());
  	if (compResults == NULL) {
    	LOGE("Invalid %s\n",strerror(errno));
    	exit(EXIT_FAILURE);
  	}

  	// show Completion results
	if(!expr.empty())
  		show_completion_results(compResults,expr);
	
  	// show Diagnosis
  	show_diagnosis(unit);
	// Add list_obj_auto and list_obj_diag
	
  	clang_disposeCodeCompleteResults(compResults);
  	clang_disposeTranslationUnit(unit);
  	clang_disposeIndex(index);
	
	// Release memory
	for(int j=0;j<arrLen;++j){
		free(cmdArgs[j]);
	}
	
	env->ReleaseStringUTFChars(fileName,sourceFile.Filename);
	env->ReleaseStringUTFChars(content,sourceFile.Contents);
	env->ReleaseStringUTFChars(fileName,filename);
	env->ReleaseStringUTFChars(expression,exp);
  	
}



//Return the result of clang syntax diagnosis
JNIEXPORT jobject JNICALL Java_com_lzy_edit_JNI_clangSyntaxDiagnosis(JNIEnv* env, jobject thiz,
  jstring fileName,jstring content,jobjectArray cmdOptions) {

	jstring expression = env->NewStringUTF("");
	complete_and_diagnosis(env,thiz,fileName,content,cmdOptions,expression,0,0);
	return list_obj_diag;
}



// Return the result of clang auto complete
JNIEXPORT jobject JNICALL Java_com_lzy_edit_JNI_clangAutoComplete(JNIEnv* env, jobject thiz,
  jstring fileName,jstring content,jobjectArray cmdOptions,jstring expression,jint currLine,jint currColumn) {

	complete_and_diagnosis(env,thiz,fileName,content,cmdOptions,expression,currLine,currColumn);
	return list_obj_auto;
}


// Get clang version
JNIEXPORT jstring JNICALL Java_com_lzy_edit_JNI_getClangVersion(JNIEnv* env, jobject thiz) {

	// The clang string isn't Unicode
	const char* version = show_clang_version();
	
	printf("******%s\n",version);
    return env->NewStringUTF(version);
}
	
#ifdef __cplusplus
};
#endif
