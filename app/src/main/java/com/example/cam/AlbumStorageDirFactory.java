package com.example.cam;

import java.io.File;

/**
 * Created by zyang_000 on 2016/7/28.
 */
abstract class AlbumStorageDirFactory {
    public abstract File getAlbumStorageDir(String albumName);

}
