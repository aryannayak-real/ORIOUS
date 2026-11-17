package app.orious.authenticator;

import android.content.Context;

import androidx.room.Room;

import app.orious.authenticator.database.AppDatabase;
import app.orious.authenticator.database.AuditLogDao;
import app.orious.authenticator.database.AuditLogRepository;
import app.orious.authenticator.icons.IconPackManager;
import app.orious.authenticator.vault.VaultManager;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;

@Module
@InstallIn(SingletonComponent.class)
public class AegisModule {
    @Provides
    @Singleton
    public static IconPackManager provideIconPackManager(@ApplicationContext Context context) {
        return new IconPackManager(context);
    }

    @Provides
    @Singleton
    public static AuditLogRepository provideAuditLogRepository(AppDatabase appDatabase) {
        AuditLogDao auditLogDao = appDatabase.auditLogDao();
        return new AuditLogRepository(auditLogDao);
    }

    @Provides
    @Singleton
    public static VaultManager provideVaultManager(@ApplicationContext Context context, AuditLogRepository auditLogRepository) {
        return new VaultManager(context, auditLogRepository);
    }

    @Provides
    public static Preferences providePreferences(@ApplicationContext Context context) {
        return new Preferences(context);
    }

    @Provides
    @Singleton
    public static AppDatabase provideAppDatabase(@ApplicationContext Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                        AppDatabase.class, "aegis-db")
                .build();
    }
}
