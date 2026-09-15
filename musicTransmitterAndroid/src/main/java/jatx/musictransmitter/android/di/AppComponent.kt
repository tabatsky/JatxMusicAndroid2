package jatx.musictransmitter.android.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import jatx.musictransmitter.android.data.ContentStorageImpl
import jatx.musictransmitter.android.data.PlaylistKeeperImpl
import jatx.musictransmitter.android.data.SettingsImpl
import jatx.musictransmitter.android.data.TrackInfoStorageImpl
import jatx.musictransmitter.android.db.AppDatabase
import jatx.musictransmitter.android.db.dao.TrackDao
import jatx.musictransmitter.android.domain.ContentStorage
import jatx.musictransmitter.android.domain.PlaylistKeeper
import jatx.musictransmitter.android.domain.Settings
import jatx.musictransmitter.android.domain.TrackInfoStorage
import jatx.musictransmitter.android.services.MusicTransmitterService
import jatx.musictransmitter.android.ui.MusicEditorActivity
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class
    ],
)
interface AppComponent: AppDeps {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): AppComponent
    }

    fun injectMusicTransmitterActivity(musicTransmitterActivity: MusicTransmitterActivity)

    @OptIn(UnstableApi::class)
    fun injectMusicTransmitterService(musicTransmitterService: MusicTransmitterService)

    fun injectMusicEditorActivity(musicEditorActivity: MusicEditorActivity)
}

@Module
class AppModule {
    @Provides
    @Singleton
    fun provideSettings(context: Context, trackInfoStorage: TrackInfoStorage): Settings =
        SettingsImpl(context, trackInfoStorage)

    @Provides
    @Singleton
    fun provideContentStorage(context: Context): ContentStorage = ContentStorageImpl(context)

    @Provides
    @Singleton
    fun provideTrackDao(context: Context) = AppDatabase.invoke(context).trackDao()

    @Provides
    @Singleton
    fun provideTrackInfoStorage(trackDao: TrackDao): TrackInfoStorage = TrackInfoStorageImpl(trackDao)

    @Provides
    @Singleton
    fun providePlaylistKeeper(settings: Settings): PlaylistKeeper = PlaylistKeeperImpl(settings)
}

interface AppDeps {
    fun context(): Context
}