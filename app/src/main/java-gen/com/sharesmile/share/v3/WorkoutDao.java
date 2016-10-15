package com.sharesmile.share.v3;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.sharesmile.share.v3.Workout;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "WORKOUT".
*/
public class WorkoutDao extends AbstractDao<Workout, Long> {

    public static final String TABLENAME = "WORKOUT";

    /**
     * Properties of entity Workout.<br/>
     * Can be used for QueryBuilder and for referencing column names.
    */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Distance = new Property(1, float.class, "distance", false, "DISTANCE");
        public final static Property ElapsedTime = new Property(2, String.class, "elapsedTime", false, "ELAPSED_TIME");
        public final static Property Steps = new Property(3, Integer.class, "steps", false, "STEPS");
        public final static Property RecordedTime = new Property(4, float.class, "recordedTime", false, "RECORDED_TIME");
        public final static Property AvgSpeed = new Property(5, float.class, "avgSpeed", false, "AVG_SPEED");
        public final static Property CauseBrief = new Property(6, String.class, "causeBrief", false, "CAUSE_BRIEF");
        public final static Property Date = new Property(7, java.util.Date.class, "date", false, "DATE");
        public final static Property RunAmount = new Property(8, Float.class, "runAmount", false, "RUN_AMOUNT");
        public final static Property Is_sync = new Property(9, Boolean.class, "is_sync", false, "IS_SYNC");
    };


    public WorkoutDao(DaoConfig config) {
        super(config);
    }
    
    public WorkoutDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"WORKOUT\" (" + //
                "\"_id\" INTEGER PRIMARY KEY ," + // 0: id
                "\"DISTANCE\" REAL NOT NULL ," + // 1: distance
                "\"ELAPSED_TIME\" TEXT NOT NULL ," + // 2: elapsedTime
                "\"STEPS\" INTEGER," + // 3: steps
                "\"RECORDED_TIME\" REAL NOT NULL ," + // 4: recordedTime
                "\"AVG_SPEED\" REAL NOT NULL ," + // 5: avgSpeed
                "\"CAUSE_BRIEF\" TEXT," + // 6: causeBrief
                "\"DATE\" INTEGER," + // 7: date
                "\"RUN_AMOUNT\" REAL," + // 8: runAmount
                "\"IS_SYNC\" INTEGER);"); // 9: is_sync
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"WORKOUT\"";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Workout entity) {
        stmt.clearBindings();
 
        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }
        stmt.bindDouble(2, entity.getDistance());
        stmt.bindString(3, entity.getElapsedTime());
 
        Integer steps = entity.getSteps();
        if (steps != null) {
            stmt.bindLong(4, steps);
        }
        stmt.bindDouble(5, entity.getRecordedTime());
        stmt.bindDouble(6, entity.getAvgSpeed());
 
        String causeBrief = entity.getCauseBrief();
        if (causeBrief != null) {
            stmt.bindString(7, causeBrief);
        }
 
        java.util.Date date = entity.getDate();
        if (date != null) {
            stmt.bindLong(8, date.getTime());
        }
 
        Float runAmount = entity.getRunAmount();
        if (runAmount != null) {
            stmt.bindDouble(9, runAmount);
        }
 
        Boolean is_sync = entity.getIs_sync();
        if (is_sync != null) {
            stmt.bindLong(10, is_sync ? 1L: 0L);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }    

    /** @inheritdoc */
    @Override
    public Workout readEntity(Cursor cursor, int offset) {
        Workout entity = new Workout( //
            cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
            cursor.getFloat(offset + 1), // distance
            cursor.getString(offset + 2), // elapsedTime
            cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3), // steps
            cursor.getFloat(offset + 4), // recordedTime
            cursor.getFloat(offset + 5), // avgSpeed
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6), // causeBrief
            cursor.isNull(offset + 7) ? null : new java.util.Date(cursor.getLong(offset + 7)), // date
            cursor.isNull(offset + 8) ? null : cursor.getFloat(offset + 8), // runAmount
            cursor.isNull(offset + 9) ? null : cursor.getShort(offset + 9) != 0 // is_sync
        );
        return entity;
    }
     
    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Workout entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setDistance(cursor.getFloat(offset + 1));
        entity.setElapsedTime(cursor.getString(offset + 2));
        entity.setSteps(cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3));
        entity.setRecordedTime(cursor.getFloat(offset + 4));
        entity.setAvgSpeed(cursor.getFloat(offset + 5));
        entity.setCauseBrief(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
        entity.setDate(cursor.isNull(offset + 7) ? null : new java.util.Date(cursor.getLong(offset + 7)));
        entity.setRunAmount(cursor.isNull(offset + 8) ? null : cursor.getFloat(offset + 8));
        entity.setIs_sync(cursor.isNull(offset + 9) ? null : cursor.getShort(offset + 9) != 0);
     }
    
    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Workout entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    /** @inheritdoc */
    @Override
    public Long getKey(Workout entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override    
    protected boolean isEntityUpdateable() {
        return true;
    }
    
}
