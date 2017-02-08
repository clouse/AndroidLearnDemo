package com.clouse.androiddemo.imageloader.bean;

/**
 * Created by 浩 on 2017/2/8.
 */

public class FolderBean {
    /**
     * 当前文件夹的路径
     */
    private String dir;
    private String firstImgPath;
    private String name;
    private int imgCount;
    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }

    public int getImgCount() {
        return imgCount;
    }

    public void setImgCount(int imgCount) {
        this.imgCount = imgCount;
    }


}
