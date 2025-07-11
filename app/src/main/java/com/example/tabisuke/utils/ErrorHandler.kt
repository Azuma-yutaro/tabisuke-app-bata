package com.example.tabisuke.utils

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestoreException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorHandler {
    
    private const val TAG = "ErrorHandler"
    
    /**
     * Firebaseエラーをユーザーフレンドリーなメッセージに変換
     */
    fun getFirebaseErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseNetworkException -> "ネットワーク接続を確認してください"
            is SocketTimeoutException -> "接続がタイムアウトしました。再度お試しください"
            is UnknownHostException -> "インターネット接続を確認してください"
            is FirebaseFirestoreException -> {
                when (exception.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "アクセス権限がありません"
                    FirebaseFirestoreException.Code.NOT_FOUND -> "データが見つかりません"
                    FirebaseFirestoreException.Code.ALREADY_EXISTS -> "既に存在するデータです"
                    FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED -> "リソースが不足しています"
                    FirebaseFirestoreException.Code.FAILED_PRECONDITION -> "操作の前提条件が満たされていません"
                    FirebaseFirestoreException.Code.ABORTED -> "操作が中断されました"
                    FirebaseFirestoreException.Code.OUT_OF_RANGE -> "範囲外の操作です"
                    FirebaseFirestoreException.Code.UNIMPLEMENTED -> "未実装の機能です"
                    FirebaseFirestoreException.Code.INTERNAL -> "内部エラーが発生しました"
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "サービスが利用できません"
                    FirebaseFirestoreException.Code.DATA_LOSS -> "データが失われました"
                    FirebaseFirestoreException.Code.UNAUTHENTICATED -> "認証が必要です"
                    else -> "データベースエラーが発生しました"
                }
            }
            else -> "エラーが発生しました。再度お試しください"
        }
    }
    
    /**
     * エラーログを出力
     */
    fun logError(tag: String, message: String, exception: Exception?) {
        Log.e(tag, "$message: ${exception?.message}", exception)
    }
    
    /**
     * ネットワークエラーかどうかを判定
     */
    fun isNetworkError(exception: Exception?): Boolean {
        return exception is FirebaseNetworkException || 
               exception is SocketTimeoutException || 
               exception is UnknownHostException
    }
    
    /**
     * リトライ可能なエラーかどうかを判定
     */
    fun isRetryableError(exception: Exception?): Boolean {
        return isNetworkError(exception) || 
               exception is FirebaseFirestoreException && 
               (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                exception.code == FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED)
    }
} 