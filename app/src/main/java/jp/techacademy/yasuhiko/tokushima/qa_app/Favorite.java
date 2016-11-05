package jp.techacademy.yasuhiko.tokushima.qa_app;

import java.io.Serializable;

/**
 * Created by tokushima on 2016/11/03.
 */

public class Favorite implements Serializable {
    private String mFavKey;

    public void setmFavKey(String mFavKey) {
        this.mFavKey = mFavKey;
    }

    public String getmFavKey() {

        return mFavKey;
    }

    private String mFavoriteQid;

    public void setmFavoriteQid(String mFavoriteQid) {
        this.mFavoriteQid = mFavoriteQid;
    }

    public String getmFavoriteQid() {
        return mFavoriteQid;
    }

    public Favorite(String favKey, String favoriteqid) {
        mFavKey = favKey;
        mFavoriteQid = favoriteqid;
    }
}
