package jatx.musictransmitter.android.di

import android.content.Context
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import jatx.musictransmitter.android.data.ContentStorageTestImpl
import jatx.musictransmitter.android.data.SettingsImpl
import jatx.musictransmitter.android.data.TrackInfoStorageTestImpl
import jatx.musictransmitter.android.domain.ContentStorage
import jatx.musictransmitter.android.domain.Settings
import jatx.musictransmitter.android.domain.TrackInfoStorage
import jatx.musictransmitter.android.services.MusicTransmitterService
import jatx.musictransmitter.android.ui.MusicEditorActivity
import jatx.musictransmitter.android.ui.MusicTransmitterActivity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        TestAppModule::class
    ],
)
interface TestAppComponent: AppDeps {
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        fun build(): TestAppComponent
    }

    fun injectMusicTransmitterActivity(musicTransmitterActivity: MusicTransmitterActivity)

    fun injectMusicTransmitterService(musicTransmitterService: MusicTransmitterService)

    fun injectMusicEditorActivity(musicEditorActivity: MusicEditorActivity)

    fun injectTestDeps(testDeps: TestDeps)
}

class TestDeps {
    @Inject
    lateinit var contentStorage: ContentStorage

    @Inject
    lateinit var trackInfoStorage: TrackInfoStorage

    @Inject
    lateinit var settings: Settings
}

@Module
class TestAppModule {
    @Provides
    @Singleton
    fun provideSettings(context: Context): Settings = SettingsImpl(context)

    @Provides
    @Singleton
    fun provideContentStorage(): ContentStorage = ContentStorageTestImpl()

    @Provides
    @Singleton
    fun provideTrackInfoStorage(): TrackInfoStorage = TrackInfoStorageTestImpl()
}