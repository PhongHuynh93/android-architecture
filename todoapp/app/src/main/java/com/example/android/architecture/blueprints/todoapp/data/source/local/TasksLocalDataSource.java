/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.architecture.blueprints.todoapp.data.source.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.TasksDataSource;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract.TaskEntry;
import com.squareup.sqlbrite.BriteDatabase;
import com.squareup.sqlbrite.SqlBrite;

import java.util.List;

import rx.Observable;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Concrete implementation of a data source as a db.
 */
public class TasksLocalDataSource implements TasksDataSource {

    private static TasksLocalDataSource INSTANCE;
    private final BriteDatabase mDatabaseHelper;
    private Func1<Cursor, Task> mTaskMapperFunction;

    // Prevent direct instantiation.
    private TasksLocalDataSource(@NonNull Context context) {
        checkNotNull(context);

        // todo get the data from db
        TasksDbHelper dbHelper = new TasksDbHelper(context);
        SqlBrite sqlBrite = SqlBrite.create();
        mDatabaseHelper = sqlBrite.wrapDatabaseHelper(dbHelper, Schedulers.io());

        // TODO: 9/1/16 the function to translate a row in cursor into the object
        mTaskMapperFunction = new Func1<Cursor, Task>() {
            @Override
            public Task call(Cursor c) {
                String itemId = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_ENTRY_ID));
                String title = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_TITLE));
                String description = c.getString(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_DESCRIPTION));
                boolean completed = c.getInt(c.getColumnIndexOrThrow(TaskEntry.COLUMN_NAME_COMPLETED)) == 1;
                return new Task(title, description, itemId, completed);
            }
        };
    }

    public static TasksLocalDataSource getInstance(@NonNull Context context) {
        if (INSTANCE == null) {
            INSTANCE = new TasksLocalDataSource(context);
        }
        return INSTANCE;
    }

    /**
     * todo get all tasks
     * @return
     */
    @Override
    public Observable<List<Task>> getTasks() {
        /**
         * declare the column to get the datas
         */
        String[] projection = {
                TaskEntry.COLUMN_NAME_ENTRY_ID,
                TaskEntry.COLUMN_NAME_TITLE,
                TaskEntry.COLUMN_NAME_DESCRIPTION,
                TaskEntry.COLUMN_NAME_COMPLETED
        };

        /**
         * todo - make a query to select datas from db
         *
         * TextUtils.join(): add , to the each column
         */
        String sql = String.format("SELECT %s FROM %s", TextUtils.join(",", projection), TaskEntry.TABLE_NAME);
        /**
         * todo createQuery: get cursor from db
         * todo mapToList: translate cursor to list of tasks
         */
        return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql)
                .mapToList(mTaskMapperFunction);
    }

    /**
     * todo get one task
     * @param taskId
     * @return
     */
    @Override
    public Observable<Task> getTask(@NonNull String taskId) {
        String[] projection = {
                TaskEntry.COLUMN_NAME_ENTRY_ID,
                TaskEntry.COLUMN_NAME_TITLE,
                TaskEntry.COLUMN_NAME_DESCRIPTION,
                TaskEntry.COLUMN_NAME_COMPLETED
        };

        /**
         * The LIKE operator is used in a WHERE clause to search for a specified pattern in a column.
         * @see <a href="http://www.w3schools.com/sql/sql_like.asp"></a>
         */
        String sql = String.format("SELECT %s FROM %s WHERE %s LIKE ?",
                TextUtils.join(",", projection), TaskEntry.TABLE_NAME, TaskEntry.COLUMN_NAME_ENTRY_ID);

        /**
         * mapToOneOrDefault: Creates an observable operator which transforms a query returning a single row to T using mapper.
         * translate one row in cursor into one task
         * return one task
         */
        return mDatabaseHelper.createQuery(TaskEntry.TABLE_NAME, sql, taskId)
                .mapToOneOrDefault(mTaskMapperFunction, null);
    }

    /**
     * todo save to db
     * @param task
     */
    @Override
    public void saveTask(@NonNull Task task) {
        checkNotNull(task);
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_ENTRY_ID, task.getId());
        values.put(TaskEntry.COLUMN_NAME_TITLE, task.getTitle());
        values.put(TaskEntry.COLUMN_NAME_DESCRIPTION, task.getDescription());
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, task.isCompleted());
        mDatabaseHelper.insert(TaskEntry.TABLE_NAME, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    @Override
    public void completeTask(@NonNull Task task) {
        completeTask(task.getId());
    }

    /**
     * todo update the db
     * @param taskId
     */
    @Override
    public void completeTask(@NonNull String taskId) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, true);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};
        mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void activateTask(@NonNull Task task) {
        activateTask(task.getId());
    }

    /**
     * todo upate the db
     * @param taskId
     */
    @Override
    public void activateTask(@NonNull String taskId) {
        ContentValues values = new ContentValues();
        values.put(TaskEntry.COLUMN_NAME_COMPLETED, false);

        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};
        mDatabaseHelper.update(TaskEntry.TABLE_NAME, values, selection, selectionArgs);
    }

    @Override
    public void clearCompletedTasks() {
        String selection = TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?";
        String[] selectionArgs = {"1"};
        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }

    @Override
    public void refreshTasks() {
        // Not required because the {@link TasksRepository} handles the logic of refreshing the
        // tasks from all the available data sources.
    }

    @Override
    public void deleteAllTasks() {
        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, null);
    }

    /**
     * todo delete from db
     * @param taskId
     */
    @Override
    public void deleteTask(@NonNull String taskId) {
        String selection = TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {taskId};
        mDatabaseHelper.delete(TaskEntry.TABLE_NAME, selection, selectionArgs);
    }
}
