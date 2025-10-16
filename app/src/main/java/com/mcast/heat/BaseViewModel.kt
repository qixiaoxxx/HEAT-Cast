package com.mcast.heat

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.mcast.heat.data.BaseResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform


abstract class BaseViewModel(application: Application) : AndroidViewModel(application)

sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val exception: Throwable) : Result<Nothing>
    data object Loading : Result<Nothing>
}

fun <T> Flow<T>.asResult(): Flow<Result<T>> = map<T, Result<T>> { Result.Success(it) }
    .onStart { emit(Result.Loading) }
    .catch {
        it.printStackTrace()
        emit(Result.Error(it))
    }

fun <T> Flow<Result<BaseResponse<T>>>.transResult(context: Context) = transform {
    if (it is Result.Success) {
        if (it.data.code == 200) {
            emit(it.data.data)
        } else {
            Toast.makeText(context, it.data.message ?: "", Toast.LENGTH_SHORT).show()
        }
    }
}
