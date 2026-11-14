package org.youschool.app.core.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
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
import org.youschool.app.core.database.entities.HomeworkStatusEntity;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HomeworkStatusDao_Impl implements HomeworkStatusDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HomeworkStatusEntity> __insertionAdapterOfHomeworkStatusEntity;

  public HomeworkStatusDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHomeworkStatusEntity = new EntityInsertionAdapter<HomeworkStatusEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `homework_status` (`lessonId`,`isCompleted`,`updatedTimestamp`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HomeworkStatusEntity entity) {
        statement.bindLong(1, entity.getLessonId());
        final int _tmp = entity.isCompleted() ? 1 : 0;
        statement.bindLong(2, _tmp);
        statement.bindLong(3, entity.getUpdatedTimestamp());
      }
    };
  }

  @Override
  public Object setStatus(final HomeworkStatusEntity status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHomeworkStatusEntity.insert(status);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HomeworkStatusEntity>> getAllStatuses() {
    final String _sql = "SELECT * FROM homework_status";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"homework_status"}, new Callable<List<HomeworkStatusEntity>>() {
      @Override
      @NonNull
      public List<HomeworkStatusEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLessonId = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonId");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfUpdatedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedTimestamp");
          final List<HomeworkStatusEntity> _result = new ArrayList<HomeworkStatusEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HomeworkStatusEntity _item;
            final long _tmpLessonId;
            _tmpLessonId = _cursor.getLong(_cursorIndexOfLessonId);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final long _tmpUpdatedTimestamp;
            _tmpUpdatedTimestamp = _cursor.getLong(_cursorIndexOfUpdatedTimestamp);
            _item = new HomeworkStatusEntity(_tmpLessonId,_tmpIsCompleted,_tmpUpdatedTimestamp);
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
  public Object getStatusForLesson(final long lessonId,
      final Continuation<? super HomeworkStatusEntity> $completion) {
    final String _sql = "SELECT * FROM homework_status WHERE lessonId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, lessonId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HomeworkStatusEntity>() {
      @Override
      @Nullable
      public HomeworkStatusEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfLessonId = CursorUtil.getColumnIndexOrThrow(_cursor, "lessonId");
          final int _cursorIndexOfIsCompleted = CursorUtil.getColumnIndexOrThrow(_cursor, "isCompleted");
          final int _cursorIndexOfUpdatedTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedTimestamp");
          final HomeworkStatusEntity _result;
          if (_cursor.moveToFirst()) {
            final long _tmpLessonId;
            _tmpLessonId = _cursor.getLong(_cursorIndexOfLessonId);
            final boolean _tmpIsCompleted;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsCompleted);
            _tmpIsCompleted = _tmp != 0;
            final long _tmpUpdatedTimestamp;
            _tmpUpdatedTimestamp = _cursor.getLong(_cursorIndexOfUpdatedTimestamp);
            _result = new HomeworkStatusEntity(_tmpLessonId,_tmpIsCompleted,_tmpUpdatedTimestamp);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
