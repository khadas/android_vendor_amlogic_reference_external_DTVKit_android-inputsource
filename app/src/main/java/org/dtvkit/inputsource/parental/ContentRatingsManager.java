/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.droidlogic.dtvkit.inputsource.parental;

import android.content.Context;
import android.media.tv.TvContentRating;
import android.media.tv.TvContentRatingSystemInfo;
import android.media.tv.TvInputManager;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.droidlogic.dtvkit.inputsource.parental.ContentRatingsParser;
import com.droidlogic.dtvkit.inputsource.parental.ContentRatingSystem;
import com.droidlogic.dtvkit.inputsource.parental.ContentRatingSystem.Rating;
import com.droidlogic.dtvkit.inputsource.parental.ContentRatingSystem.SubRating;
import java.util.ArrayList;
import java.util.List;

public class ContentRatingsManager {
    private final String TAG = ContentRatingsManager.class.getSimpleName();
    private final boolean DEBUG = true;
    private final List<ContentRatingSystem> mContentRatingSystems = new ArrayList<>();

    private final Context mContext;
    private final TvInputManager mTvInputManager;

    public ContentRatingsManager(
            Context context, TvInputManager tvInputManager) {
        mContext = context;
        this.mTvInputManager = tvInputManager;
    }

    public void update() {
        mContentRatingSystems.clear();
        ContentRatingsParser parser = new ContentRatingsParser(mContext);
        List<TvContentRatingSystemInfo> infoList = mTvInputManager.getTvContentRatingSystemList();
        for (TvContentRatingSystemInfo info : infoList) {
            List<ContentRatingSystem> list = parser.parse(info);
            if (list != null) {
                mContentRatingSystems.addAll(list);
            }
        }
        if (DEBUG) {
            Log.d(TAG, "infoList size:" + infoList.size());
            Log.d(TAG, "ContentRatingSystems size:" + mContentRatingSystems.size());
        }
    }

    /** Returns the content rating system with the give ID. */
    @Nullable
    public ContentRatingSystem getContentRatingSystem(String contentRatingSystemId) {
        for (ContentRatingSystem ratingSystem : mContentRatingSystems) {
            if (TextUtils.equals(ratingSystem.getId(), contentRatingSystemId)) {
                return ratingSystem;
            }
        }
        return null;
    }

    /** Returns a new list of all content rating systems defined. */
    public List<ContentRatingSystem> getContentRatingSystems() {
        return new ArrayList<>(mContentRatingSystems);
    }

    /**
     * Returns the long name of a given content rating including descriptors (sub-ratings) that is
     * displayed to the user. For example, "TV-PG (L, S)".
     */
    public String getDisplayNameForRating(TvContentRating canonicalRating) {
        if (TvContentRating.UNRATED.equals(canonicalRating)) {
            return "Unrated";
        }
        Rating rating = getRating(canonicalRating);
        if (rating == null) {
            return null;
        }
        List<SubRating> subRatings = getSubRatings(rating, canonicalRating);
        if (!subRatings.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (SubRating subRating : subRatings) {
                builder.append(subRating.getTitle());
                builder.append(", ");
            }
            return rating.getTitle() + " (" + builder.substring(0, builder.length() - 2) + ")";
        }
        return rating.getTitle();
    }

    public Rating getRating(TvContentRating canonicalRating) {
        if (canonicalRating == null) {
            return null;
        }
        Log.d(TAG, "ContentRatingSystems size:" + mContentRatingSystems.size());
        for (ContentRatingSystem system : mContentRatingSystems) {
            if (system.getDomain().equals(canonicalRating.getDomain())
                    && system.getName().equals(canonicalRating.getRatingSystem())) {
                for (Rating rating : system.getRatings()) {
                    if (rating.getName().equals(canonicalRating.getMainRating())) {
                        return rating;
                    }
                }
            } else {
                Log.d(TAG, "system.getDomain():" + system.getDomain());
                Log.d(TAG, "canonicalRating.getDomain():" + canonicalRating.getDomain());
                Log.d(TAG, "system.getName():" + system.getName());
                Log.d(TAG, "canonicalRating.getRatingSystem():" + canonicalRating.getRatingSystem());
            }
        }
        return null;
    }

    private List<SubRating> getSubRatings(Rating rating, TvContentRating canonicalRating) {
        List<SubRating> subRatings = new ArrayList<>();
        if (rating == null
                || rating.getSubRatings() == null
                || canonicalRating == null
                || canonicalRating.getSubRatings() == null) {
            return subRatings;
        }
        for (String subRatingString : canonicalRating.getSubRatings()) {
            for (SubRating subRating : rating.getSubRatings()) {
                if (subRating.getName().equals(subRatingString)) {
                    subRatings.add(subRating);
                    break;
                }
            }
        }
        return subRatings;
    }
}
