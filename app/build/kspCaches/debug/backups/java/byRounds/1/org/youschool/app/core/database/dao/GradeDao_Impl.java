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
import org.youschool.app.core.database.entities.GradeEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GradeDao_Impl implements GradeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GradeEntity> __insertionAdapterOfGradeEntity;

  private final SharedSQLiteStatement __preparedStmtOfClearAllGrades;

  public GradeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGradeEntity = new EntityInsertionAdapter<GradeEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `grades` (`id`,`lessonId`,`unitId`,`subject`,`weight`,`markVal`,`markNum`,`markDate`,`workType`,`teacherFio`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GradeEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getLessonId() == null) {
          statement.bindNull(2);
        } else {
          statement.bindLong(2, entity.getLessonId());
        }
        if (entity.getUnitId() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getUnitId());
        }
        statement.bindString(4, entity.getSubject());
        statement.bindLong(5, entity.getWeight());
        statement.bindString(6, entity.getMarkVal());
        statement.bindDouble(7, entity.getMarkNum());
        statement.bindString(8, entity.getMarkDate());
        statement.bindString(9, entity.getWorkType());
        statement.bindString(10, entity.getTeacherFio());
      }
    };
    this.__preparedStmtOfClearAllGrades = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM grades";
        return _query;
      }
    };
  }

  @Override
  public Object insertGrades(final List<GradeEntity> grades,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGradeEntity.insert(grades);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllGrades(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllGrades.acquire();
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
          __preparedStmtOfClearAllGrades.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<GradeEntity>> getAllGrades() {
    final String _sql = "SELECT * FROM grades ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"grades"}, new Callable<List<GradeEntity>>() {
      @Override
      @NonNull
      public List<GradeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLessonId = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonId");
          final int _cursorIndexOfUnitId = CursorUtil.getColumnIndexOrThrow(_cursor, "unitId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfMarkVal = CursorUtil.getColumnIndexOrThrow(_cursor, "markVal");
          final int _cursorIndexOfMarkNum = CursorUtil.getColumnIndexOrThrow(_cursor, "markNum");
          final int _cursorIndexOfMarkDate = CursorUtil.getColumnIndexOrThrow(_cursor, "markDate");
          final int _cursorIndexOfWorkType = CursorUtil.getColumnIndexOrThrow(_cursor, "workType");
          final int _cursorIndexOfTeacherFio = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherFio");
          final List<GradeEntity> _result = new ArrayList<GradeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GradeEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpLessonId;
            if (_cursor.isNull(_cursorIndexOfLessonId)) {
              _tmpLessonId = null;
            } else {
              _tmpLessonId = _cursor.getLong(_cursorIndexOfLessonId);
            }
            final Long _tmpUnitId;
            if (_cursor.isNull(_cursorIndexOfUnitId)) {
              _tmpUnitId = null;
            } else {
              _tmpUnitId = _cursor.getLong(_cursorIndexOfUnitId);
            }
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpWeight;
            _tmpWeight = _cursor.getInt(_cursorIndexOfWeight);
            final String _tmpMarkVal;
            _tmpMarkVal = _cursor.getString(_cursorIndexOfMarkVal);
            final double _tmpMarkNum;
            _tmpMarkNum = _cursor.getDouble(_cursorIndexOfMarkNum);
            final String _tmpMarkDate;
            _tmpMarkDate = _cursor.getString(_cursorIndexOfMarkDate);
            final String _tmpWorkType;
            _tmpWorkType = _cursor.getString(_cursorIndexOfWorkType);
            final String _tmpTeacherFio;
            _tmpTeacherFio = _cursor.getString(_cursorIndexOfTeacherFio);
            _item = new GradeEntity(_tmpId,_tmpLessonId,_tmpUnitId,_tmpSubject,_tmpWeight,_tmpMarkVal,_tmpMarkNum,_tmpMarkDate,_tmpWorkType,_tmpTeacherFio);
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
  public Flow<List<GradeEntity>> getGradesForSubject(final String subjectName) {
    final String _sql = "SELECT * FROM grades WHERE subject = ? ORDER BY id DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, subjectName);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"grades"}, new Callable<List<GradeEntity>>() {
      @Override
      @NonNull
      public List<GradeEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLessonId = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonId");
          final int _cursorIndexOfUnitId = CursorUtil.getColumnIndexOrThrow(_cursor, "unitId");
          final int _cursorIndexOfSubject = CursorUtil.getColumnIndexOrThrow(_cursor, "subject");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfMarkVal = CursorUtil.getColumnIndexOrThrow(_cursor, "markVal");
          final int _cursorIndexOfMarkNum = CursorUtil.getColumnIndexOrThrow(_cursor, "markNum");
          final int _cursorIndexOfMarkDate = CursorUtil.getColumnIndexOrThrow(_cursor, "markDate");
          final int _cursorIndexOfWorkType = CursorUtil.getColumnIndexOrThrow(_cursor, "workType");
          final int _cursorIndexOfTeacherFio = CursorUtil.getColumnIndexOrThrow(_cursor, "teacherFio");
          final List<GradeEntity> _result = new ArrayList<GradeEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GradeEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final Long _tmpLessonId;
            if (_cursor.isNull(_cursorIndexOfLessonId)) {
              _tmpLessonId = null;
            } else {
              _tmpLessonId = _cursor.getLong(_cursorIndexOfLessonId);
            }
            final Long _tmpUnitId;
            if (_cursor.isNull(_cursorIndexOfUnitId)) {
              _tmpUnitId = null;
            } else {
              _tmpUnitId = _cursor.getLong(_cursorIndexOfUnitId);
            }
            final String _tmpSubject;
            _tmpSubject = _cursor.getString(_cursorIndexOfSubject);
            final int _tmpWeight;
            _tmpWeight = _cursor.getInt(_cursorIndexOfWeight);
            final String _tmpMarkVal;
            _tmpMarkVal = _cursor.getString(_cursorIndexOfMarkVal);
            final double _tmpMarkNum;
            _tmpMarkNum = _cursor.getDouble(_cursorIndexOfMarkNum);
            final String _tmpMarkDate;
            _tmpMarkDate = _cursor.getString(_cursorIndexOfMarkDate);
            final String _tmpWorkType;
            _tmpWorkType = _cursor.getString(_cursorIndexOfWorkType);
            final String _tmpTeacherFio;
            _tmpTeacherFio = _cursor.getString(_cursorIndexOfTeacherFio);
            _item = new GradeEntity(_tmpId,_tmpLessonId,_tmpUnitId,_tmpSubject,_tmpWeight,_tmpMarkVal,_tmpMarkNum,_tmpMarkDate,_tmpWorkType,_tmpTeacherFio);
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
