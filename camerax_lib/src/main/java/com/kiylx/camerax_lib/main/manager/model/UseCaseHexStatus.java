package com.kiylx.camerax_lib.main.manager.model;

import androidx.camera.core.UseCase;

import com.kiylx.camerax_lib.main.manager.UseCaseHolder;
import com.kiylx.camerax_lib.main.manager.util.HexStatusManager;

public class UseCaseHexStatus {
    public static final int USE_CASE_NONE = 0x0001;
    public static final int USE_CASE_PREVIEW = 0x0002;
    public static final int USE_CASE_IMAGE_CAPTURE = 0x0004;
    public static final int USE_CASE_IMAGE_ANALYZE = 0x0008;
    public static final int USE_CASE_VIDEO_CAPTURE = 0x00010;

    public static boolean canTakePicture(int status) {
        return HexStatusManager.isContain(status, USE_CASE_IMAGE_CAPTURE);
    }

    public static boolean canTakeVideo(int status) {
        return HexStatusManager.isContain(status, USE_CASE_VIDEO_CAPTURE);
    }

    public static boolean canAnalyze(int status) {
        return HexStatusManager.isContain(status, USE_CASE_IMAGE_ANALYZE);
    }

    public static boolean canPreview(int status) {
        return HexStatusManager.isContain(status, USE_CASE_PREVIEW);
    }

    public static int removeUseCase(int status, int useCase) {
        return HexStatusManager.remove(status, useCase);
    }
    public static int addUseCase(int status, int useCase) {
        return HexStatusManager.add(status, useCase);
    }


}
