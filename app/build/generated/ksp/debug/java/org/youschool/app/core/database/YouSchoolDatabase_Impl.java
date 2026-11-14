package org.youschool.app.core.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import org.youschool.app.core.database.dao.GradeDao;
import org.youschool.app.core.database.dao.GradeDao_Impl;
import org.youschool.app.core.database.dao.HomeworkStatusDao;
import org.youschool.app.core.database.dao.HomeworkStatusDao_Impl;
import org.youschool.app.core.database.dao.LessonDao;
import org.youschool.app.core.database.dao.LessonDao_Impl;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class YouSchoolDatabase_Impl extends YouSchoolDatabase {
  private volatile LessonDao _lessonDao;

  private volatile GradeDao _gradeDao;

  private volatile HomeworkStatusDao _homeworkStatusDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `lessons` (`id` INTEGER NOT NULL, `dateMillis` INTEGER NOT NULL, `numInDay` INTEGER NOT NULL, `subjectId` INTEGER, `subjectName` TEXT NOT NULL, `subjectShort` TEXT NOT NULL, `topic` TEXT NOT NULL, `teacherName` TEXT NOT NULL, `homeworkText` TEXT NOT NULL, `homeworkFilesJson` TEXT NOT NULL, `gradeValue` TEXT, `gradeWeight` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `grades` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `lessonId` INTEGER, `unitId` INTEGER, `subject` TEXT NOT NULL, `weight` INTEGER NOT NULL, `markVal` TEXT NOT NULL, `markNum` REAL NOT NULL, `markDate` TEXT NOT NULL, `workType` TEXT NOT NULL, `teacherFio` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `homework_status` (`lessonId` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `updatedTimestamp` INTEGER NOT NULL, PRIMARY KEY(`lessonId`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '31804edc08e720074afeb9a143572d31')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `lessons`");
        db.execSQL("DROP TABLE IF EXISTS `grades`");
        db.execSQL("DROP TABLE IF EXISTS `homework_status`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsLessons = new HashMap<String, TableInfo.Column>(12);
        _columnsLessons.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("dateMillis", new TableInfo.Column("dateMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("numInDay", new TableInfo.Column("numInDay", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("subjectId", new TableInfo.Column("subjectId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("subjectName", new TableInfo.Column("subjectName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("subjectShort", new TableInfo.Column("subjectShort", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("topic", new TableInfo.Column("topic", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("teacherName", new TableInfo.Column("teacherName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("homeworkText", new TableInfo.Column("homeworkText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("homeworkFilesJson", new TableInfo.Column("homeworkFilesJson", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("gradeValue", new TableInfo.Column("gradeValue", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLessons.put("gradeWeight", new TableInfo.Column("gradeWeight", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLessons = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesLessons = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoLessons = new TableInfo("lessons", _columnsLessons, _foreignKeysLessons, _indicesLessons);
        final TableInfo _existingLessons = TableInfo.read(db, "lessons");
        if (!_infoLessons.equals(_existingLessons)) {
          return new RoomOpenHelper.ValidationResult(false, "lessons(org.youschool.app.core.database.entities.LessonEntity).\n"
                  + " Expected:\n" + _infoLessons + "\n"
                  + " Found:\n" + _existingLessons);
        }
        final HashMap<String, TableInfo.Column> _columnsGrades = new HashMap<String, TableInfo.Column>(10);
        _columnsGrades.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("lessonId", new TableInfo.Column("lessonId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("unitId", new TableInfo.Column("unitId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("subject", new TableInfo.Column("subject", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("weight", new TableInfo.Column("weight", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("markVal", new TableInfo.Column("markVal", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("markNum", new TableInfo.Column("markNum", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("markDate", new TableInfo.Column("markDate", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("workType", new TableInfo.Column("workType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGrades.put("teacherFio", new TableInfo.Column("teacherFio", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGrades = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGrades = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGrades = new TableInfo("grades", _columnsGrades, _foreignKeysGrades, _indicesGrades);
        final TableInfo _existingGrades = TableInfo.read(db, "grades");
        if (!_infoGrades.equals(_existingGrades)) {
          return new RoomOpenHelper.ValidationResult(false, "grades(org.youschool.app.core.database.entities.GradeEntity).\n"
                  + " Expected:\n" + _infoGrades + "\n"
                  + " Found:\n" + _existingGrades);
        }
        final HashMap<String, TableInfo.Column> _columnsHomeworkStatus = new HashMap<String, TableInfo.Column>(3);
        _columnsHomeworkStatus.put("lessonId", new TableInfo.Column("lessonId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHomeworkStatus.put("isCompleted", new TableInfo.Column("isCompleted", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHomeworkStatus.put("updatedTimestamp", new TableInfo.Column("updatedTimestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHomeworkStatus = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHomeworkStatus = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHomeworkStatus = new TableInfo("homework_status", _columnsHomeworkStatus, _foreignKeysHomeworkStatus, _indicesHomeworkStatus);
        final TableInfo _existingHomeworkStatus = TableInfo.read(db, "homework_status");
        if (!_infoHomeworkStatus.equals(_existingHomeworkStatus)) {
          return new RoomOpenHelper.ValidationResult(false, "homework_status(org.youschool.app.core.database.entities.HomeworkStatusEntity).\n"
                  + " Expected:\n" + _infoHomeworkStatus + "\n"
                  + " Found:\n" + _existingHomeworkStatus);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "31804edc08e720074afeb9a143572d31", "a3695725881b2a124c6162fa8952646e");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "lessons","grades","homework_status");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `lessons`");
      _db.execSQL("DELETE FROM `grades`");
      _db.execSQL("DELETE FROM `homework_status`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(LessonDao.class, LessonDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GradeDao.class, GradeDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HomeworkStatusDao.class, HomeworkStatusDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public LessonDao lessonDao() {
    if (_lessonDao != null) {
      return _lessonDao;
    } else {
      synchronized(this) {
        if(_lessonDao == null) {
          _lessonDao = new LessonDao_Impl(this);
        }
        return _lessonDao;
      }
    }
  }

  @Override
  public GradeDao gradeDao() {
    if (_gradeDao != null) {
      return _gradeDao;
    } else {
      synchronized(this) {
        if(_gradeDao == null) {
          _gradeDao = new GradeDao_Impl(this);
        }
        return _gradeDao;
      }
    }
  }

  @Override
  public HomeworkStatusDao homeworkStatusDao() {
    if (_homeworkStatusDao != null) {
      return _homeworkStatusDao;
    } else {
      synchronized(this) {
        if(_homeworkStatusDao == null) {
          _homeworkStatusDao = new HomeworkStatusDao_Impl(this);
        }
        return _homeworkStatusDao;
      }
    }
  }
}
