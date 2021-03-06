package com.sharesmile.share.v1;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import com.sharesmile.share.v1.Workout;
import com.sharesmile.share.v1.User;
import com.sharesmile.share.v1.Cause;

import com.sharesmile.share.v1.WorkoutDao;
import com.sharesmile.share.v1.UserDao;
import com.sharesmile.share.v1.CauseDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig workoutDaoConfig;
    private final DaoConfig userDaoConfig;
    private final DaoConfig causeDaoConfig;

    private final WorkoutDao workoutDao;
    private final UserDao userDao;
    private final CauseDao causeDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        workoutDaoConfig = daoConfigMap.get(WorkoutDao.class).clone();
        workoutDaoConfig.initIdentityScope(type);

        userDaoConfig = daoConfigMap.get(UserDao.class).clone();
        userDaoConfig.initIdentityScope(type);

        causeDaoConfig = daoConfigMap.get(CauseDao.class).clone();
        causeDaoConfig.initIdentityScope(type);

        workoutDao = new WorkoutDao(workoutDaoConfig, this);
        userDao = new UserDao(userDaoConfig, this);
        causeDao = new CauseDao(causeDaoConfig, this);

        registerDao(Workout.class, workoutDao);
        registerDao(User.class, userDao);
        registerDao(Cause.class, causeDao);
    }
    
    public void clear() {
        workoutDaoConfig.getIdentityScope().clear();
        userDaoConfig.getIdentityScope().clear();
        causeDaoConfig.getIdentityScope().clear();
    }

    public WorkoutDao getWorkoutDao() {
        return workoutDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public CauseDao getCauseDao() {
        return causeDao;
    }

}
