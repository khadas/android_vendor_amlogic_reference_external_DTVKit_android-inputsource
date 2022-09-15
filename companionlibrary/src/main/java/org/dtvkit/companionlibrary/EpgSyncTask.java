package org.dtvkit.companionlibrary;

import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ACTION_SYNC_STATUS_CHANGED;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_ERROR_REASON;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_INPUT_ID;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_CHANNEL_ONLY;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_FROM;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_REASON;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_CHANNEL;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ERROR_DATABASE_INSERT;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ERROR_EPG_SYNC_CANCELED;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ERROR_INPUT_ID_NULL;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ERROR_NO_CHANNELS;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.ERROR_NO_PROGRAMS;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_BY_OTHER;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_BY_SCAN;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_ERROR;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_FINISHED;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_REASON;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_STARTED;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.SYNC_STATUS;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_NEED_UPDATE_CHANNEL;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_CURRENT_PLAY_CHANNEL_ID;
import static com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService.BUNDLE_KEY_SYNC_FREQUENCY;


import android.content.ContentProviderOperation;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.media.tv.TvContract;
import android.net.Uri;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.LongSparseArray;

import com.droidlogic.dtvkit.companionlibrary.EpgSyncJobService;
import com.droidlogic.dtvkit.companionlibrary.model.Channel;
import com.droidlogic.dtvkit.companionlibrary.model.Program;
import com.droidlogic.dtvkit.companionlibrary.utils.TvContractUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class EpgSyncTask {
    private static final int BATCH_OPERATION_COUNT = 50;
    private final String TAG = EpgSyncTask.class.getSimpleName();
    private final boolean DEBUG = false;
    private final ThreadFactory sThreadFactory = new NamedThreadFactory("EpgSyncTask");
    private final ExecutorService EPG_EXECUTOR = Executors.newSingleThreadExecutor(sThreadFactory);
    private final EpgSyncJobService mMainService;
    private final String INVALID_DEFAULT_REASON = "---";
    private FutureTask<String> mFutureTask;
    private String mSyncReason = INVALID_DEFAULT_REASON;

    public EpgSyncTask(EpgSyncJobService service) {
        mMainService = service;
    }

    public void run(Intent intent) {
        if (intent != null) {
            String syncReason = intent.getStringExtra(BUNDLE_KEY_SYNC_FROM);
//            if (TextUtils.equals(mSyncReason, syncReason)) {
//                Log.i(TAG, "sync reason is the same from:" + syncReason);
//                return;
//            }
        }
        if (mFutureTask != null) {
            Log.i(TAG, "cancel " + mFutureTask.cancel(true));
        }
        if (intent == null) {
            Log.w(TAG, "intent is null, do nothing!");
            return;
        }
        PersistableBundle persistableBundle = new PersistableBundle();
        if (intent.getStringExtra("inputId") == null) {
            persistableBundle.putString(EpgSyncJobService.BUNDLE_KEY_INPUT_ID,
                    EpgSyncJobService.DTVKIT_INPUTID);
        } else {
            persistableBundle.putString(EpgSyncJobService.BUNDLE_KEY_INPUT_ID,
                    intent.getStringExtra("inputId"));
        }
        persistableBundle.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_NOW_NEXT,
                intent.getBooleanExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_NOW_NEXT, false));
        persistableBundle.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_CHANNEL_ONLY,
                intent.getBooleanExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_CHANNEL_ONLY, false));
        persistableBundle.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_CHANNEL,
                intent.getBooleanExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_SEARCHED_CHANNEL, false));
        persistableBundle.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_REASON,
                intent.getBooleanExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_REASON, false));
        persistableBundle.putString(EpgSyncJobService.BUNDLE_KEY_SYNC_FROM,
                intent.getStringExtra(BUNDLE_KEY_SYNC_FROM));
        persistableBundle.putInt(EpgSyncJobService.BUNDLE_KEY_SYNC_FREQUENCY,
                intent.getIntExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_FREQUENCY, -1));
        persistableBundle.putBoolean(EpgSyncJobService.BUNDLE_KEY_SYNC_NEED_UPDATE_CHANNEL,
                intent.getBooleanExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_NEED_UPDATE_CHANNEL, true));
        persistableBundle.putLong(EpgSyncJobService.BUNDLE_KEY_SYNC_CURRENT_PLAY_CHANNEL_ID,
                intent.getLongExtra(BUNDLE_KEY_SYNC_CURRENT_PLAY_CHANNEL_ID, -1));
        Bundle parameters = intent.getBundleExtra(EpgSyncJobService.BUNDLE_KEY_SYNC_PARAMETERS);
        if (parameters != null) {
            Set<String> keySet = parameters.keySet();
            Object obj;
            for (String key : keySet) {
                obj = parameters.get(key);
                if (obj != null) {
                    if (obj instanceof Boolean) {
                        persistableBundle.putBoolean(key, (boolean) obj);
                    } else if (obj instanceof String) {
                        persistableBundle.putString(key, (String) obj);
                    } else if (obj instanceof Long) {
                        persistableBundle.putLong(key, (long) obj);
                    } else if (obj instanceof Integer) {
                        persistableBundle.putInt(key, (int) obj);
                    } else {
                        Log.w(TAG, "not support for " + obj);
                    }
                }
            }
        }

        mFutureTask = new FutureTask<>(new EpgCallable(persistableBundle));
        EPG_EXECUTOR.execute(mFutureTask);
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger mCount = new AtomicInteger(0);
        private final ThreadFactory mDefaultThreadFactory;
        private final String mPrefix;

        public NamedThreadFactory(final String prefix) {
            mDefaultThreadFactory = Executors.defaultThreadFactory();
            mPrefix = prefix + "-";
        }

        @Override
        public Thread newThread(@NonNull final Runnable runnable) {
            final Thread thread = mDefaultThreadFactory.newThread(runnable);
            thread.setName(mPrefix + mCount.getAndIncrement());
            return thread;
        }
    }

    private class EpgCallable implements Callable<String> {
        private final PersistableBundle mBundle;
        private boolean mUpdateByScan;
        private String mInputId;
        private long mStartTime = 0;

        public EpgCallable(PersistableBundle bundle) {
            mBundle = bundle;
        }

        private void startCall() {
            mSyncReason = mBundle.getString(BUNDLE_KEY_SYNC_FROM);
            Log.i(TAG, "Send SYNC_STARTED broadcast");
            Intent intent = new Intent(ACTION_SYNC_STATUS_CHANGED);
            intent.putExtra(BUNDLE_KEY_INPUT_ID, mBundle.getString(BUNDLE_KEY_INPUT_ID));
            intent.putExtra(SYNC_STATUS, SYNC_STARTED);
            LocalBroadcastManager.getInstance(mMainService.getApplicationContext()).sendBroadcast(intent);
            mStartTime = System.currentTimeMillis();
        }

        private void endCall(int reason) {
            mSyncReason = INVALID_DEFAULT_REASON;
            Log.i(TAG, "finishEpgSync reason = " + reason + ", mUpdateByScan = " + mUpdateByScan);
            Intent intent = new Intent(ACTION_SYNC_STATUS_CHANGED);
            intent.putExtra(BUNDLE_KEY_INPUT_ID, mInputId);
            intent.putExtra(BUNDLE_KEY_SYNC_FROM, mBundle.getString(BUNDLE_KEY_SYNC_FROM));
            if (reason != 0) {
                intent.putExtra(SYNC_STATUS, SYNC_ERROR);
                intent.putExtra(BUNDLE_KEY_ERROR_REASON, reason);
            } else {
                intent.putExtra(SYNC_STATUS, SYNC_FINISHED);
                if (mUpdateByScan) {
                    intent.putExtra(SYNC_REASON, SYNC_BY_SCAN);
                } else {
                    intent.putExtra(SYNC_REASON, SYNC_BY_OTHER);
                }
            }
            if (mStartTime > 0) {
                Log.i(TAG, "costTime=" + (System.currentTimeMillis() - mStartTime) + "ms");
            }
            Log.i(TAG, "Send SYNC_FINISHED broadcast");
            LocalBroadcastManager.getInstance(mMainService.getApplicationContext()).sendBroadcast(intent);
        }

        private boolean isCancelled() {
            return Thread.currentThread().isInterrupted();
        }

        public int doInBackground() {
            mInputId = mBundle.getString(BUNDLE_KEY_INPUT_ID);
            boolean mIsSearchedChannel = mBundle.getBoolean(BUNDLE_KEY_SYNC_SEARCHED_CHANNEL, false);
            mUpdateByScan = mBundle.getBoolean(BUNDLE_KEY_SYNC_REASON, false);
            String syncSignalType = mBundle.getString(BUNDLE_KEY_SYNC_SEARCHED_SIGNAL_TYPE);
            boolean syncCurrent = !TextUtils.equals("full", syncSignalType);
            boolean updateChannel = mBundle.getBoolean(BUNDLE_KEY_SYNC_NEED_UPDATE_CHANNEL, true);
            Long currentChannelId = mBundle.getLong(BUNDLE_KEY_SYNC_CURRENT_PLAY_CHANNEL_ID, -1);
            int frequency = mBundle.getInt(BUNDLE_KEY_SYNC_FREQUENCY, -1);

            if (mInputId == null) {
                return ERROR_INPUT_ID_NULL;
            }

            if (isCancelled()) {
                return ERROR_EPG_SYNC_CANCELED;
            }

            if (syncCurrent) {
                if (!TextUtils.isEmpty(syncSignalType) && !mMainService.checkSignalTypesMatch(syncSignalType)) {
                    //dvb source changed, should cancel this job
                    return ERROR_EPG_SYNC_CANCELED;
                }

                if (!TextUtils.isEmpty(EpgSyncJobService.getChannelTypeFilter())
                        && !mMainService.checkSignalTypesMatch(EpgSyncJobService.getChannelTypeFilter())) {
                    //dvb source changed, should cancel this job
                    return ERROR_EPG_SYNC_CANCELED;
                }
            }

            if (updateChannel) {
                List<Channel> tvChannels = mMainService.getChannels(syncCurrent);
                TvContractUtils.updateChannels(mMainService, mInputId, mIsSearchedChannel, tvChannels, EpgSyncJobService.getChannelTypeFilter(), mBundle);
            }
            LinkedList<Channel> channelList = TvContractUtils.buildChannelMap(
                    mMainService.getContentResolver(), mInputId, frequency, currentChannelId);
            if (channelList == null) {
                return ERROR_NO_CHANNELS;
            }

            /* Get which type of sync this is. Now next or updated event period sync */
//            boolean nowNext = mBundle.getBoolean(BUNDLE_KEY_SYNC_NOW_NEXT, false);

            boolean channelOnly = mBundle.getBoolean(BUNDLE_KEY_SYNC_CHANNEL_ONLY, false);

            /* Get the updated event periods if required for this type of sync */
            /* List<EventPeriod> eventPeriods = new ArrayList<>();
            if (!nowNext) {
                eventPeriods = getListOfUpdatedEventPeriods();
            }*/
            for (int i = 0; i < channelList.size(); ++i) {
                if (DEBUG) {
                    Log.d(TAG, "Update channel " + channelList.get(i).toString());
                }
                Uri channelUri = TvContract.buildChannelUri(channelList.get(i).getId());

                /* Check whether the job has been cancelled */
                if (isCancelled()) {
                    return ERROR_EPG_SYNC_CANCELED;
                }

                if (!channelOnly) {
                    /* Get the programs */
                    List<Program> programs = new ArrayList<>(mMainService.getAllProgramsForChannel(channelUri,  channelList.get(i)));

                    if (!programs.isEmpty()) {
                        /* Set channel ids if not set */
                        for (int index = 0; index < programs.size(); index++) {
                            if (programs.get(index).getChannelId() == -1) {
                                programs.set(index,
                                        new Program.Builder(programs.get(index))
                                                .setChannelId(channelList.get(i).getId())
                                                .build());
                            }
                        }

                        /* Double check whether the job has been cancelled */
                        if (isCancelled()) {
                            return ERROR_EPG_SYNC_CANCELED;
                        }
                        int ret = updatePrograms(channelUri, programs);
                        if (ret != 0) {
                            return ret;
                        }
                    }
                }
            }
            return 0;
        }

        @Override
        public String call() {
            startCall();
            int ret = doInBackground();
            endCall(ret);
            return Integer.toString(ret);
        }

        /**
         * Updates the system database, TvProvider, with the given programs.
         *
         * <p>If there is any overlap between the given and existing programs, the existing ones
         * will be updated with the given ones if they have the same title or replaced.
         *
         * @param channelUri  The channel where the program info will be added.
         * @param newPrograms A list of {@link Program} instances which includes program
         *                    information.
         * @return error code.
         */
        public int updatePrograms(Uri channelUri, List<Program> newPrograms) {
            final int fetchedProgramsCount = newPrograms.size();
            if (fetchedProgramsCount == 0) {
                return ERROR_NO_PROGRAMS;
            }
            List<Program> oldPrograms = TvContractUtils.getPrograms(mMainService.getContentResolver(), channelUri);
            Program firstNewProgram = newPrograms.get(0);
            int oldProgramsIndex = 0;
            int newProgramsIndex = 0;

            // Skip the past programs. They will be automatically removed by the system.
            for (Program program : oldPrograms) {
                if (/*program.getEndTimeUtcMillis() < System.currentTimeMillis() ||*/
                        program.getEndTimeUtcMillis() > firstNewProgram.getStartTimeUtcMillis()) {
                    break;
                } else {
                    oldProgramsIndex++;
                }
            }
            // Compare the new programs with old programs one by one and update/delete the old one
            // or insert new program if there is no matching program in the database.
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            while (newProgramsIndex < fetchedProgramsCount) {
                Program oldProgram = oldProgramsIndex < oldPrograms.size() ? oldPrograms.get(oldProgramsIndex) : null;
                Program newProgram = newPrograms.get(newProgramsIndex);
                boolean addNewProgram = false;
                if (oldProgram != null) {
                    if (oldProgram.equals(newProgram)) {
                        // Exact match. No need to update. Move on to the next programs.
                        if (DEBUG) Log.e(TAG, "equals");
                        oldProgramsIndex++;
                        newProgramsIndex++;
                    } else if (mMainService.shouldUpdateProgramMetadata(oldProgram, newProgram)) {
                        // Partial match. Update the old program with the new one.
                        // NOTE: Use 'update' in this case instead of 'insert' and 'delete'. There
                        // could be application specific settings which belong to the old program.
                        if (DEBUG) Log.v(TAG, "shouldUpdateProgramMetadata");
                        ops.add(ContentProviderOperation.newUpdate(
                                        TvContract.buildProgramUri(oldProgram.getId()))
                                .withValues(newProgram.toContentValues())
                                .build());
                        oldProgramsIndex++;
                        newProgramsIndex++;
                    } else if (oldProgram.getEndTimeUtcMillis()
                            < newProgram.getEndTimeUtcMillis()) {
                        if (DEBUG) Log.v(TAG, "old end time < new end time");
                        // No match. Remove the old program first to see if the next program in
                        // {@code oldPrograms} partially matches the new program.
                        ops.add(ContentProviderOperation.newDelete(
                                        TvContract.buildProgramUri(oldProgram.getId()))
                                .build());
                        oldProgramsIndex++;
                    } else {
                        // No match. The new program does not match any of the old programs. Insert
                        // it as a new program.
                        if (DEBUG) Log.v(TAG, "No match");
                        addNewProgram = true;
                        newProgramsIndex++;
                    }
                } else {
                    // No old programs. Just insert new programs.
                    if (DEBUG) Log.v(TAG, "No old programs");
                    addNewProgram = true;
                    newProgramsIndex++;
                }
                if (addNewProgram) {
                    ops.add(ContentProviderOperation
                            .newInsert(TvContract.Programs.CONTENT_URI)
                            .withValues(newProgram.toContentValues())
                            .build());
                }
                // Throttle the batch operation not to cause TransactionTooLargeException.
                if (ops.size() > BATCH_OPERATION_COUNT || newProgramsIndex >= fetchedProgramsCount) {
                    if (DEBUG) Log.i(TAG, "updatePrograms number " + ops.size());
                    try {
                        mMainService.getContentResolver().applyBatch(TvContract.AUTHORITY, ops);
                    } catch (RemoteException | OperationApplicationException e) {
                        Log.e(TAG, "Failed to insert programs.", e);
                        return ERROR_DATABASE_INSERT;
                    }
                    ops.clear();
                }
            }
            return 0;
        }
    }
}
