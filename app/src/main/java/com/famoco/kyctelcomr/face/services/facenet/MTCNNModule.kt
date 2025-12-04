package com.famoco.kyctelcomr.face.services.facenet

// com/famoco/kyctelcomr/di/MTCNNModule.kt


import android.content.Context
import com.famoco.kyctelcomr.face.services.mtcnn.MTCNN
import com.famoco.kyctelcomr.face.services.facenet.MyUtil
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import java.io.IOException

@Module
@InstallIn(SingletonComponent::class)
object MTCNNModule {

    @Provides
    @Singleton
    fun provideMTCNN(
        @ApplicationContext context: Context
    ): MTCNN {
        return try {
            val mtcnn = MTCNN(context)
            mtcnn.load()
            mtcnn
        } catch (e: IOException) {
            // Handle the exception as appropriate
            // You can rethrow it as a RuntimeException or handle it gracefully
            throw RuntimeException("Failed to initialize MTCNN", e)
        }
    }
}
