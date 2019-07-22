package org.adaway.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.adaway.db.converter.DateConverter;
import org.adaway.db.converter.ListTypeConverter;
import org.adaway.db.dao.HostListItemDao;
import org.adaway.db.dao.HostsSourceDao;
import org.adaway.db.entity.HostListItem;
import org.adaway.db.entity.HostsSource;
import org.adaway.util.AppExecutors;

import static org.adaway.db.Migrations.MIGRATION_1_2;
import static org.adaway.db.entity.HostsSource.USER_SOURCE_ID;
import static org.adaway.db.entity.HostsSource.USER_SOURCE_URL;

/**
 * This class is the application database based on Room.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
@Database(entities = {HostsSource.class, HostListItem.class}, version = 2)
@TypeConverters({DateConverter.class, ListTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    /**
     * The database singleton instance.
     */
    private static volatile AppDatabase instance;

    /**
     * Get the database instance.
     *
     * @param context The application context.
     * @return The database instance.
     */
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "app.db"
                    ).addCallback(new Callback() {
                        @Override
                        public void onCreate(@NonNull SupportSQLiteDatabase db) {
                            AppExecutors.getInstance().diskIO().execute(
                                    () -> AppDatabase.initialize(instance)
                            );
                        }
                    }).addMigrations(
                            MIGRATION_1_2
                    ).build();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize the database content.
     */
    private static void initialize(AppDatabase database) {
        // Check if there is no hosts source
        HostsSourceDao hostsSourceDao = database.hostsSourceDao();
        if (!hostsSourceDao.getAll().isEmpty()) {
            return;
        }
        // User source
        HostsSource userSource = new HostsSource();
        userSource.setId(USER_SOURCE_ID);
        userSource.setUrl(USER_SOURCE_URL);
        userSource.setEnabled(true);
        hostsSourceDao.insert(userSource);
        // https://hosts-file.net
        HostsSource source1 = new HostsSource();
        source1.setUrl("https://hosts-file.net/ad_servers.txt");
        source1.setEnabled(false);
        hostsSourceDao.insert(source1);
        // https://pgl.yoyo.org/adservers/
        HostsSource source2 = new HostsSource();
        source2.setUrl("https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext");
        source2.setEnabled(false);
        hostsSourceDao.insert(source2);
        // AdAway's own mobile hosts
        HostsSource source3 = new HostsSource();
        source3.setUrl("https://adaway.org/hosts.txt");
        source3.setEnabled(true);
        hostsSourceDao.insert(source3);
    }

    /**
     * Get the hosts source DAO.
     *
     * @return The hosts source DAO.
     */
    public abstract HostsSourceDao hostsSourceDao();

    /**
     * Get the hosts list item DAO.
     *
     * @return The hosts list item DAO.
     */
    public abstract HostListItemDao hostsListItemDao();
}
