package org.youschool.app.core.database.dao;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;
import org.youschool.app.core.database.entities.LessonEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class LessonDao_Impl implements LessonDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<LessonEntity> __insertionAdapterOfLessonEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteLessonsForDay;

  public LessonDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfLessonEntity = new EntityInsertionAdapter<LessonEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `lessons` (`id`,`dateMillis`,`numInDay`,`subjectId`,`subjectName`,`subjectShort`,`topic`,`teacherName`,`homeworkText`,`homeworkFilesJson`,`gradeValue`,`gradeWeight`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final LessonEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getDateMillis());
        statement.bindLong(3, entity.getNumInDay());
        if (entity.getSubjectId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getSubjectId());
        }
        statement.bindString(5, entity.getSubjectName());
        statement.bindString(6, entity.getSubjectShort());
        statement.bindString(7, entity.getTopic());
        statement.bindString(8, entity.getTeacherName());
        statement.bindString(9, entity.getHomeworkText());
        statement.bindString(10, entity.getHomeworkFilesJson());
        if (entity.getGradeValue() == null) {
          statement.bindNull(11);
        } else {
          statement.bindString(11, entity.getGradeValue());
        }
        if (entity.getGradeWeight() == null) {
          statement.bindNull(12);
        } else {
          statement.bindLong(12, entity.getGradeWeight());
        }
      }
    };
    this.__preparedStmtOfDeleteLessonsForDay = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM lessons WHERE dateMillis >= ? AND dateMillis < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLessons(final List<LessonEntity> lessons,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfLessonEntity.insert(lessons);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteLessonsForDay(final long startOfDay, final long endOfDay,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteLessonsForDay.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, startOfDay);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, endOfDay);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteLessonsForDay.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<LessonEntity>> getLessonsForDay(final long startOfDay, final long endOfDay) {
    final String _sql = "SELECT * FROM lessons WHERE dateMillis >= ? AND dateMillis < ? ORDER BY numInDay ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, startOfDay);
    _argIndex = 2;
    _statement.bindLong(_argIndex, endOfDay);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lessons"}, new Callable<List<LessonEntity>>() {
      @Override
      @NonNull
      public List<LessonEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfNumInDay = CursorUtil.getColumnIndexOrThrow(_cursor, "numInDay");
          final int _cursorIndexOfSubjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectId");
          final int _cursorIndexOfSubjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectName");
          final int _cursorIndexOfSubjectShort = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectShort");
          final int _cursorIndexOfTopic = CursorUtil.getColumnIndexOrThrow(_cursor, "topic");
          final int _cursorIndexOfTeacherName = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherName");
          final int _cursorIndexOfHomeworkText = CursorUtil.getColumnIndexOrThrow(_cursor, "homeworkText");
          final int _cursorIndexOfHomeworkFilesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "homeworkFilesJson");
          final int _cursorIndexOfGradeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gradeValue");
          final int _cursorIndexOfGradeWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "gradeWeight");
          final List<LessonEntity> _result = new ArrayList<LessonEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LessonEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final int _tmpNumInDay;
            _tmpNumInDay = _cursor.getInt(_cursorIndexOfNumInDay);
            final Long _tmpSubjectId;
            if (_cursor.isNull(_cursorIndexOfSubjectId)) {
              _tmpSubjectId = null;
            } else {
              _tmpSubjectId = _cursor.getLong(_cursorIndexOfSubjectId);
            }
            final String _tmpSubjectName;
            _tmpSubjectName = _cursor.getString(_cursorIndexOfSubjectName);
            final String _tmpSubjectShort;
            _tmpSubjectShort = _cursor.getString(_cursorIndexOfSubjectShort);
            final String _tmpTopic;
            _tmpTopic = _cursor.getString(_cursorIndexOfTopic);
            final String _tmpTeacherName;
            _tmpTeacherName = _cursor.getString(_cursorIndexOfTeacherName);
            final String _tmpHomeworkText;
            _tmpHomeworkText = _cursor.getString(_cursorIndexOfHomeworkText);
            final String _tmpHomeworkFilesJson;
            _tmpHomeworkFilesJson = _cursor.getString(_cursorIndexOfHomeworkFilesJson);
            final String _tmpGradeValue;
            if (_cursor.isNull(_cursorIndexOfGradeValue)) {
              _tmpGradeValue = null;
            } else {
              _tmpGradeValue = _cursor.getString(_cursorIndexOfGradeValue);
            }
            final Integer _tmpGradeWeight;
            if (_cursor.isNull(_cursorIndexOfGradeWeight)) {
              _tmpGradeWeight = null;
            } else {
              _tmpGradeWeight = _cursor.getInt(_cursorIndexOfGradeWeight);
            }
            _item = new LessonEntity(_tmpId,_tmpDateMillis,_tmpNumInDay,_tmpSubjectId,_tmpSubjectName,_tmpSubjectShort,_tmpTopic,_tmpTeacherName,_tmpHomeworkText,_tmpHomeworkFilesJson,_tmpGradeValue,_tmpGradeWeight);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<LessonEntity>> getAllLessonsWithHomework() {
    final String _sql = "SELECT * FROM lessons WHERE homeworkText != '' ORDER BY dateMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"lessons"}, new Callable<List<LessonEntity>>() {
      @Override
      @NonNull
      public List<LessonEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfDateMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "dateMillis");
          final int _cursorIndexOfNumInDay = CursorUtil.getColumnIndexOrThrow(_cursor, "numInDay");
          final int _cursorIndexOfSubjectId = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectId");
          final int _cursorIndexOfSubjectName = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectName");
          final int _cursorIndexOfSubjectShort = CursorUtil.getColumnIndexOrThrow(_cursor, "subjectShort");
          final int _cursorIndexOfTopic = CursorUtil.getColumnIndexOrThrow(_cursor, "topic");
          final int _cursorIndexOfTeacherName = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherName");
          final int _cursorIndexOfHomeworkText = CursorUtil.getColumnIndexOrThrow(_cursor, "homeworkText");
          final int _cursorIndexOfHomeworkFilesJson = CursorUtil.getColumnIndexOrThrow(_cursor, "homeworkFilesJson");
          final int _cursorIndexOfGradeValue = CursorUtil.getColumnIndexOrThrow(_cursor, "gradeValue");
          final int _cursorIndexOfGradeWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "gradeWeight");
          final List<LessonEntity> _result = new ArrayList<LessonEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final LessonEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpDateMillis;
            _tmpDateMillis = _cursor.getLong(_cursorIndexOfDateMillis);
            final int _tmpNumInDay;
            _tmpNumInDay = _cursor.getInt(_cursorIndexOfNumInDay);
            final Long _tmpSubjectId;
            if (_cursor.isNull(_cursorIndexOfSubjectId)) {
              _tmpSubjectId = null;
            } else {
              _tmpSubjectId = _cursor.getLong(_cursorIndexOfSubjectId);
            }
            final String _tmpSubjectName;
            _tmpSubjectName = _cursor.getString(_cursorIndexOfSubjectName);
            final String _tmpSubjectShort;
            _tmpSubjectShort = _cursor.getString(_cursorIndexOfSubjectShort);
            final String _tmpTopic;
            _tmpTopic = _cursor.getString(_cursorIndexOfTopic);
            final String _tmpTeacherName;
            _tmpTeacherName = _cursor.getString(_cursorIndexOfTeacherName);
            final String _tmpHomeworkText;
            _tmpHomeworkText = _cursor.getString(_cursorIndexOfHomeworkText);
            final String _tmpHomeworkFilesJson;
            _tmpHomeworkFilesJson = _cursor.getString(_cursorIndexOfHomeworkFilesJson);
            final String _tmpGradeValue;
            if (_cursor.isNull(_cursorIndexOfGradeValue)) {
              _tmpGradeValue = null;
            } else {
              _tmpGradeValue = _cursor.getString(_cursorIndexOfGradeValue);
            }
            final Integer _tmpGradeWeight;
            if (_cursor.isNull(_cursorIndexOfGradeWeight)) {
              _tmpGradeWeight = null;
            } else {
              _tmpGradeWeight = _cursor.getInt(_cursorIndexOfGradeWeight);
            }
            _item = new LessonEntity(_tmpId,_tmpDateMillis,_tmpNumInDay,_tmpSubjectId,_tmpSubjectName,_tmpSubjectShort,_tmpTopic,_tmpTeacherName,_tmpHomeworkText,_tmpHomeworkFilesJson,_tmpGradeValue,_tmpGradeWeight);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
