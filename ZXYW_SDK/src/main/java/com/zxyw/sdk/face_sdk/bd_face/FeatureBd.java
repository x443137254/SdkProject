package com.zxyw.sdk.face_sdk.bd_face;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

import com.baidu.idl.main.facesdk.model.Feature;

import java.util.ArrayList;
import java.util.List;

public class FeatureBd extends SQLiteOpenHelper {

    private final String tableName = "Features";

    public FeatureBd(@Nullable Context context) {
        super(context, "FaceFeature.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id integer primary key autoincrement," +
                "groupId String," +
                "feature blob" +
                ")";
        try {
            db.execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int insert(byte[] feature, String group) {
        ContentValues contentValues = new ContentValues();
        if (feature != null) {
            contentValues.put("feature", feature);
            contentValues.put("groupId", group);
        }
        getWritableDatabase().insert(tableName, null, contentValues);
        int faceId = 0;
        final String sql = "SELECT last_insert_rowid() FROM " + tableName;
        try (Cursor cursor = getReadableDatabase().rawQuery(sql, null)) {
            if (cursor.moveToFirst()) {
                faceId = cursor.getInt(0);
            }
        }
        return faceId;
    }

    public void delete(int id) {
        String sql = "DELETE FROM " + tableName + " WHERE id=" + id;
        try {
            getWritableDatabase().execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delete(String group) {
        String sql = "DELETE FROM " + tableName + " WHERE groupId=\"" + group + "\"";
        try {
            getWritableDatabase().execSQL(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAll() {
        try {
            getWritableDatabase().execSQL("DELETE FROM " + tableName);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Feature> queryAll() {
        List<Feature> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + tableName, null);
        Feature feature;
        while (cursor.moveToNext()) {
            feature = new Feature();
            fillValues(cursor, feature);
            list.add(feature);
        }
        cursor.close();
        return list;
    }

    public List<Feature> queryAll(String group) {
        List<Feature> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("SELECT * FROM " + tableName + " WHERE groupId='" + group + "'", null);
        Feature feature;
        while (cursor.moveToNext()) {
            feature = new Feature();
            fillValues(cursor, feature);
            list.add(feature);
        }
        cursor.close();
        return list;
    }

    public Feature query(int id, String group) {
        String sql = "SELECT * FROM " + tableName + " WHERE groupId=\"" + group + "\" and id=" + id;
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        Feature feature = null;
        if (cursor.moveToNext()) {
            feature = new Feature();
            fillValues(cursor, feature);
        }
        cursor.close();
        return feature;
    }

    public Feature query(int id) {
        String sql = "SELECT * FROM " + tableName + " WHERE id=" + id;
        Cursor cursor = getReadableDatabase().rawQuery(sql, null);
        Feature feature = null;
        if (cursor.moveToNext()) {
            feature = new Feature();
            fillValues(cursor, feature);
        }
        cursor.close();
        return feature;
    }

    private void fillValues(Cursor cursor, Feature feature) {
        feature.setId(cursor.getInt(cursor.getColumnIndex("id")));
        feature.setGroupId(cursor.getString(cursor.getColumnIndex("groupId")));
        feature.setFeature(cursor.getBlob(cursor.getColumnIndex("feature")));
    }
}
