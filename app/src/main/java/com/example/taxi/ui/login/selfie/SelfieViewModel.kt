package com.example.taxi.ui.login.selfie

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxi.domain.exception.traceErrorException
import com.example.taxi.domain.model.IsCompletedModel
import com.example.taxi.domain.model.MainResponse
import com.example.taxi.domain.model.selfie.SelfieAllData
import com.example.taxi.domain.model.selfie.StatusModel
import com.example.taxi.domain.usecase.register.GetRegisterResponseUseCase
import com.example.taxi.utils.Resource
import com.example.taxi.utils.ResourceState
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class SelfieViewModel(private val getRegisterResponseUseCase: GetRegisterResponseUseCase) :
    ViewModel() {
    private val compositeDisposable = CompositeDisposable()

    private val _selfieResponse = MutableLiveData<Resource<MainResponse<SelfieAllData<IsCompletedModel,StatusModel>>>>();
    val selfieResponse get() = _selfieResponse


    fun fillSelfie(
        selfieUri: Uri, licensePhotoUri: Uri, contentResolver: ContentResolver
    ) {
       _selfieResponse.postValue(Resource(ResourceState.LOADING))

        val selfieFile = getFileFromUri(selfieUri, contentResolver)
        val licensePhotoFile = getFileFromUri(licensePhotoUri, contentResolver)
        val selfieRequestBody =
            selfieFile?.let { it.asRequestBody("image/*".toMediaTypeOrNull()) }
        val licensePhotoRequestBody =
            licensePhotoFile?.let { it.asRequestBody("image/*".toMediaTypeOrNull()) }

        val selfiePart =
            selfieRequestBody?.let {
                MultipartBody.Part.createFormData("selfie", selfieFile.name,
                    it
                )
            }
        val licensePhotoPart = licensePhotoRequestBody?.let {
            MultipartBody.Part.createFormData(
                "licensePhoto", licensePhotoFile.name, it
            )
        }

        selfiePart?.let {
            licensePhotoPart?.let { it1 ->
                getRegisterResponseUseCase.fillSelfie(it, it1)
                    .subscribeOn(Schedulers.io())
                    .doOnSubscribe {}
                    .doOnTerminate {}
                    .subscribe({  response ->
                        Log.e("tekshirish", "fillSelfie: $response", )
                        viewModelScope.launch {
                            _selfieResponse.postValue(Resource(ResourceState.SUCCESS, response))

                        }
                    }, {
                            error ->
                        _selfieResponse.postValue(
                            Resource(
                                ResourceState.ERROR,
                                message = traceErrorException(error).getErrorMessage()
                            )
                        )
                    })
            }
        }?.let {
            compositeDisposable.add(
                it
            )
        }

    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }

    private fun getFileFromUri(uri: Uri, contentResolver: ContentResolver): File? {
        val filePath = uri.path
        return filePath?.let { File(it) }
    }
}